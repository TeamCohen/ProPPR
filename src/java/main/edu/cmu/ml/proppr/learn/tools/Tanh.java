package edu.cmu.ml.proppr.learn.tools;

public class Tanh extends SquashingFunction {

	@Override
	public double compute(double sum) {
		// kmm & th 30 july 2014 to make return >=0
		return Math.max(0,Math.tanh(sum));
	}

	@Override
	public double computeDerivative(double weight) {
		return (1 - compute(weight) * compute(weight));
	}

	@Override
	public double defaultValue() {
		return 0.0;
	}

	@Override
	public String toString() { return "tanh"; }

//	@Override
//	public double projection(double rw, double alpha, int nonRestartNodeNum) {
//		return arcTanh(rw * (1 - alpha) / (alpha * nonRestartNodeNum));
//	}
	private double arcTanh (double z) {
		if (z>1 || z<-1) return -Double.MAX_VALUE;
		return 0.5 * (Math.log(1.0 + z) - Math.log(1.0 - z));
	}
}
