package edu.cmu.ml.praprolog.learn.tools;

public class ExpWeightingScheme extends WeightingScheme {
	
	@Override
	public double edgeWeightFunction(double sum) {
		return Math.exp(sum);
	}

	@Override
	public double derivEdgeWeight(double weight) {
		return Math.exp(weight);
	}

	@Override
	public double defaultWeight() {
		return 0.0;
	}

	@Override
	public String toString() { return "exponential"; }
}
