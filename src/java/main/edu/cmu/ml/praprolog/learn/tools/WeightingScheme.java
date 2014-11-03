package edu.cmu.ml.praprolog.learn.tools;

import java.util.List;
import java.util.Map;

import edu.cmu.ml.praprolog.graph.v1.Feature;
import edu.cmu.ml.praprolog.util.Dictionary;

public abstract class WeightingScheme<G> {
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
	public double edgeWeight(Map<G,Double> params, Map<G,Double> features) {
		double sum = 0.0;
		for (Map.Entry<G,Double> f : features.entrySet()) {
			sum += Dictionary.safeGet(params, f.getKey(), this.defaultWeight()) * f.getValue();
		}
		return edgeWeightFunction(sum);
	}
}
