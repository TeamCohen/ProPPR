package edu.cmu.ml.proppr.prove;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;


import edu.cmu.ml.proppr.prove.MinAlphaException;
import edu.cmu.ml.proppr.prove.wam.LogicProgramException;
import edu.cmu.ml.proppr.prove.wam.Outlink;
import edu.cmu.ml.proppr.prove.wam.ProofGraph;
import edu.cmu.ml.proppr.prove.wam.State;
import edu.cmu.ml.proppr.util.Dictionary;

/**
 * prover using depth-first approximate personalized pagerank
 * @author wcohen,krivard
 *
 */
public class DprProver extends Prover {
	private static final Logger log = Logger.getLogger(DprProver.class);
	public static final double EPS_DEFAULT = 0.0001, MINALPH_DEFAULT=0.1;
	public static int ADJUST_ALPHA = 1;
	public static int BOOST_ALPHA = 2;
	public static int THROW_ALPHA_ERRORS = 3;
	public static final int STRATEGY_DEFAULT=THROW_ALPHA_ERRORS;
	public static final double STAYPROB_DEFAULT = 0.0;
	public static final double STAYPROB_LAZY = 0.5;
	private static final boolean TRUELOOP_ON = true;
	private static final boolean RESTART_ON = true;
	private final double epsilon;
	// not final, we might change it with the ADJUST_ALPHA strategy!
	private double minAlpha;
	private final double stayProbability;
	private final double moveProbability;
	private final int minAlphaErrorStrategy;
	// for timing traces
	private long last;
	// for debug
	private Backtrace<State> backtrace = new Backtrace<State>(log);

	@Override
	public String toString() { 
		return String.format("dpr:%.6g:%g:%s", this.epsilon, this.minAlpha, new String[]{"adjust","boost","throw"}[minAlphaErrorStrategy-1]);
		//return "dprprover(eps="+this.epsilon+", minAlpha="+this.minAlpha+", strat="+minAlphaErrorStrategy+")"; 
	}

	public DprProver() { this(false); }

	public DprProver(boolean lazyWalk) {
		this(lazyWalk,EPS_DEFAULT,MINALPH_DEFAULT,STRATEGY_DEFAULT);
	}
	public DprProver(double epsilon, double minalpha) {
		this(false, epsilon, minalpha,STRATEGY_DEFAULT);
	}
	public DprProver(double epsilon, double minalpha,int strat) {
		this(false, epsilon, minalpha,strat);
	}
	public DprProver(boolean lazyWalk, double epsilon, double minalpha) {
		this( (lazyWalk?STAYPROB_LAZY:STAYPROB_DEFAULT),epsilon,minalpha,STRATEGY_DEFAULT);
	}
	public DprProver(boolean lazyWalk, double epsilon, double minalpha, int strat) {
		this( (lazyWalk?STAYPROB_LAZY:STAYPROB_DEFAULT),epsilon,minalpha,strat);
	}
	protected DprProver(double stayP, double eps, double mina) {
		this( stayP,eps,mina,STRATEGY_DEFAULT);
	}
	protected DprProver(double stayP, double eps, double mina,int strat) {
		this.epsilon = eps;
		this.minAlpha = mina;
		this.stayProbability = stayP;
		this.moveProbability = 1.0-stayProbability;
		this.minAlphaErrorStrategy = strat;
	}

	public Prover copy() {
		DprProver copy = new DprProver(this.stayProbability, this.epsilon, this.minAlpha, this.minAlphaErrorStrategy);
		copy.setWeighter(weighter);
		return copy;
	}


	public Map<State, Double> prove(ProofGraph pg) {

		Map<State,Double> p = new HashMap<State,Double>();
		Map<State,Double> r = new HashMap<State,Double>();
		State state0 = pg.getStartState();
		r.put(state0, 1.0);
		Map<State,Integer> deg = new HashMap<State,Integer>();
		int d=-1;
		try {
			d = pg.pgDegree(state0, TRUELOOP_ON, RESTART_ON) - 1;
		} catch (LogicProgramException e) {
			throw new IllegalStateException(e);
		}
		deg.put(state0,d);
		backtrace.start();
		int numPushes = 0;
		int numIterations = 0;
		for(int pushCounter = 0; ;) {
			last = System.currentTimeMillis();
			pushCounter = this.dfsPushes(pg,p,r,deg,state0,0);
			numIterations++;
			if(log.isInfoEnabled()) log.info("Iteration: "+numIterations+" pushes: "+pushCounter+" r-states: "+r.size()+" p-states: "+p.size());
			if(pushCounter==0) break;
			numPushes+=pushCounter;
		}
		if(log.isInfoEnabled()) log.info("total iterations "+numIterations+" total pushes "+numPushes);
		return p;
	}

	private int dfsPushes(ProofGraph pg, Map<State,Double> p, Map<State, Double> r,
			Map<State, Integer> deg, State u, int pushCounter) {
		double ru = r.get(u);
		if (ru / deg.get(u) > epsilon) {
			backtrace.push(u);
			if (log.isInfoEnabled()) {
				long now = System.currentTimeMillis(); 
//				if (now - last > 1000) {
					log.info("push "+pushCounter+"->"+(pushCounter+1)+" ru "+ru+" "+r.size()+" r-states u "+u);
					last = now;
//				}
				if (log.isDebugEnabled()) log.debug("PUSHPATH include "+(pushCounter+1)+" "+u);
			}
			pushCounter += 1;
			Outlink restart=null;
			try {
				List<Outlink> outs = pg.pgOutlinks(u, TRUELOOP_ON, RESTART_ON);
//				restart = lp.lpRestartWeight(u,true); // trueLoop
//				if (log.isDebugEnabled()) log.debug("restart weight for pushlevel "+pushCounter);
//				double unNormalizedAlpha = restart.getWeight();
				double unNormalizedAlpha = 0.0;

//				List<LogicProgramOutlink> outs = lp.lpOutlinks(u,true,false); // trueloop, restart
//				double z= unNormalizedAlpha; 
				double z = 0.0;
				double m = 0.0;
				for (Outlink o : outs) {
					o.wt = this.weighter.w(o.fd);
					z += o.wt;
					m = Math.max(m,o.wt);
					if (o.child.equals(pg.getStartState())) {
						restart = o;
						unNormalizedAlpha = o.wt; 
					}
				}
				if (restart==null) {
					throw new IllegalStateException("No restart link in walk from "+u);
				}

				double localAlpha = unNormalizedAlpha / z;

				if (localAlpha < this.minAlpha) {
					log.warn("minAlpha problem, strategy="+minAlphaErrorStrategy);
					if (minAlphaErrorStrategy==ADJUST_ALPHA) {
						log.warn("decreasing minAlpha from "+minAlpha+" to "+localAlpha);
						this.minAlpha = localAlpha;
					} else if (minAlphaErrorStrategy==BOOST_ALPHA) {
						// figure out how much we need to increment the unNormalizedAlpha to get to minAlpha
						if (log.isDebugEnabled()) {
							log.debug("minAlpha issue: minAlpha="+this.minAlpha+" localAlpha="+localAlpha
									+" max outlink weight="+m+"; numouts="+outs.size()+"; unAlpha=<unsupported>; z="+z);
						}
						// figure out how much to boost
						double nonresetWeightSum = z - unNormalizedAlpha;
						double amountToBoost = (this.minAlpha*(nonresetWeightSum + unNormalizedAlpha) - unNormalizedAlpha)/(1.0 - this.minAlpha);
						z += amountToBoost;
						unNormalizedAlpha += amountToBoost;
						localAlpha = unNormalizedAlpha/z;
						log.warn("boosted to localAlpha="+localAlpha+"; unAlpha="+unNormalizedAlpha+"; z="+z);
					} else {
						log.warn("max outlink weight="+m+"; numouts="+outs.size()+"; unAlpha=<unsupported>; z="+z);
						log.warn("ru="+ru+"; degu="+deg.get(u)+"; u="+u);
						throw new MinAlphaException(minAlpha,localAlpha,u);
					}
				}
				Dictionary.increment(p,u,minAlpha * ru,"(elided)");
				r.put(u, r.get(u) * stayProbability * (1.0-minAlpha));

				restart.wt = ( z * (localAlpha - minAlpha) );
				for (Outlink o : outs) {
					if (log.isDebugEnabled()) log.debug("PUSHPATH candidate "+(pushCounter+1)+" "+u+" -> "+o.child);
					includeState(o,r,deg,z,ru,pg);
				}

				for (Outlink o : outs) {
					if (o.equals(restart)) continue;
//					if (gw != null) gw.writeEdge(u, o.getState(), o.getFeatureList());
					// current pushcounter is passed down, gets incremented and returned, and 
					// on the next for loop iter is passed down again...
					pushCounter = this.dfsPushes(pg,p,r,deg,o.child,pushCounter);
				}
			} catch (LogicProgramException e) {
				backtrace.print(e);
			}
			backtrace.pop(u);
		} else {
			if (log.isDebugEnabled()) log.debug("PUSHPATH exclude "+(pushCounter+1)+" "+u);
		}
		return pushCounter;
	}
	private void includeState(Outlink o, Map<State, Double> r,
			Map<State, Integer> deg, double z, double ru, ProofGraph pg) throws LogicProgramException {
		backtrace.push(o.child);

		boolean followup = !r.containsKey(o.child);
		Dictionary.increment(r, o.child, moveProbability * (o.wt / z) * ru,"(elided)");
		if(followup) {
			try {
				int degree = pg.pgDegree(o.child,true,true);
				deg.put(o.child,degree); // trueloop, restart
			} catch (LogicProgramException e) {
//				backtrace.print(e);
			}
		}
		if (deg.get(o.child) == 0)
			throw new LogicProgramException("Zero degree for "+o.child);
		backtrace.pop(o.child);
	}
	public double getAlpha() {
		return this.minAlpha;
	}
}
