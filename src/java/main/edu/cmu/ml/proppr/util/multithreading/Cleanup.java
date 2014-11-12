package edu.cmu.ml.proppr.util.multithreading;

import java.util.concurrent.Future;

public abstract class Cleanup<Result> {

	public abstract Runnable cleanup(Future<Result> in, int id);
}
