package edu.cmu.ml.praprolog.learn.tools;

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
		return 1.0;
	}

	@Override
	public String toString() { return "ReLU"; }

	@Override
	public double projection(double rw, double alpha, int nonRestartNodeNum) {
		return rw * (1 - alpha) / (alpha * nonRestartNodeNum);
	}
}
