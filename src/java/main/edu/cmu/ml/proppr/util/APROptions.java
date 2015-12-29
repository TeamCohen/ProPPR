package edu.cmu.ml.proppr.util;
import edu.cmu.ml.proppr.learn.tools.FixedWeightRules;

public class APROptions {
	public static final double EPS_DEFAULT = 1e-4, MINALPH_DEFAULT=0.1;
	public static final int MAXDEPTH_DEFAULT=20;
	private enum names {
		eps,
		epsilon,
		alph,
		alpha,
		depth
	}
	public int maxDepth;
	public double alpha;
	public double epsilon;
	
	public APROptions() {
		this(EPS_DEFAULT,MINALPH_DEFAULT,MAXDEPTH_DEFAULT);
	}
	public APROptions(double eps, double minalph,
			int depth) {
		this.epsilon = eps;
		this.alpha = minalph;
		this.maxDepth = depth;
	}
	public APROptions(String...optionValues) {
		this();
		for (String o : optionValues) {
			this.set(o.split("="));
		}
	}
	public void set(String...setting) {
		switch(names.valueOf(setting[0])) {
		case eps:
		case epsilon:
			this.epsilon = Double.parseDouble(setting[1]); return;
		case alph:
		case alpha:
			this.alpha = Double.parseDouble(setting[1]); return;
		case depth:
			this.maxDepth = Integer.parseInt(setting[1]); return;
		default:
			throw new IllegalArgumentException("No option to set '"+setting[0]+"'");
		}
	}
}
