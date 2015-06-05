package edu.cmu.ml.proppr.util.math;

/**
 * @author wcohen
 */

public abstract class SmoothFunction
{
	/** Integer id of the function **/
	public int functionId() { return -1; }

	/** Compute function **/
	abstract public double compute(double x);

	/** Compute inverse of function **/
	public double computeInverse(double x) {
		throw new UnsupportedOperationException();
	}

	/** Compute derivative of function at x **/
	public double computeDeriv(double x) {
		throw new UnsupportedOperationException();
	}
}
