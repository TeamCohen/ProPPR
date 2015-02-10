package edu.cmu.ml.proppr.learn;

import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.ParamVector;
import edu.cmu.ml.proppr.util.SRWOptions;
import edu.cmu.ml.proppr.util.SymbolTable;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;

public class SRWRevision {	
	private static final Logger log = Logger.getLogger(SRWRevision.class);		
	private static Random random = new Random();
	public static void seed(long seed) { random.setSeed(seed); }
	protected Set<String> untrainedFeatures;
	protected int epoch;
	protected SRWOptions c;	
	public SRWRevision() { this(new SRWOptions()); }
	public SRWRevision(int maxT) { this(new SRWOptions(maxT)); }
	public SRWRevision(SRWOptions params) {
		this.c = params;
		this.epoch = 1;
		this.untrainedFeatures = new TreeSet<String>();
		if (log.isDebugEnabled()) {
			log.warn("SRW loss tracking not threadsafe with debug logging enabled! Any reported losses will be totally bogus!");
		}
	}
	
	/** fills M, dM in ex **/
	protected void load(ParamVector params, SgdExample ex) {
		initializeFeatures(params, ex);
		ex.M = new double[ex.g.node_hi][];
		// use compact extendible arrays here while we accumulate; convert to primitive array later
		TIntArrayList dM_features = new TIntArrayList();
		TDoubleArrayList dM_values = new TDoubleArrayList();
		for (int uid = 0; uid < ex.g.node_hi; uid++) {
			// (a)
			double tu = 0;
			int udeg = ex.g.node_near_hi[uid] - ex.g.node_near_lo[uid];
			// (b)
			TIntDoubleMap dtu = new TIntDoubleHashMap();
			double[] suv = new double[udeg];
			double[][] dfu = new double[udeg][];
			// begin (c)
			for(int eid = ex.g.node_near_lo[uid], xvi = 0; eid < ex.g.node_near_hi[uid]; eid++, xvi++) {
				int vid = ex.g.edge_dest[eid];
				// i.
				suv[xvi] = 0;
				for (int fid = ex.g.edge_labels_lo[eid]; fid < ex.g.edge_labels_hi[eid]; fid++) {
					suv[xvi] += ex.g.label_feature_weight[fid] * params.get(ex.featureNames.getSymbol(ex.g.label_feature_id[fid]));
				}
				// ii.
				tu += c.weightingScheme.edgeWeight(suv[xvi]);
				// iii.
				double [] dfuv = new double[ex.g.edge_labels_hi[eid] - ex.g.edge_labels_lo[eid]] ;
				double cee = c.weightingScheme.derivEdgeWeight(suv[xvi]);
				for (int fid = ex.g.edge_labels_lo[eid], dfuvi = 0; fid < ex.g.edge_labels_hi[eid]; fid++, dfuvi++) {
					// iii. again
					dfuv[dfuvi] = cee * params.get(ex.featureNames.getSymbol(ex.g.label_feature_id[fid]));
					// iv.
					dtu.adjustOrPutValue(fid, dfuv[dfuvi], dfuv[dfuvi]); // remember to dereference fid with g.label_feature_id before using!
				}
				dfu[xvi] = dfuv;
			}
			// end (c)
			
			// begin (d)
			ex.dM_lo[uid] = new int[udeg];
			ex.dM_hi[uid] = new int[udeg];
			ex.M[uid] = new double[udeg];
			double scale = 1 / (tu*tu);
			for(int eid = ex.g.node_near_lo[uid], xvi = 0; eid < ex.g.node_near_hi[uid]; eid++, xvi++) {
				int vid = ex.g.edge_dest[eid];
				ex.dM_lo[uid][xvi] = dM_features.size();
				for (int fid = ex.g.edge_labels_lo[eid], dfuvi = 0; fid < ex.g.edge_labels_hi[eid]; fid++, dfuvi++) {
					dM_features.add(fid);
					double dMuvi = scale * (tu * dfu[xvi][dfuvi] - c.weightingScheme.edgeWeight(suv[xvi])*dtu.get(fid));
					dM_values.add(dMuvi);
				}
				ex.dM_hi[uid][xvi] = dM_features.size();
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
	protected void inference(ParamVector params, SgdExample mdm) {
		
	}
	
	/** edits params */
	protected void sgd(ParamVector params, SgdExample mdm) {
		Regularization r = regularization(params,mdm);
	}
	
	protected Regularization regularization(ParamVector params, SgdExample mdm) {
		return null;
	}
	
	static class Regularization {
		int[] r_feature_id;
		double[] r_value;
	}
	
	static class SgdExample {
		PosNegRWExample ex;
		Graph g;
		SymbolTable<String> featureNames;
		
		// length = sum(nodes i) (degree of i) = #edges
		double[][] M;
		
		// length = sum(edges e) (# features on e) = #feature assignments
		int[] dM_feature_id;
		double[] dM_value;
		// length = sum(nodes i) degree of i = #edges
		int[][] dM_lo;
		int[][] dM_hi;
		
		double[] p;
		double[][] dp;
		
		public SgdExample() {}
	}
	
	static class Graph {
		// length = #feature assignments (= sum(edge) #features on that edge)
		int[] label_feature_id;
		double[] label_feature_weight;
		
		// length = #edges
		int[] edge_dest;
		int[] edge_labels_lo;
		int[] edge_labels_hi;
		
		// length = #nodes
		int[] node_near_lo;
		int[] node_near_hi;
		
		// node_lo = 0;
		int node_hi;
	}
	
	
	//////////////////////////// copypasta from SRW.java:
	
	public boolean trainable(String feature) {
		return !untrainedFeatures.contains(feature);
	}

	/**
	 * Builds a set of features in the specified set that are not on the untrainedFeatures list.
	 * @param candidates feature names
	 * @return
	 */
	public Set<String> trainableFeatures(Set<String> candidates) {
		TreeSet<String> result = new TreeSet<String>();
		for (String f : candidates) {
			if (trainable(f)) result.add(f);
		}
		return result;
	}
	/**
	 * Builds a set of features from the keys of the specified map that are not on the untrainedFeatures list.
	 * @param paramVec Maps from features names to nonnegative values.
	 * @return
	 */
	public Set<String> trainableFeatures(Map<String,?> paramVec) {
		return trainableFeatures(paramVec.keySet());
	}

	/** Allow subclasses to filter feature list **/
	public Set<String> localFeatures(ParamVector<String,?> paramVec, SgdExample example) {
		return paramVec.keySet();
	}

	public Set<String> untrainedFeatures() { return this.untrainedFeatures; }
}
