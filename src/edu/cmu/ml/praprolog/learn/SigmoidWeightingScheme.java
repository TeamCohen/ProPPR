package edu.cmu.ml.praprolog.learn;

public class SigmoidWeightingScheme extends WeightingScheme {

	@Override
	public double edgeWeightFunction(double product) {
		return 1/(1 + Math.exp(-product));
	}

	@Override
	public double derivEdgeWeightFunction(double weight) {
		return edgeWeightFunction(weight) * (1 - edgeWeightFunction(weight));
	}

}
