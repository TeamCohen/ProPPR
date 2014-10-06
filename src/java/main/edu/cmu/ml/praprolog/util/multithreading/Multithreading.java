package edu.cmu.ml.praprolog.util.multithreading;

import java.io.BufferedWriter;
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
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

public class Multithreading<In,Out> {
	public static final int NO_THROTTLE=-1;
	public static final int DEFAULT_THROTTLE=NO_THROTTLE;
	public Logger log;
	
	public Multithreading(Logger l) { this.log = l; }

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
	public void executeJob(int nThreads,Iterable<In> streamer,Transformer<In,Out> transformer,String outputFile, int throttle) throws IOException {
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
	public void executeJob(int nThreads,Iterable<In> streamer,Transformer<In,Out> transformer,Cleanup<Out> cleanup,int throttle) {
		ExecutorService transformerPool = Executors.newFixedThreadPool(nThreads);
		ExecutorService cleanupPool = Executors.newFixedThreadPool(1);

		ArrayDeque<Future<?>> cleanupQueue = new ArrayDeque<Future<?>>();
		
		int id=0;
		for (In item : streamer) {
			id++;

			tidyQueue(cleanupQueue);
			if (throttle > 0 && cleanupQueue.size() > throttle) {
				int wait = 100;
				log.debug("Throttling @"+id+"...");
				while(cleanupQueue.size() > throttle) {
					try {
						Thread.sleep(wait);
					} catch (InterruptedException e) {
						log.error("Interrupted while throttling tasks");
					}
					tidyQueue(cleanupQueue);
					wait *= 1.5;
				}
				log.debug("Throttling complete "+wait);
			}
			
			log.debug("Adding "+id);
			Future<Out> transformerFuture = transformerPool.submit(transformer.transformer(item, id));
			Future<?> cleanupFuture = cleanupPool.submit(cleanup.cleanup(transformerFuture, id));
			cleanupQueue.add(cleanupFuture);
		}
		transformerPool.shutdown();
		cleanupPool.shutdown();
		try {
			log.debug("Finishing cleanup...");
			cleanupPool.awaitTermination(7, TimeUnit.DAYS);
			log.debug("Cleanup finished.");
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
