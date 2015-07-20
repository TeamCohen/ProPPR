package edu.cmu.ml.proppr.util.multithreading;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

public class Multithreading<In,Out> {
	public static final int NO_THROTTLE=-1;
	public static final int DEFAULT_THROTTLE=NO_THROTTLE;
	public static final boolean ORDER_MAINTAIN=true;
	public static final boolean DEFAULT_ORDER=ORDER_MAINTAIN;
	/** Note this log is NOT static to the class, but is 
	 * passed in by the caller. 
	 */
	public Logger log;
	private boolean maintainOrder;
	
	public Multithreading(Logger l) {
		this(l, DEFAULT_ORDER);
	}
	public Multithreading(Logger l, boolean ordered) {
		this.log = l;
		this.maintainOrder = ordered;
	}

	/** Runs the specified transformer on each item in the streamer and blocks until complete (no throttling).
	 * Output is written to the specified file; make sure transformer transforms to String.
	 * 
	 * @param nThreads
	 * @param streamer
	 * @param transformer
	 * @param outputFile
	 * @throws IOException 
	 */
	@SuppressWarnings("unchecked")
	public void executeJob(int nThreads,Iterable<In> streamer,Transformer<In,Out> transformer,String outputFile) throws IOException {
		Writer w = new BufferedWriter(new FileWriter(outputFile));
		executeJob(nThreads, streamer, transformer, (Cleanup<Out>) new WritingCleanup(w, this.log), DEFAULT_THROTTLE);
		w.close();
	}
	
	/** Runs the specified transformer on each item in the streamer and blocks until complete. 
	 * 
	 * Throttles input when output queue grows beyond the specified size.
	 * 
	 * Output is written to the specified file; make sure transformer transforms to String.
	 * 
	 * @param nThreads
	 * @param streamer
	 * @param transformer
	 * @param outputFile
	 * @param throttle
	 * @throws IOException 
	 */
	@SuppressWarnings("unchecked")
	public void executeJob(int nThreads,Iterable<In> streamer,Transformer<In,Out> transformer,File outputFile, int throttle) throws IOException 
	{
		Writer w = new BufferedWriter(new FileWriter(outputFile));
		executeJob(nThreads, streamer, transformer, (Cleanup<Out>) new WritingCleanup(w, this.log), throttle);
		w.close();
	}
	
	/**
	 * 
	 * @param nThreads
	 * @param streamer
	 * @param transformer
	 * @param cleanup
	 * @param throttle
	 */
	public void executeJob(int nThreads,Iterable<In> streamer,Transformer<In,Out> transformer,Cleanup<Out> cleanup,int throttle) 
	{
		log.info("executeJob:"
						 +" streamer: "+streamer.getClass().getCanonicalName()
						 +" transformer: "+transformer.getClass().getCanonicalName()
						 +" throttle: "+throttle);
		ExecutorService transformerPool = Executors.newFixedThreadPool(nThreads, new NamedThreadFactory("transformer"));
		ExecutorService cleanupPool = Executors.newFixedThreadPool(1, new NamedThreadFactory("cleanup"));

		ArrayDeque<Future<?>> transformerQueue = new ArrayDeque<Future<?>>();
		
		int id=0;
		if (log.isDebugEnabled()) log.debug("Adding start "+(id+1));
		for (In item : streamer) {
			id++;

			if (throttle > 0) {
				tidyQueue(transformerQueue);
				if(transformerQueue.size() > throttle) {
					int wait = 100;
					if (log.isDebugEnabled()) log.debug("Throttling @"+id+"...");
					while(transformerQueue.size() > throttle) {
						try {
							Thread.sleep(wait);
						} catch (InterruptedException e) {
							log.error("Interrupted while throttling tasks");
						}
						tidyQueue(transformerQueue);
						wait *= 1.5;
					}
					if (log.isDebugEnabled()) log.debug("Throttling complete "+wait);
				}
			}
			
			Future<Out> transformerFuture = transformerPool.submit(transformer.transformer(item, id));
			if (log.isDebugEnabled()) log.debug("Adding done "+(id));
			if (maintainOrder) {
				cleanupPool.submit(cleanup.cleanup(transformerFuture, id));
			} else {
				if (log.isDebugEnabled()) log.debug("Permitting rescheduling of #"+id);
				cleanupPool.submit(cleanup.cleanup(transformerFuture, cleanupPool, id));
			}
			if (throttle > 0) transformerQueue.add(transformerFuture);
			if (log.isDebugEnabled()) log.debug("Adding start "+(id+1));
		}
		
		// first we wait for all transformers to finish
		transformerPool.shutdown();
		try {
			transformerPool.awaitTermination(7, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			log.error("Interrupted?",e);
		}
		// at this point all transformers are complete, so no task in the cleanup pool will need to be rescheduled
		cleanupPool.shutdown();
		try {
			if (log.isDebugEnabled()) log.debug("Finishing cleanup...");
			cleanupPool.awaitTermination(7, TimeUnit.DAYS);
			if (log.isDebugEnabled()) log.debug("Cleanup finished.");
		} catch (InterruptedException e) {
			log.error("Interrupted?",e);
		}
	}
	
	private void tidyQueue(ArrayDeque queue) {
		synchronized(queue) {
			for (Iterator<Future<?>> it = queue.iterator(); it.hasNext();) {
				Future<?> f = it.next();
				if (f.isDone()) it.remove();
			}
		}
	}
}
