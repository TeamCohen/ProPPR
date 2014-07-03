package edu.cmu.ml.praprolog.learn;

public class ReLUWeightingScheme extends WeightingScheme {

	@Override
	public double edgeWeightFunction(double sum) {
		return Math.max(0,sum);
	}

	@Override
	public double derivEdgeWeight(double weight) {
		double grad = 0;
		if (weight > 0) {grad = 1;}            
		return grad;
	}

	@Override
	public double defaultWeight() {
		return 0.0;
	}

	@Override
	public String toString() { return "ReLU"; }
}
