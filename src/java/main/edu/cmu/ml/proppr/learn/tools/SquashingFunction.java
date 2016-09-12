package edu.cmu.ml.proppr.learn.tools;

import java.util.Map;

import edu.cmu.ml.proppr.graph.LearningGraph;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.math.ParamVector;
import gnu.trove.iterator.TObjectDoubleIterator;
import gnu.trove.map.TObjectDoubleMap;

public abstract class SquashingFunction<F> {
	/** Wrapper functions must deliver a value >= 0 */
	public abstract double compute(double x);
	public abstract double computeDerivative(double weight);
	public abstract double defaultValue();
//	public abstract double fixedValue();
	
	/** Support method for proving
	 * 
	 * @param params
	 * @param features
	 * @return
	 */
	public double edgeWeight(Map<F,Double> params, Map<F,Double> features) {
		double ret = 0.0;
		for (Map.Entry<F,Double> f : features.entrySet()) {
			ret += Dictionary.safeGet(params, f.getKey(), this.defaultValue()) * f.getValue(); // this is wrong. f.getValue() is only used if f.key is not in the parameter map. wtf
		}
		ret = compute(ret);
		if (Double.isInfinite(ret)) return Double.MAX_VALUE;
		return Math.max(0, ret);
	}
	
	/** Support method for learning
	 * 
	 * @param sum
	 * @return
	 */
	public double edgeWeight(double sum) {
		return Math.max(0,compute(sum));
	}
	
	/** Support method for learning
	 * 
	 * @param params
	 * @param features
	 * @return
	 */
	public double edgeWeight(LearningGraph g, int eid,
			ParamVector<String, ?> params) {
		double ret = 0.0;
		// iterate over the features on the edge
		for(int fid = g.edge_labels_lo[eid]; fid<g.edge_labels_hi[eid]; fid++) {
			ret += Dictionary.safeGet(params, 
					g.featureLibrary.getSymbol(g.label_feature_id[fid]), 
					this.defaultValue()) * g.label_feature_weight[fid];
		}
		ret = compute(ret);
		if (Double.isInfinite(ret)) return Double.MAX_VALUE;
		return Math.max(0, ret);
	}
}
