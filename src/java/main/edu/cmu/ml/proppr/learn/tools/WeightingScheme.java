package edu.cmu.ml.proppr.learn.tools;

import java.util.Map;

import edu.cmu.ml.proppr.util.Dictionary;
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
	public abstract double projection(double rw, double alpha, int nonRestartNodeNum);
	
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
		return Math.max(0,edgeWeightFunction(sum));
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
		return Math.max(0, edgeWeightFunction(sum));
	}
	
	/** Support method for learning
	 * 
	 * @param sum
	 * @return
	 */
	public double edgeWeight(double sum) {
		return Math.max(0,edgeWeightFunction(sum));
	}
}
