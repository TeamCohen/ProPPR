package edu.cmu.ml.proppr.util.multithreading;

import java.util.concurrent.Callable;

public abstract class Transformer<From,To> {
	public abstract Callable<To> transformer(From in, int id);
}
