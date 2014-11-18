package edu.cmu.ml.proppr.learn.tools;


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

	@Override
	public String toString() { return "sigmoid"; }

	@Override
	public double projection(double rw, double alpha, int nonRestartNodeNum) {
		return logit(rw * (1 - alpha) / (alpha * nonRestartNodeNum));
	}	
	private double logit (double p) {
		return Math.log(p / (1-p));
	}
}
