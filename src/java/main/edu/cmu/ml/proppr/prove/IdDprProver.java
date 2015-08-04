package edu.cmu.ml.proppr.prove;


import java.util.Map;

import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.prove.wam.CachingIdProofGraph;
import edu.cmu.ml.proppr.prove.wam.Goal;
import edu.cmu.ml.proppr.prove.wam.LogicProgramException;
import edu.cmu.ml.proppr.prove.wam.ProofGraph;
import edu.cmu.ml.proppr.prove.wam.State;
import edu.cmu.ml.proppr.prove.wam.StateProofGraph;
import edu.cmu.ml.proppr.util.APROptions;
import edu.cmu.ml.proppr.util.math.LongDense;
import edu.cmu.ml.proppr.util.math.SmoothFunction;

/**
 * prover using depth-first approximate personalized pagerank
 * @author wcohen,krivard
 *
 */
public class IdDprProver extends Prover<CachingIdProofGraph> {
	private static final Logger log = Logger.getLogger(IdDprProver.class);
	public static final double STAYPROB_DEFAULT = 0.0;
	public static final double STAYPROB_LAZY = 0.5;
	private static final boolean TRUELOOP_ON = true;
	protected final double stayProbability;
	protected final double moveProbability;
	protected LongDense.AbstractFloatVector params=null;
	
	@Override
	public String toString() { 
		return String.format("idpr:%.6g:%g", apr.epsilon, apr.alpha);
	}

	public IdDprProver() { this(false); }

	public IdDprProver(boolean lazyWalk) {
		this(lazyWalk,new APROptions());
	}
	public IdDprProver(APROptions apr) {
		this(false, apr);
	}
	public IdDprProver(boolean lazyWalk, APROptions apr) {
		this( (lazyWalk?STAYPROB_LAZY:STAYPROB_DEFAULT),apr);
	}
	protected IdDprProver(double stayP, APROptions apr) {
		super(apr);
		this.stayProbability = stayP;
		this.moveProbability = 1.0-stayProbability;
	}

	public Prover copy() {
		IdDprProver copy = new IdDprProver(this.stayProbability, apr);
		copy.setWeighter(weighter);
		copy.params = this.params;
		return copy;
	}
	@Override
	public Class<CachingIdProofGraph> getProofGraphClass() { return CachingIdProofGraph.class; }
	
	private LongDense.AbstractFloatVector getFrozenParams(CachingIdProofGraph pg) {
		if (params != null) return params;
		if (this.weighter.weights.size()==0) 
			params = new LongDense.UnitVector();
		else 
			params = pg.paramsAsVector(this.weighter.weights,this.weighter.squashingFunction.defaultValue()); // FIXME: default value should depend on f
		return params;
	}

	public Map<State, Double> prove(CachingIdProofGraph pg) {
		LongDense.FloatVector p = new LongDense.FloatVector();
		LongDense.FloatVector r = new LongDense.FloatVector();
		LongDense.AbstractFloatVector params = getFrozenParams(pg);
		int state0 = pg.getRootId();
		r.set( state0, 1.0);
		int numPushes = 0;
		int numIterations = 0;
		double iterEpsilon = 1.0;
		for (int pushCounter = 0; ;) {
			iterEpsilon = Math.max(iterEpsilon/10,apr.epsilon);
			pushCounter = this.proveState(pg,p,r,state0,0,iterEpsilon,params);
			numIterations++;
			if(log.isInfoEnabled()) log.info(Thread.currentThread()+" iteration: "+numIterations+" pushes: "+pushCounter+" r-states: "+r.size()+" p-states: "+p.size());
			if(iterEpsilon == apr.epsilon && pushCounter==0) break;
			numPushes += pushCounter;
		}
		if(log.isInfoEnabled()) log.info(Thread.currentThread()+" total iterations "+numIterations+" total pushes "+numPushes);
		return pg.asMap(p);
	}
	
	
	protected int proveState(CachingIdProofGraph cg, LongDense.FloatVector p, LongDense.FloatVector r,
													 int uid, int pushCounter, double iterEpsilon,LongDense.AbstractFloatVector params) 
	{
		return proveState(cg, p, r, uid, pushCounter, 1, iterEpsilon, params);
	}

	protected int proveState(CachingIdProofGraph cg, LongDense.FloatVector p, LongDense.FloatVector r,
													 int uid, int pushCounter, int depth, double iterEpsilon,
													 LongDense.AbstractFloatVector params)
	{

		try {
			int deg = cg.getDegreeById(uid);
			if (r.get(uid) / deg > iterEpsilon) {
				pushCounter += 1;
				try {
					double z = cg.getTotalWeightOfOutlinks(uid, params, this.weighter.squashingFunction);
					// push this state as far as you can
					while( r.get(uid)/deg > iterEpsilon ) {
						double ru = r.get(uid);
						if (log.isDebugEnabled()) log.debug(String.format("Pushing eps %f @depth %d ru %.6f deg %d state %s", iterEpsilon, depth, ru, deg, uid));
						
						// p[u] += alpha * ru
						//addToP(p,u,ru);
						p.inc(uid,apr.alpha * ru);
						// r[u] *= (1-alpha) * stay?
						//r.put(u, (1.0-apr.alpha) * stayProbability * ru);
						r.set(uid, (1.0-apr.alpha) * stayProbability * ru);
						// for each v near u
						for (int i=0; i<deg; i++) {
							// r[v] += (1-alpha) * move? * Muv * ru
							//Dictionary.increment(r, o.child, (1.0-apr.alpha) * moveProbability * (o.wt / z) * ru,"(elided)");
							double wuv = cg.getIthWeightById(uid,i,params,this.weighter.squashingFunction);
							if (wuv==0) continue;
							int vid = cg.getIthNeighborById(uid,i);
							r.inc(vid, (1.0-apr.alpha) * moveProbability * (wuv/z) * ru);
							if (Double.isNaN(r.get(vid))) log.debug("NaN in r at v="+vid+" wuv="+wuv+" z="+z+" ru="+ru);
						}
						if (log.isDebugEnabled()) {
							// sanity-check r:
							double sumr = 0;
							for (int i=0;i<r.size();i++) { sumr += r.get(i); }
							double sump = 0;
							for (int i=0;i<p.size();i++) { sump += p.get(i); }
							if (Math.abs(sump + sumr - 1.0) > apr.epsilon) {
								log.debug("Should be 1.0 but isn't: after push sum p + r = "+sump+" + "+sumr+" = "+(sump+sumr));
							}
						}
					}

					// for each v near u:
					for (int i=0; i<deg; i++) {
						// proveState(v):
						// current pushcounter is passed down, gets incremented and returned, and 
						// on the next for loop iter is passed down again...
						int vid = cg.getIthNeighborById(uid,i);
						if (vid!=cg.getRootId()) {
							//pushCounter = this.proveState(pg,p,r,o.child,pushCounter,depth+1,iterEpsilon);
							pushCounter = proveState(cg,p,r,vid,pushCounter,depth+1,iterEpsilon,params);
						}
					}
				} catch (LogicProgramException e) {
					throw new IllegalStateException(e);
				}
			} else { 
				if (log.isDebugEnabled()) log.debug(String.format("Rejecting eps %f @depth %d ru %.6f deg %d state %s", iterEpsilon, depth, r.get(uid), deg, uid));
			}
		} catch (LogicProgramException e) {
			throw new IllegalStateException(e);
		}
		return pushCounter;
	}
	
	public double getAlpha() {
		return apr.alpha;
	}
}
