package edu.cmu.ml.proppr.prove;

import edu.cmu.ml.proppr.prove.wam.State;

public class MinAlphaException extends IllegalStateException {
	public MinAlphaException(double minAlpha, double localAlpha, State u) {
		super("minAlpha too high! Decrease minAlpha by setting it in --apr, or use the boost or adjust alpha strategies in --prover. "+
                		"dpr minAlpha ="+minAlpha+" localAlpha="+localAlpha+" for state "+u);
	}

	public MinAlphaException(String string) {
		super(string);
	}
}
