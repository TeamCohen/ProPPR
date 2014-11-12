package edu.cmu.ml.proppr.learn.tools;

public class TanhWeightingScheme extends WeightingScheme {

	@Override
	public double edgeWeightFunction(double sum) {
		// kmm & th 30 july 2014 to make return >=0
		return Math.tanh(sum);//+1;
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
