package edu.cmu.ml.proppr.learn.tools;

/**
 * Leaky ReLU nonlinearity based on
 * Rectifier Nonlinearities Improve Neural Network Acoustic Models
 * Maas, Hannun, Ng
 * http://web.stanford.edu/~awni/papers/relu_hybrid_icml2013_final.pdf
 * 
 * See also the ReLU/LReLU sections in
 * http://cs231n.github.io/neural-networks-1/#actfun
 * @author krivard
 *
 * @param <G>
 */
public class LReLU<G> extends SquashingFunction<G> {
	private static final double LEAK=0.01;
	@Override
	public double compute(double sum) {
		return Math.max(LEAK*sum,sum);
	}

	@Override
	public double computeDerivative(double weight) {
		double grad = LEAK;
		if (weight > 0) {grad = 1;}            
		return grad;
	}

	@Override
	public double defaultValue() {
		return 1.0;
	}

	@Override
	public String toString() { return "leaky ReLU"; }
}
