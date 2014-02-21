package edu.cmu.ml.praprolog.prove;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.cmu.ml.praprolog.graph.GraphWriter;
import edu.cmu.ml.praprolog.prove.LogicProgram.LogicProgramOutlink;
import edu.cmu.ml.praprolog.prove.Prover;
import edu.cmu.ml.praprolog.util.Dictionary;

/**
 * prover using depth-first approximate personalized pagerank
 * @author wcohen,krivard
 *
 */
public class DprProver extends Prover {
	private static final Logger log = Logger.getLogger(DprProver.class);
	public static final double EPS_DEFAULT = 0.0001, MINALPH_DEFAULT=0.1;
	private final double epsilon;
	private final double minAlpha;
	private final double stayProbability;
	private final double moveProbability;
	private long start, last;
	private Backtrace backtrace = new Backtrace(log);

	public DprProver() { this(false); }
	public DprProver(boolean lazyWalk) {
		this(lazyWalk,EPS_DEFAULT,MINALPH_DEFAULT);
	}
	public DprProver(double epsilon, double minalpha) {
		this(false, epsilon, minalpha);
	}
	public DprProver(boolean lazyWalk, double epsilon, double minalpha) {
		this( (lazyWalk?0.5:0.0),epsilon,minalpha);
	}
	protected DprProver(double stayP, double eps, double mina) {
		this.epsilon = eps;
		this.minAlpha = mina;
		this.stayProbability = stayP;
		moveProbability = 1.0-stayProbability;
	}
	
	public Prover copy() {
		return new DprProver(this.stayProbability, this.epsilon, this.minAlpha);
	}
	
	@Override
	public Map<LogicProgramState, Double> proveState(LogicProgram lp, LogicProgramState state0, GraphWriter gw) {

		Map<LogicProgramState,Double> p = new HashMap<LogicProgramState,Double>();
		Map<LogicProgramState,Double> r = new HashMap<LogicProgramState,Double>();
		r.put(state0, 1.0);
		Map<LogicProgramState,Integer> deg = new HashMap<LogicProgramState,Integer>();
		boolean trueLoop=true, restart=false;
		int d=-1;
		try {
			d = lp.lpDegree(state0,trueLoop,restart);
		} catch (LogicProgramException e) {
			throw new IllegalStateException(e);
		}
		deg.put(state0,d);
		backtrace.start();
		int numPushes = 0;
		int numIterations = 0;
		for(int pushCounter = 0; ;) {
			start = last = System.currentTimeMillis();
			pushCounter = this.dfsPushes(lp,p,r,deg,state0,gw,0);
			numIterations++;
			if(log.isInfoEnabled()) log.info("Iteration: "+numIterations+" pushes: "+pushCounter+" r-states: "+r.size()+" p-states: "+p.size());
			if(pushCounter==0) break;
			numPushes+=pushCounter;
		}
		if(log.isInfoEnabled()) log.info("total iterations "+numIterations+" total pushes "+numPushes);
		return p;
	}

	private int dfsPushes(LogicProgram lp, Map<LogicProgramState,Double> p, Map<LogicProgramState, Double> r,
			Map<LogicProgramState, Integer> deg, LogicProgramState u, GraphWriter gw, int pushCounter) {
		if (r.get(u) / deg.get(u) > epsilon) {
			backtrace.push(u);
			if (log.isInfoEnabled()) {
				long now = System.currentTimeMillis(); 
				if (now - last > 1000) {
					log.info("push "+pushCounter+"->"+(pushCounter+1)+" "+r.size()+" r-states u "+u);
					last = now;
				}
			}
			pushCounter += 1;
			double ru = r.get(u);
			LogicProgramOutlink restart;
			try {
				restart = lp.lpRestartWeight(u,true); // trueLoop
				if (log.isDebugEnabled()) log.debug("restart weight for pushlevel "+pushCounter);
				double unNormalizedAlpha = restart.getWeight();

				List<LogicProgramOutlink> outs = lp.lpOutlinks(u,true,false); // trueloop, restart
				double z= unNormalizedAlpha; 
				double m=0.0;
				for (LogicProgramOutlink o : outs) {
					z += o.getWeight();
					m = Math.max(m,o.getWeight());
				}

				double localAlpha = unNormalizedAlpha / z;

				if (localAlpha < this.minAlpha) {
					log.warn("max outlink weight="+m+"; numouts="+outs.size()+"; unAlpha="+restart.getWeight()+"; z="+z);
					log.warn("ru="+ru+"; degu="+deg.get(u)+"; u="+u);
					throw new MinAlphaException(minAlpha,localAlpha,u);
				}
				Dictionary.increment(p,u,minAlpha * ru,"(elided)");
				r.put(u, r.get(u) * stayProbability * (1.0-minAlpha));

				for (LogicProgramOutlink o : outs) {
					includeState(o,r,deg,z,ru,lp);
				}
				// include the reset state with weight (alph - minAlpha):
				restart.weight = z * (localAlpha - minAlpha);
				includeState(restart,r,deg,z,ru,lp);

				if (gw!=null) gw.writeEdge(u, u.restart(), restart.getFeatureList());
				for (LogicProgramOutlink o : outs) {
					if (gw != null) gw.writeEdge(u, o.getState(), o.getFeatureList());
					// current pushcounter is passed down, gets incremented and returned, and 
					// on the next for loop iter is passed down again...
					pushCounter = this.dfsPushes(lp,p,r,deg,o.getState(),gw,pushCounter);
				}
			} catch (LogicProgramException e) {
				backtrace.print(e);
			}
			backtrace.pop(u);
		}
		return pushCounter;
	}
	private void includeState(LogicProgramOutlink o, Map<LogicProgramState, Double> r,
			Map<LogicProgramState, Integer> deg, double z, double ru, LogicProgram lp) throws LogicProgramException {
		backtrace.push(o.getState());
		
		boolean followup = !r.containsKey(o.getState());
		double old = Dictionary.safeGet(r, o.getState());
		Dictionary.increment(r, o.getState(), moveProbability * (o.getWeight() / z) * ru,"(elided)");
		if(followup) {
			try {
				int degree = lp.lpDegree(o.getState(),true,true);
				deg.put(o.getState(),degree); // trueloop, restart
			} catch (LogicProgramException e) {
				backtrace.print(e);
			}
		}
		if (deg.get(o.getState()) == 0)
			throw new LogicProgramException("Zero degree for "+o.getState());
		backtrace.pop(o.getState());
	}
	public double getAlpha() {
		return this.minAlpha;
	}

}
