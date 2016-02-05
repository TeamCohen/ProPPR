package edu.cmu.ml.proppr.util.multithreading;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;


public abstract class Cleanup<Result> {
	private static final int LOGUPDATE_MS = 5000;
	protected int count=0;
	public abstract Runnable cleanup(Future<Result> in, int id);
	public Logger getLog() { return null; }
	
	public Runnable cleanup(Future<Result> in, ExecutorService cleanupPool, int id) {
		return new TryCleanup(in, cleanup(in,id), cleanupPool, id);
	}

	public class TryCleanup implements Runnable {
		Runnable wrapped;
		ExecutorService cleanupPool;
		int id;
		Future<Result> input;
		public TryCleanup(Future<Result> in, Runnable r, ExecutorService p, int id) {
			this.input = in;
			this.wrapped = r;
			this.cleanupPool = p;
			this.id = id;
		}
		@Override
		public void run() {
			if (cleanupPool != null) {
				try {
					Result result = input.get(50, TimeUnit.MILLISECONDS);
				} catch (TimeoutException e) {
					// if we timeout, resubmit the job
					if (getLog() != null && getLog().isDebugEnabled()) getLog().debug("Rescheduling #"+id);
					cleanupPool.submit(this);
					return;
				} catch (InterruptedException | ExecutionException e) { return; }
			}
			
			// otherwise pass to the wrapped runnable:
			wrapped.run();
		}
	}
}
