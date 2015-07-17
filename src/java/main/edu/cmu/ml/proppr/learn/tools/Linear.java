package edu.cmu.ml.proppr.learn.tools;

public class Linear extends SquashingFunction {

	@Override
	public double compute(double sum) {
		return sum;
	}

	@Override
	public double computeDerivative(double weight) {
		return weight;
	}

	@Override
	public double defaultValue() {
		return 1.0;
	}

	@Override
	public String toString() { return "linear"; }
}
