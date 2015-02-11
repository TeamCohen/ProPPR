package edu.cmu.ml.proppr.learn;

import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.graph.ArrayLearningGraph;
import edu.cmu.ml.proppr.graph.LearningGraph;
import edu.cmu.ml.proppr.learn.tools.LossData;
import edu.cmu.ml.proppr.learn.tools.LossData.LOSS;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.ParamVector;
import edu.cmu.ml.proppr.util.SRWOptions;
import edu.cmu.ml.proppr.util.SymbolTable;
import gnu.trove.iterator.TIntDoubleIterator;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;

public class SRWRevision {	
	private static final Logger log = Logger.getLogger(SRWRevision.class);
	private static final double BOUND = 1.0e-15; //Prevent infinite log loss.
	private static Random random = new Random();
	public static void seed(long seed) { random.setSeed(seed); }
	protected Set<String> untrainedFeatures;
	protected int epoch;
	protected SRWOptions c;
	protected LossData cumloss;
	public SRWRevision() { this(new SRWOptions()); }
	public SRWRevision(int maxT) { this(new SRWOptions(maxT)); }
	public SRWRevision(SRWOptions params) {
		this.c = params;
		this.epoch = 1;
		this.untrainedFeatures = new TreeSet<String>();
		this.cumloss = new LossData();
		if (log.isDebugEnabled()) {
			log.warn("SRW loss tracking not threadsafe with debug logging enabled! Any reported losses will be totally bogus!");
		}
	}
	
	/**
	 * Modify the parameter vector paramVec by taking a gradient step along the dir suggested by this example.
	 * @param weightVec
	 * @param posNegRWExample
	 */
	public void trainOnExample(ParamVector paramVec, PosNegRWExample example) {
		SgdExample sgdex = new SgdExample(example);
		load(paramVec, sgdex);
		inference(sgdex);
		sgd(paramVec, sgdex);
	}

	
	/** fills M, dM in ex **/
	protected void load(ParamVector params, SgdExample ex) {
		initializeFeatures(params, ex);
		ex.M = new double[ex.g.node_hi][];
		// use compact extendible arrays here while we accumulate; convert to primitive array later
		TIntArrayList dM_features = new TIntArrayList();
		TDoubleArrayList dM_values = new TDoubleArrayList();
		for (int uid = 0; uid < ex.g.node_hi; uid++) {
			// (a); (b): initialization
			double tu = 0;
			TIntDoubleMap dtu = new TIntDoubleHashMap();
			int udeg = ex.g.node_near_hi[uid] - ex.g.node_near_lo[uid];
			double[] suv = new double[udeg];
			double[][] dfu = new double[udeg][];
			// begin (c): for each neighbor v of u,
			for(int eid = ex.g.node_near_lo[uid], xvi = 0; eid < ex.g.node_near_hi[uid]; eid++, xvi++) {
				int vid = ex.g.edge_dest[eid];
				// i. s_{uv} = w * phi_{uv}, a scalar:
				suv[xvi] = 0;
				for (int fid = ex.g.edge_labels_lo[eid]; fid < ex.g.edge_labels_hi[eid]; fid++) {
					suv[xvi] += ex.g.label_feature_weight[fid] * params.get(ex.g.featureLibrary.getSymbol(ex.g.label_feature_id[fid]));
				}
				// ii. t_u += f(s_{uv}), a scalar:
				tu += c.weightingScheme.edgeWeight(suv[xvi]);
				// iii. df_{uv} = f'(s_{uv})* phi_{uv}, a vector, as sparse as phi_{uv}
				// by looping over features i in phi_{uv}
				double [] dfuv = new double[ex.g.edge_labels_hi[eid] - ex.g.edge_labels_lo[eid]] ;
				double cee = c.weightingScheme.derivEdgeWeight(suv[xvi]);
				for (int fid = ex.g.edge_labels_lo[eid], dfuvi = 0; fid < ex.g.edge_labels_hi[eid]; fid++, dfuvi++) {
					// iii. again
					dfuv[dfuvi] = cee * params.get(ex.g.featureLibrary.getSymbol(ex.g.label_feature_id[fid]));
					// iv. dt_u += df_{uv}, a vector, a sparse as sum_{v'} phi_{uv'}
					// by looping over features i in df_{uv} 
					// (identical to features i in phi_{uv}, so we use the same loop)
					dtu.adjustOrPutValue(fid, dfuv[dfuvi], dfuv[dfuvi]); // remember to dereference fid with g.label_feature_id before using!
				}
				dfu[xvi] = dfuv;
			}
			// end (c)
			
			// begin (d): for each neighbor v of u,
			ex.dM_lo[uid] = new int[udeg];
			ex.dM_hi[uid] = new int[udeg];
			ex.M[uid] = new double[udeg];
			double scale = 1 / (tu*tu);
			for(int eid = ex.g.node_near_lo[uid], xvi = 0; eid < ex.g.node_near_hi[uid]; eid++, xvi++) {
				int vid = ex.g.edge_dest[eid];
				ex.dM_lo[uid][xvi] = dM_features.size();
				// create the vector dM_{uv} = (1/t^2_u) * (t_u * df_{uv} - f(s_{uv}) * dt_u)
				// by looping over features i in dt_u
				for (int fid = ex.g.edge_labels_lo[eid], dfuvi = 0; fid < ex.g.edge_labels_hi[eid]; fid++, dfuvi++) {
					dM_features.add(fid);
					double dMuvi = scale * (tu * dfu[xvi][dfuvi] - c.weightingScheme.edgeWeight(suv[xvi]) * dtu.get(fid));
					dM_values.add(dMuvi);
				}
				ex.dM_hi[uid][xvi] = dM_features.size();
				// also create the scalar M_{uv} = f(s_{uv}) / t_u
				ex.M[uid][xvi] = c.weightingScheme.edgeWeight(suv[xvi]) / tu;
			}
		}
		// discard extendible version in favor of primitive array
		ex.dM_feature_id = dM_features.toArray();
		ex.dM_value = dM_values.toArray();
	}
	
	/** adds new features to params vector @ 1% random perturbation */
	protected void initializeFeatures(ParamVector params, SgdExample ex) {
		for (String f : ex.ex.getGraph().getFeatureSet()) {
			if (!params.containsKey(f)) {
				params.put(f,c.weightingScheme.defaultWeight()+ (trainable(f) ? 0.01*random.nextDouble() : 0));
			}
		}
	}
	
	/** fills p, dp */
	protected void inference(SgdExample ex) {
		for (int i=0; i<c.maxT; i++) {
			inferenceUpdate(ex);
		}
	}
	protected void inferenceUpdate(SgdExample ex) {
		double[] pNext = new double[ex.g.node_hi];
		TIntDoubleMap[] dNext = new TIntDoubleMap[ex.g.node_hi];
		for (int uid = 0; uid < ex.g.node_hi; uid++) {
			// p: 2(a)
			pNext[uid] += c.apr.alpha * Dictionary.safeGet(ex.ex.getQueryVec(), uid, 0.0);
			for(int eid = ex.g.node_near_lo[uid], xvi = 0; eid < ex.g.node_near_hi[uid]; eid++, xvi++) {
				// p: 2(b)
				pNext[uid] += (1-c.apr.alpha) * ex.M[uid][xvi] * ex.p[xvi];
				// d: i.
				dNext[uid] = new TIntDoubleHashMap(ex.dM_hi[uid][xvi] - ex.dM_lo[uid][xvi]);
				for (int dmi = ex.dM_lo[uid][xvi]; dmi < ex.dM_hi[uid][xvi]; dmi++) {
					double inc = (1-c.apr.alpha) * ex.dM_value[dmi] * ex.p[xvi];
					dNext[uid].adjustOrPutValue(ex.dM_feature_id[dmi], inc, inc);
				}
				// d: ii.
				for (TIntDoubleIterator it = ex.dp[ex.g.edge_dest[eid]].iterator(); it.hasNext();) {
					it.advance();
					double inc = (1-c.apr.alpha) * ex.M[uid][xvi] * it.value();
					dNext[uid].adjustOrPutValue(it.key(),inc,inc);
				}
			}
		}
		ex.p = pNext;
		ex.dp = dNext;
	}
	
	/** edits params */
	protected void sgd(ParamVector params, SgdExample ex) {
		Set<String> features = this.localFeatures(params, ex);
		TIntDoubleMap gradient = new TIntDoubleHashMap(features.size());
		// add regularization term
		regularization(params, ex, gradient);
		// add empirical loss gradient term
		double pmax = 0;
		for (int a : ex.ex.getPosList()) {
			double pa = clip(ex.p[a]);
			if(pa > pmax) pmax = pa;
			for (TIntDoubleIterator da = ex.dp[a].iterator(); da.hasNext(); ) {
				da.advance();
				double aterm = -da.value() / pa;
				gradient.adjustOrPutValue(da.key(), aterm, aterm);
			}
			this.cumloss.add(LOSS.LOG, -Math.log(pa));
			
		}

		//negative instance booster
		double h = pmax + c.delta;
		double beta = 1;
		if(c.delta < 0.5) beta = (Math.log(1/h))/(Math.log(1/(1-h)));

		for (int b : ex.ex.getNegList()) {
			double pb = clip(ex.p[b]);
			for (TIntDoubleIterator db = ex.dp[b].iterator(); db.hasNext(); ) {
				db.advance();
				double bterm = beta * db.value() / (1 - pb);
				gradient.adjustOrPutValue(db.key(), bterm, bterm);
			}
			this.cumloss.add(LOSS.LOG, -Math.log(1.0-pb));
		}
		// apply gradient to param vector
		for (TIntDoubleIterator grad = gradient.iterator(); grad.hasNext(); ) {
			grad.advance();
			String feature = ex.g.featureLibrary.getSymbol(grad.key());
			if (trainable(feature)) params.adjustValue(feature, learningRate() * grad.value());
		}
	}
	
	/** template: update gradient with regularization term */
	protected void regularization(ParamVector params, SgdExample ex, TIntDoubleMap gradient) {}
	
	static class SgdExample {
		PosNegRWExample ex;
		ArrayLearningGraph g;
		
		// length = sum(nodes i) (degree of i) = #edges
		double[][] M;
		
		// length = sum(edges e) (# features on e) = #feature assignments
		int[] dM_feature_id;
		double[] dM_value;
		// length = sum(nodes i) degree of i = #edges
		int[][] dM_lo;
		int[][] dM_hi;
		
		// p[u] = value
		double[] p;
		// dp[u].put(fid, value)
		TIntDoubleMap[] dp;

		public SgdExample(PosNegRWExample example) {
			this.ex = example;
			if (! (example.getGraph() instanceof ArrayLearningGraph))
				throw new IllegalStateException("Revised SRW requires ArrayLearningGraph in streamed examples. Run with --graphClass ArrayLearningGraph.");
			this.g = (ArrayLearningGraph) example.getGraph();
		}
	}
	
	
	//////////////////////////// copypasta from SRW.java:
	
	protected double learningRate() {
		return Math.pow(this.epoch,-2) * c.eta;
	}
	
	public double clip(double prob)
	{
		if(prob <= 0) return BOUND;
		return prob;
	}
	
	public boolean trainable(String feature) {
		return !untrainedFeatures.contains(feature);
	}

//	/**
//	 * Builds a set of features in the specified set that are not on the untrainedFeatures list.
//	 * @param candidates feature names
//	 * @return
//	 */
//	public Set<String> trainableFeatures(Set<String> candidates) {
//		TreeSet<String> result = new TreeSet<String>();
//		for (String f : candidates) {
//			if (trainable(f)) result.add(f);
//		}
//		return result;
//	}
//	/**
//	 * Builds a set of features from the keys of the specified map that are not on the untrainedFeatures list.
//	 * @param paramVec Maps from features names to nonnegative values.
//	 * @return
//	 */
//	public Set<String> trainableFeatures(Map<String,?> paramVec) {
//		return trainableFeatures(paramVec.keySet());
//	}

	/** Allow subclasses to filter feature list **/
	public Set<String> localFeatures(ParamVector<String,?> paramVec, SgdExample example) {
		return paramVec.keySet();
	}

	public Set<String> untrainedFeatures() { return this.untrainedFeatures; }
}
