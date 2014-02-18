package edu.cmu.ml.praprolog.learn;

public class TanhWeightingScheme extends WeightingScheme {

	@Override
	public double edgeWeightFunction(double product) {
		return Math.tanh(product);
	}

	@Override
	public double derivEdgeWeightFunction(double weight) {
		return (1 - edgeWeightFunction(weight) * edgeWeightFunction(weight));
	}

}
