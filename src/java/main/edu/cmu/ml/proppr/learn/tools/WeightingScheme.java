package edu.cmu.ml.proppr.learn.tools;

import java.util.List;
import java.util.Map;

import edu.cmu.ml.proppr.graph.v1.Feature;
import edu.cmu.ml.proppr.util.Dictionary;
import gnu.trove.iterator.TObjectDoubleIterator;
import gnu.trove.map.TObjectDoubleMap;

public abstract class WeightingScheme<F> {
	public static final int WEIGHT_SIGMOID=0;
	public static final int WEIGHT_TANH=1;
	public static final int WEIGHT_LINEAR = 2;
	public abstract double edgeWeightFunction(double sum);
	public abstract double derivEdgeWeight(double weight);
	public abstract double defaultWeight();
	
	/** Support method for learning
	 * 
	 * @param params
	 * @param features
	 * @return
	 */
	public double edgeWeight(Map<String,Double> params, List<Feature> features) {
		double sum = 0.0;
		for (Feature f : features) {
			sum += Dictionary.safeGet(params, f.featureName, this.defaultWeight()) * f.weight;
		}
		return edgeWeightFunction(sum);
	}
	
	/** Support method for proving
	 * 
	 * @param params
	 * @param features
	 * @return
	 */
	public double edgeWeight(Map<F,Double> params, Map<F,Double> features) {
		double sum = 0.0;
		for (Map.Entry<F,Double> f : features.entrySet()) {
			sum += Dictionary.safeGet(params, f.getKey(), this.defaultWeight()) * f.getValue();
		}
		return edgeWeightFunction(sum);
	}
	
	/** Support method for learning
	 * 
	 * @param params
	 * @param features
	 * @return
	 */
	public double edgeWeight(Map<F,Double> params, TObjectDoubleMap<F> features) {
		double sum = 0.0;
		for (TObjectDoubleIterator<F> f = features.iterator(); f.hasNext();) {
			f.advance();
			sum += Dictionary.safeGet(params, f.key(), this.defaultWeight()) * f.value();
		}
		return edgeWeightFunction(sum);
	}
}
