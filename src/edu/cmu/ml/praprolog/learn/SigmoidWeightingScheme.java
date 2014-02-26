package edu.cmu.ml.praprolog.learn;


public class SigmoidWeightingScheme extends WeightingScheme {

	@Override
	public double edgeWeightFunction(double sum) {
		return 1/(1 + Math.exp(-sum));
	}

	@Override
	public double derivEdgeWeight(double weight) {
		return edgeWeightFunction(weight) * (1 - edgeWeightFunction(weight));
	}

	@Override
	public double defaultWeight() {
		return 0.0;
	}

}
