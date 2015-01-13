package edu.cmu.ml.praprolog.learn.tools;

public class TanhWeightingScheme extends WeightingScheme {

	@Override
	public double edgeWeightFunction(double sum) {
		// kmm & th 30 july 2014 to make return >=0
		return Math.max(0,Math.tanh(sum));
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

	@Override
	public double projection(double rw, double alpha, int nonRestartNodeNum) {
		return arcTanh(rw * (1 - alpha) / (alpha * nonRestartNodeNum));
	}
	private double arcTanh (double z) {
		return 0.5 * (Math.log(1.0 + z) - Math.log(1.0 - z));
	}
}
