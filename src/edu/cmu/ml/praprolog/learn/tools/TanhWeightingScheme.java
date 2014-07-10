package edu.cmu.ml.praprolog.learn.tools;

public class TanhWeightingScheme extends WeightingScheme {

	@Override
	public double edgeWeightFunction(double sum) {
		return Math.tanh(sum);
	}

	@Override
	public double derivEdgeWeight(double weight) {
		return (1 - edgeWeightFunction(weight) * edgeWeightFunction(weight));
	}

	@Override
	public double defaultWeight() {
		return 0.0;
	}

	@Override
	public String toString() { return "tanh"; }
}
