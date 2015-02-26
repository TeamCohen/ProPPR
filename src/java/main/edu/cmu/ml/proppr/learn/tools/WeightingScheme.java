package edu.cmu.ml.proppr.learn.tools;

import java.util.Map;

import edu.cmu.ml.proppr.graph.ArrayLearningGraph;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.ParamVector;
import gnu.trove.iterator.TObjectDoubleIterator;
import gnu.trove.map.TObjectDoubleMap;

public abstract class WeightingScheme<F> {
	public static final int WEIGHT_SIGMOID=0;
	public static final int WEIGHT_TANH=1;
	public static final int WEIGHT_LINEAR = 2;
	/** Wrapper functions must deliver a value >= 0 */
	public abstract double edgeWeightFunction(double sum);
	public abstract double derivEdgeWeight(double weight);
	public abstract double defaultWeight();
	public abstract double inverseEdgeWeightFunction(double x);
//	public abstract double projection(double rw, double alpha, int nonRestartNodeNum);
	
	/** Support method for proving
	 * 
	 * @param params
	 * @param features
	 * @return
	 */
	public double edgeWeight(Map<F,Double> params, Map<F,Double> features) {
		double ret = 0.0;
		for (Map.Entry<F,Double> f : features.entrySet()) {
			ret += Dictionary.safeGet(params, f.getKey(), this.defaultWeight()) * f.getValue();
		}
		ret = edgeWeightFunction(ret);
		if (Double.isInfinite(ret)) return Double.MAX_VALUE;
		return Math.max(0, ret);
	}
	
//	/** Support method for tests
//	 * 
//	 * @param params
//	 * @param features
//	 * @return
//	 */
//	public double edgeWeight(Map<F,Double> params, TObjectDoubleMap<F> features) {
//		double sum = 0.0;
//		for (TObjectDoubleIterator<F> f = features.iterator(); f.hasNext();) {
//			f.advance();
//			sum += Dictionary.safeGet(params, f.key(), this.defaultWeight()) * f.value();
//		}
//		return Math.max(0, edgeWeightFunction(sum));
//	}
	
	/** Support method for learning
	 * 
	 * @param sum
	 * @return
	 */
	public double edgeWeight(double sum) {
		return Math.max(0,edgeWeightFunction(sum));
	}
	
	/** Support method for learning
	 * 
	 * @param params
	 * @param features
	 * @return
	 */
	public double edgeWeight(ArrayLearningGraph g, int eid,
			ParamVector<String, ?> params) {
		double ret = 0.0;
		// iterate over the features on the edge
		for(int fid = g.edge_labels_lo[eid]; fid<g.edge_labels_hi[eid]; fid++) {
			ret += Dictionary.safeGet(params, 
					g.featureLibrary.getSymbol(g.label_feature_id[fid]), 
					this.defaultWeight()) * g.label_feature_weight[fid];
		}
		ret = edgeWeightFunction(ret);
		if (Double.isInfinite(ret)) return Double.MAX_VALUE;
		return Math.max(0, ret);
	}
}
