package edu.cmu.ml.proppr.learn.tools;


public class Sigmoid extends SquashingFunction {

	@Override
	public double compute(double sum) {
		return 1/(1 + Math.exp(-sum));
	}

	@Override
	public double computeDerivative(double weight) {
		return compute(weight) * (1 - compute(weight));
	}

	@Override
	public double defaultValue() {
		return 0.0;
	}

	@Override
	public String toString() { return "sigmoid"; }

	private double logit (double p) {
		return Math.log(p / (1-p));
	}
}
