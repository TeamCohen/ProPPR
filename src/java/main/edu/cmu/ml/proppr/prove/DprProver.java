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
	// necessary to avoid rounding errors when rescaling reset weight
	private static final double ALPHA_BUFFER = 1e-16;
	public static final double STAYPROB_DEFAULT = 0.0;
	public static final double STAYPROB_LAZY = 0.5;
	private static final boolean TRUELOOP_ON = true;
	private static final boolean RESTART_ON = true;
	protected APROptions apr;
	protected final double stayProbability;
	protected final double moveProbability;
	// for timing traces
	protected long last;
	// for debug
	protected Backtrace<State> backtrace = new Backtrace<State>(log);
	protected ProofGraph current;

	@Override
	public String toString() { 
		return String.format("dpr:%.6g:%g:%s", apr.epsilon, apr.alpha, apr.alphaErrorStrategy.name());
		//return "dprprover(eps="+this.epsilon+", minAlpha="+apr.alpha+", strat="+minAlphaErrorStrategy+")"; 
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
		this.apr = apr;
		this.stayProbability = stayP;
		this.moveProbability = 1.0-stayProbability;
	}

	public Prover copy() {
		DprProver copy = new DprProver(this.stayProbability, apr);
		copy.setWeighter(weighter);
		return copy;
	}


	public Map<State, Double> prove(ProofGraph pg) {
		this.current = pg;

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
		
		//clear state
		this.current = null;
		return p;
	}
	
	/**
	 * z = sum[edges] f( sum[features] theta_feature * weight_feature )
	 * rw = f( sum[resetfeatures] theta_feature * weight_feature )
	 *    = f( theta_alphaBooster * weight_alphaBooster + sum[otherfeatures] theta_feature * weight_feature )
	 * nonBoosterReset = sum[otherfeatures] theta_feature * weight_feature = f_inv( rw ) - theta_alphaBooster * weight_alphaBooster
	 * 
	 * assert rw_new / z = alpha
	 * then:
	 * 
	 * f( theta_alphaBooster * newweight_alphaBooster + sum[otherfeatures] theta_feature * weight_feature ) = alpha * z
	 * theta_alphaBooster * newweight_alphaBooster + f_inv( rw ) - theta_alphaBooster * oldweight_alphaBooster = f_inv( alpha * z )
	 * newweight_alphaBooster = (1/theta_alphaBooster) * (f_inv( alpha * z) - (f_inv( rw ) - theta_alphaBooster))
	 * 
	 * NB f_inv( alpha*z ) - nonBoosterReset < 0 when default reset weight is high relative to z;
	 * when this is true, no reset boosting is necessary and we can set newweight_alphaBooster = 0.
	 * @param currentAB
	 * @param z
	 * @param rw
	 * @return
	 */
	protected double rescaleAlphaBooster(double currentAB, double z, double rw) {
		double thetaAB = Dictionary.safeGet(this.weighter.weights,ProofGraph.ALPHABOOSTER,this.weighter.weightingScheme.defaultWeight());
		double nonBoosterReset = this.weighter.weightingScheme.inverseEdgeWeightFunction(rw) - thetaAB * currentAB;
		double numerator = (this.weighter.weightingScheme.inverseEdgeWeightFunction( (this.apr.alpha + ALPHA_BUFFER) * z ) - nonBoosterReset); 
		return Math.max(0,numerator / thetaAB);
	}
	protected int dfsPushes(ProofGraph pg, Map<State,Double> p, Map<State, Double> r,
			Map<State, Integer> deg, State u, int pushCounter) {
		return dfsPushes(pg, p, r, deg, u, pushCounter, 1);
	}
	protected int dfsPushes(ProofGraph pg, Map<State,Double> p, Map<State, Double> r,
			Map<State, Integer> deg, State u, int pushCounter, int depth) {
		double ru = r.get(u);
		if (ru / deg.get(u) > apr.epsilon) {
			backtrace.push(u);
//			if (log.isInfoEnabled()) {
//				long now = System.currentTimeMillis(); 
//				log.info("push "+pushCounter+"->"+(pushCounter+1)+" ru "+ru+" "+r.size()+" r-states u "+u);
//				last = now;
				if (log.isDebugEnabled()) log.debug("PUSHPATH on "+ru+"@"+depth+" "+u);
//			}
			pushCounter += 1;
			Outlink restart=null;
			try {
				List<Outlink> outs = pg.pgOutlinks(u, TRUELOOP_ON, RESTART_ON);
				double unNormalizedAlpha = 0.0;
				double z = 0.0;
				double rawz = 0.0;
				double m = 0.0;
				for (Outlink o : outs) {
					o.wt = this.weighter.w(o.fd);
					z += o.wt;
					m = Math.max(m,o.wt);
					if (o.child.equals(pg.getStartState())) {
						restart = o;
					}
				}
				if (restart==null) {
					throw new IllegalStateException("No restart link in walk from "+u);
				}

				// scale alphaBooster feature using current weighting scheme
				if (restart.fd.containsKey(ProofGraph.ALPHABOOSTER)) {
					double newAB = rescaleAlphaBooster(restart.fd.get(ProofGraph.ALPHABOOSTER), z, restart.wt);
//					log.warn("Default  booster: "+restart.fd.get(ProofGraph.ALPHABOOSTER));
//					log.warn("Rescaled booster: "+newAB);
					restart.fd.put(ProofGraph.ALPHABOOSTER,newAB);
					restart.wt = this.weighter.w(restart.fd);
				}
				
				unNormalizedAlpha = restart.wt;
				double localAlpha = unNormalizedAlpha / z;

				if (localAlpha < apr.alpha) {
					
					TreeMap<Goal,Integer> featureDist = new TreeMap<Goal,Integer>();
					for (Outlink o : outs) {
						for (Goal g : o.fd.keySet()) Dictionary.increment(featureDist,g);
					}
					for (Map.Entry<Goal,Integer> f : featureDist.entrySet()) {
						log.warn(String.format("% 5d:%20s %5f",f.getValue(),f.getKey(),Dictionary.safeGet(this.weighter.weights,f.getKey())));
					}
					
					log.warn("minAlpha problem, strategy="+apr.alphaErrorStrategy+": localAlpha="+localAlpha+"; unAlpha="+unNormalizedAlpha+"; z="+z);
					if (apr.alphaErrorStrategy==APROptions.ALPHA_STRATEGY.adjust) {
						log.warn("decreasing minAlpha from "+apr.alpha+" to "+localAlpha);
						apr.alpha = localAlpha;
					} else if (apr.alphaErrorStrategy==APROptions.ALPHA_STRATEGY.boost) {
						// figure out how much we need to increment the unNormalizedAlpha to get to minAlpha
						if (log.isDebugEnabled()) {
							log.debug("minAlpha issue: minAlpha="+apr.alpha+" localAlpha="+localAlpha
									+" max outlink weight="+m+"; numouts="+outs.size()+"; unAlpha=<unsupported>; z="+z);
						}
						// figure out how much to boost
						double nonresetWeightSum = z - unNormalizedAlpha;
						double amountToBoost = (apr.alpha*(nonresetWeightSum + unNormalizedAlpha) - unNormalizedAlpha)/(1.0 - apr.alpha);
						z += amountToBoost;
						unNormalizedAlpha += amountToBoost;
						localAlpha = unNormalizedAlpha/z;
						// save boost amount in feature dict component.
						// (Outlinks are passed by reference from the proof graph, so fd updates go directly to the grounded output)
						restart.fd.put(pg.ALPHABOOSTER, this.weighter.boosted(pg.ALPHABOOSTER, unNormalizedAlpha, restart.fd));
						double w = this.weighter.w(restart.fd);
						log.warn("boosted to localAlpha="+localAlpha+"; unAlpha="+unNormalizedAlpha+"("+w+"); z="+z);
					} else {
						log.warn("max outlink weight="+m+"; numouts="+outs.size()+"; unAlpha="+unNormalizedAlpha+"; z="+z);
						log.warn("ru="+ru+"; degu="+deg.get(u)+"; u="+u);
						throw new MinAlphaException(apr.alpha,localAlpha,u);
					}
				}
				addToP(p,u,ru);
				r.put(u, ru * stayProbability * (1.0-apr.alpha));

				restart.wt = ( z * (localAlpha - apr.alpha) );
				if (log.isDebugEnabled()) log.debug("PUSHPATH deg "+outs.size()+"@"+depth);
				for (Outlink o : outs) {
					if (log.isDebugEnabled()) log.debug("PUSHPATH add "+moveProbability * (o.wt / z) * ru+"@"+depth+" "+ o.child);
//					if (log.isDebugEnabled()) log.debug("PUSHPATH candidate "+(pushCounter+1)+" "+u+" -> "+o.child);
					includeState(o,r,deg,z,ru,pg);
				}

				for (Outlink o : outs) {
					if (o.equals(restart)) continue;
					// current pushcounter is passed down, gets incremented and returned, and 
					// on the next for loop iter is passed down again...
					pushCounter = this.dfsPushes(pg,p,r,deg,o.child,pushCounter,depth+1);
				}
			} catch (LogicProgramException e) {
				backtrace.print(e);
			}
			backtrace.pop(u);
		} else {
			if (log.isDebugEnabled()) log.debug("PUSHPATH remove "+ru+"@"+depth+" "+u);
		}
		return pushCounter;
	}
	
	protected void addToP(Map<State, Double> p, State u, double ru) {
		Dictionary.increment(p,u,apr.alpha * ru,"(elided)");
	}

	protected void includeState(Outlink o, Map<State, Double> r,
			Map<State, Integer> deg, double z, double ru, ProofGraph pg) throws LogicProgramException {
		backtrace.push(o.child);
		Dictionary.increment(r, o.child, moveProbability * (o.wt / z) * ru,"(elided)");
		if(!deg.containsKey(o.child)) {
			try {
				int degree = pg.pgDegree(o.child,true,true);
				deg.put(o.child,degree); // trueloop, restart
			} catch (LogicProgramException e) {
				backtrace.print(e);
			}
		}
		if (deg.get(o.child) == 0)
			throw new LogicProgramException("Zero degree for "+o.child);
		backtrace.pop(o.child);
	}
	public double getAlpha() {
		return apr.alpha;
	}
}
