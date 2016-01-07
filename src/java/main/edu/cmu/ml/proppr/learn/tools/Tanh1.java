package edu.cmu.ml.proppr.learn.tools;

public class Tanh1 extends SquashingFunction {

	@Override
	public double compute(double sum) {
		return Math.tanh(sum)+1;
	}

	@Override
	public double computeDerivative(double weight) {
	    double x = Math.tanh(weight);
	    return (1 - x*x);
	}

	@Override
	public double defaultValue() {
		return 0.0;
	}

	@Override
	public String toString() { return "tanh+1"; }
}
