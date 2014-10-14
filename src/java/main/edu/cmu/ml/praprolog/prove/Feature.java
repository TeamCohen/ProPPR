package edu.cmu.ml.praprolog.prove;

public class Feature {
	public String functor;
	public int arity;
	public Argument[] args;
	private int ai;
	public Feature(String f, int a) {
		functor = f;
		arity = a;
		args = new Argument[arity];
		ai = 0;
	}
	public void append(Argument a) {
		args[ai] = a;
		ai++;
	}
}
