package edu.cmu.ml.proppr.prove;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;


import edu.cmu.ml.proppr.prove.MinAlphaException;
import edu.cmu.ml.proppr.prove.wam.Goal;
import edu.cmu.ml.proppr.prove.wam.LogicProgramException;
import edu.cmu.ml.proppr.prove.wam.Outlink;
import edu.cmu.ml.proppr.prove.wam.ProofGraph;
import edu.cmu.ml.proppr.prove.wam.State;
import edu.cmu.ml.proppr.util.APROptions;
import edu.cmu.ml.proppr.util.Dictionary;

/**
 * prover using depth-first approximate personalized pagerank
 * @author wcohen,krivard
 *
 */
public class DprProver extends Prover {
	private static final Logger log = Logger.getLogger(DprProver.class);
	public static final double STAYPROB_DEFAULT = 0.0;
	public static final double STAYPROB_LAZY = 0.5;
	private static final boolean TRUELOOP_ON = true;
	protected final double stayProbability;
	protected final double moveProbability;
	// for timing traces
	protected long last;
	// for debug
	protected Backtrace<State> backtrace = new Backtrace<State>(log);
	protected ProofGraph current;

	@Override
	public String toString() { 
		return String.format("dpr:%.6g:%g", apr.epsilon, apr.alpha);
	}

	public DprProver() { this(false); }

	public DprProver(boolean lazyWalk) {
		this(lazyWalk,new APROptions());
	}
	public DprProver(APROptions apr) {
		this(false, apr);
	}
	public DprProver(boolean lazyWalk, APROptions apr) {
		this( (lazyWalk?STAYPROB_LAZY:STAYPROB_DEFAULT),apr);
	}
	protected DprProver(double stayP, APROptions apr) {
		super(apr);
		this.stayProbability = stayP;
		this.moveProbability = 1.0-stayProbability;
	}

	public Prover copy() {
		DprProver copy = new DprProver(this.stayProbability, apr);
		copy.setWeighter(weighter);
		return copy;
	}


	public Map<State, Double> prove(ProofGraph pg) {
		if (this.current != null) throw new IllegalStateException("DprProver not threadsafe -- one instance per thread only, please!");
		this.current = pg;

		Map<State,Double> p = new HashMap<State,Double>();
		Map<State,Double> r = new HashMap<State,Double>();
		State state0 = pg.getStartState();
		r.put(state0, 1.0);
		backtrace.start();
		int numPushes = 0;
		int numIterations = 0;
		double iterEpsilon = 1;
		for(int pushCounter = 0; ;) {
			iterEpsilon = Math.max(iterEpsilon/10,apr.epsilon);
			last = System.currentTimeMillis();
			if(log.isDebugEnabled()) log.debug("Starting iteration with eps = "+iterEpsilon);
			pushCounter = this.proveState(pg,p,r,state0,0,iterEpsilon);
			numIterations++;
			if(log.isInfoEnabled()) log.info("Iteration: "+numIterations+" pushes: "+pushCounter+" r-states: "+r.size()+" p-states: "+p.size());
			if(iterEpsilon == apr.epsilon && pushCounter==0) break;
			numPushes+=pushCounter;
		}
		if(log.isInfoEnabled()) log.info("total iterations "+numIterations+" total pushes "+numPushes);
		
		//clear state
		this.current = null;
		return p;
	}
	
	
	protected int proveState(ProofGraph pg, Map<State,Double> p, Map<State, Double> r,
			State u, int pushCounter, double iterEpsilon) {
		return proveState(pg, p, r, u, pushCounter, 1, iterEpsilon);
	}
	protected int proveState(ProofGraph pg, Map<State,Double> p, Map<State, Double> r,
			State u, int pushCounter, int depth, double iterEpsilon) {
		try {
			int deg = pg.pgDegree(u);
			if (r.get(u) / deg > iterEpsilon) {
				backtrace.push(u);
				pushCounter += 1;
				try {
					
					List<Outlink> outs = pg.pgOutlinks(u, TRUELOOP_ON);
					double z = 0.0;
					for (Outlink o : outs) {
						o.wt = this.weighter.w(o.fd);
						if (Double.isInfinite(o.wt)) log.warn("Infinite weight at outlink "+o.child+";"
								+Dictionary.buildString(o.fd,new StringBuilder(),"\n\t").toString());
						z += o.wt;
					}
					
					// push this state as far as you can
					while( r.get(u) / deg > iterEpsilon ) {
						double ru = r.get(u);
						if (log.isDebugEnabled()) log.debug(String.format("Pushing eps %f @depth %d ru %.6f deg %d state %s", iterEpsilon, depth, ru, deg, u));
						
						// p[u] += alpha * ru
						addToP(p,u,ru);
						// r[u] *= (1-alpha) * stay?
						r.put(u, (1.0-apr.alpha) * stayProbability * ru);
						
						// for each v near u:
						for (Outlink o : outs) {
							// r[v] += (1-alpha) * move? * Muv * ru
							Dictionary.increment(r, o.child, (1.0-apr.alpha) * moveProbability * (o.wt / z) * ru,"(elided)");
						}
						
						if (log.isDebugEnabled()) {
							// sanity-check r:
							double sumr = 0;
							for (Double d : r.values()) { sumr += d; }
							double sump = 0;
							for (Double d : p.values()) { sump += d; }
							if (Math.abs(sump + sumr - 1.0) > apr.epsilon) {
								log.debug("Should be 1.0 but isn't: after push sum p + r = "+sump+" + "+sumr+" = "+(sump+sumr));
							}
						}
					}

					
					// for each v near u:
					for (Outlink o : outs) {
						// proveState(v):
						// current pushcounter is passed down, gets incremented and returned, and 
						// on the next for loop iter is passed down again...
						if (o.child.equals(pg.getStartState())) continue;
						pushCounter = this.proveState(pg,p,r,o.child,pushCounter,depth+1,iterEpsilon);
					}
				} catch (LogicProgramException e) {
					backtrace.rethrow(e);
				}
				backtrace.pop(u);
			}
		} catch (LogicProgramException e) {
			this.backtrace.rethrow(e);
		}
		return pushCounter;
	}
	
	/** Template: Subclasses may add side effects */
	protected void addToP(Map<State, Double> p, State u, double ru) {
		Dictionary.increment(p,u,apr.alpha * ru,"(elided)");
	}

	public double getAlpha() {
		return apr.alpha;
	}
}
