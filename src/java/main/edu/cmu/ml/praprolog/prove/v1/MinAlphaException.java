package edu.cmu.ml.praprolog.prove.v1;

public class MinAlphaException extends IllegalStateException {
	public MinAlphaException(double minAlpha, double localAlpha, LogicProgramState u) {
		super("minAlpha too high! Did you remember to set alpha in logic program components? "+
                		"dpr minAlpha ="+minAlpha+" localAlpha="+localAlpha+" for state "+u);
	}
}
