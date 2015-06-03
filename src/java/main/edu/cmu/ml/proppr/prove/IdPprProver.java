package edu.cmu.ml.proppr.prove;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.prove.wam.LogicProgramException;
import edu.cmu.ml.proppr.prove.wam.Outlink;
import edu.cmu.ml.proppr.prove.wam.ProofGraph;
import edu.cmu.ml.proppr.prove.wam.State;
import edu.cmu.ml.proppr.util.APROptions;
import edu.cmu.ml.proppr.util.Dictionary;

import edu.cmu.ml.proppr.util.LongDense;
import edu.cmu.ml.proppr.util.SmoothFunction;

/**
 * prover using power iteration
 * @author "William Cohen <wcohen@cs.cmu.edu>"
 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
 *
 */
public class IdPprProver extends Prover {
	private static final double SEED_WEIGHT = 1.0;
	private static final Logger log = Logger.getLogger(IdPprProver.class);
	private static final boolean DEFAULT_TRACE=false;
	private static final boolean RESTART = true;
	private static final boolean TRUELOOP = true;
	protected boolean trace;
	
	public IdPprProver() { this(DEFAULT_TRACE); }
	public IdPprProver(boolean tr) {
		init(tr);
	}
	public IdPprProver(APROptions apr) { super(apr); init(DEFAULT_TRACE); }
	public IdPprProver(FeatureDictWeighter w, APROptions apr, boolean tr) {
		super(w, apr);
		init(tr);
	}
	private void init(boolean tr) {
		trace=tr;
	}
	
	@Override
	public String toString() { return "ippr:"+this.apr.maxDepth; }
	
	public Prover copy() {
		Prover copy = new IdPprProver(weighter, this.apr, this.trace);
		return copy;
	}
	
	public void setMaxDepth(int i) {
		this.apr.maxDepth = i;
	}
	public void setTrace(boolean b) {
		this.trace = b;
	}

	@Override
	public Map<State, Double> prove(ProofGraph pg) 
	{

		ProofGraph.CachingIdGraph cg = new ProofGraph.CachingIdGraph(pg);
		LongDense.FloatVector startVec = new LongDense.FloatVector();
		startVec.set( cg.getRootId(), SEED_WEIGHT );

		SmoothFunction f = new SmoothFunction() {
				@Override public double compute(double x) {
					return x>=0.0 ? x : 0;
				}
			};
		LongDense.UnitVector params = new LongDense.UnitVector();
		LongDense.FloatVector vec = startVec;

		LongDense.FloatVector nextVec = new LongDense.FloatVector();
		LongDense.FloatVector tmp;

		for (int i=0; i<this.apr.maxDepth; i++) {
			// vec = walkOnce(cg,vec,params,f);
			walkOnceBuffered(cg,vec,nextVec,params,f);
			// save vec as the next buffer, then point vec at the new result
			tmp = vec;
			tmp.clear();
			vec = nextVec;
			// now use the saved space as buffer next iteration
			nextVec = tmp;
			//System.out.println("ippr iter "+(i+1)+" size "+vec.size());
		}

		return cg.asMap(vec);
	}

	LongDense.FloatVector walkOnce(ProofGraph.CachingIdGraph cg, LongDense.FloatVector vec,LongDense.AbstractFloatVector params,SmoothFunction f) 
	{
		LongDense.FloatVector nextVec = new LongDense.FloatVector(vec.size());
		nextVec.set( cg.getRootId(), apr.alpha * SEED_WEIGHT );
		try {
			for (int uid=cg.getRootId(); uid<vec.size(); uid++) {
				double vu = vec.get(uid);
				if (vu >= 0.0) {
					double z = cg.getTotalWeightOfOutlinks(uid, params, f);
					int d = cg.getDegreeById(uid);
					for (int i=0; i<d; i++) {
						double wuv = cg.getIthWeightById(uid,i,params,f);
						int vid = cg.getIthNeighborById(uid,i);
						nextVec.inc(vid, vu*(1.0-apr.alpha)*(wuv/z));
					}
				}
			}
		} catch (LogicProgramException ex) {
				throw new IllegalStateException(ex);			
		}
		return nextVec;
	}

	void walkOnceBuffered(ProofGraph.CachingIdGraph cg, 
												LongDense.FloatVector vec,LongDense.FloatVector nextVec,
												LongDense.AbstractFloatVector params,SmoothFunction f) 
	{
		nextVec.clear();
		nextVec.set( cg.getRootId(), apr.alpha * SEED_WEIGHT );
		try {
			for (int uid=cg.getRootId(); uid<vec.size(); uid++) {
				double vu = vec.get(uid);
				if (vu >= 0.0) {
					double z = cg.getTotalWeightOfOutlinks(uid, params, f);
					int d = cg.getDegreeById(uid);
					for (int i=0; i<d; i++) {
						double wuv = cg.getIthWeightById(uid,i,params,f);
						int vid = cg.getIthNeighborById(uid,i);
						nextVec.inc(vid, vu*(1.0-apr.alpha)*(wuv/z));
					}
				}
			}
		} catch (LogicProgramException ex) {
				throw new IllegalStateException(ex);			
		}
	}
}
