package edu.cmu.ml.proppr.learn.tools;

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

//	@Override
//	public double projection(double rw, double alpha, int nonRestartNodeNum) {
//		return Math.log(rw * (1 - alpha) / (alpha * nonRestartNodeNum));
//	}
	
	@Override
	public double inverseEdgeWeightFunction(double x) {
		return Math.log(x);
	}
}