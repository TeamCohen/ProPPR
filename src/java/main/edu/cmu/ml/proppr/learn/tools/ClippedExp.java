package edu.cmu.ml.proppr.learn.tools;

public class ClippedExp<G> extends SquashingFunction<G> {
	private static final double CLIP=Math.exp(1);
	
	@Override
	public double compute(double sum) {
		return sum < 1 ? Math.exp(sum) : CLIP*sum;
	}

	@Override
	public double computeDerivative(double weight) {
		return weight < 1 ? Math.exp(weight) : CLIP;
	}

	@Override
	public double defaultValue() {
		return 0.0;
	}

	@Override
	public String toString() { return "clipped exponential"; }
}
