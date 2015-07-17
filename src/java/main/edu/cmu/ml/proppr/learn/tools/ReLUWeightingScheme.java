package edu.cmu.ml.proppr.learn.tools;

public class ReLUWeightingScheme<G> extends WeightingScheme<G> {

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
//		rosecatherinek  Check default of 0
//		return 0.0;
		
	}

	@Override
	public String toString() { return "ReLU"; }

//	@Override
//	public double projection(double rw, double alpha, int nonRestartNodeNum) {
//		return rw * (1 - alpha) / (alpha * nonRestartNodeNum);
//	}

	@Override
	public double inverseEdgeWeightFunction(double x) {
		return x;
	}
}
