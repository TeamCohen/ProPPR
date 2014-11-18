package edu.cmu.ml.proppr.prove.v1;

public class MinAlphaException extends IllegalStateException {
	public MinAlphaException(double minAlpha, double localAlpha, LogicProgramState u) {
		super("minAlpha too high! Did you remember to set alpha in logic program components? "+
                		"dpr minAlpha ="+minAlpha+" localAlpha="+localAlpha+" for state "+u);
	}
	public MinAlphaException(String s) {
		super(s);
	}
	public MinAlphaException(String s, Exception cause) {
		super(s,cause);
	}
}
