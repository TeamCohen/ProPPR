package edu.cmu.ml.proppr.prove.wam;
/**
 * Substitute for mixed-type feature-accumulator from Python
 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
 *
 */
public class FeatureBuilder {
	public String functor;
	public int arity;
	public int[] args;
	private int ai;
	public double wt=1.0;
	public FeatureBuilder(String f, int a) {
		functor = f;
		arity = a;
		args = new int[arity];
		ai = 0;
	}
	public void append(int a) {
		args[ai] = a;
		ai++;
	}
}
