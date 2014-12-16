package edu.cmu.ml.proppr.util;

public class APROptions {
	public static final double EPS_DEFAULT = 1e-4, MINALPH_DEFAULT=0.1;
	public static final int MAXDEPTH_DEFAULT=5;
	public static final ALPHA_STRATEGY ALPHA_STRATEGY_DEFAULT=ALPHA_STRATEGY.exception;
	private enum names {
		eps,
		epsilon,
		alph,
		alpha,
		depth,
		strat,
		strategy
	}
	public static enum ALPHA_STRATEGY {
		adjust, // reduces minalpha (inference only)
		boost, // adds weight to alphaBooster
		suppress, // reduces non-reset weight (learning only)
		exception
	}
	public int maxDepth;
	public double alpha;
	public double epsilon;
	public APROptions.ALPHA_STRATEGY alphaErrorStrategy;
	
	public APROptions() {
		this(EPS_DEFAULT,MINALPH_DEFAULT,MAXDEPTH_DEFAULT,ALPHA_STRATEGY_DEFAULT);
	}
	public APROptions(double eps, double minalph,
			int depth, ALPHA_STRATEGY strat) {
		this.epsilon = eps;
		this.alpha = minalph;
		this.maxDepth = depth;
		this.alphaErrorStrategy = strat;
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
		case strat:
		case strategy:
			this.alphaErrorStrategy = ALPHA_STRATEGY.valueOf(setting[1]); return;
		default:
			throw new IllegalArgumentException("No option to set '"+setting[0]+"'");
		}
	}
}
