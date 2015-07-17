package edu.cmu.ml.proppr.learn.tools;

public class Exp extends SquashingFunction {
	
	@Override
	public double compute(double sum) {
		return Math.exp(sum);
	}

	@Override
	public double computeDerivative(double weight) {
		return Math.exp(weight);
	}

	@Override
	public double defaultValue() {
		return 0.0;
	}

	@Override
	public String toString() { return "exponential"; }
}