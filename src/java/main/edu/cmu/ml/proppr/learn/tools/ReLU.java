package edu.cmu.ml.proppr.learn.tools;

public class ReLU<G> extends SquashingFunction<G> {

	@Override
	public double compute(double sum) {
		return Math.max(0,sum);
	}

	@Override
	public double computeDerivative(double weight) {
		double grad = 0;
		if (weight > 0) {grad = 1;}            
		return grad;
	}

	@Override
	public double defaultValue() {
		return 1.0;
	}

	@Override
	public String toString() { return "ReLU"; }
}
