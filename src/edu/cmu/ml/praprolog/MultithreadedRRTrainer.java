package edu.cmu.ml.praprolog;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;

import edu.cmu.ml.praprolog.learn.PosNegRWExample;
import edu.cmu.ml.praprolog.learn.SRW;

public class MultithreadedRRTrainer<T> extends Trainer<T> {
	private static final Logger log = Logger.getLogger(MultithreadedRRTrainer.class);
	public static final int DEFAULT_CAPACITY = 16;
	public static final float DEFAULT_LOAD = (float) 0.75;
	protected int nthreads = 1;
	protected TrainingRun currentTrainingRun;
	
	public MultithreadedRRTrainer(SRW<PosNegRWExample<T>> learner, int numThreads) {
		super(learner);
		nthreads = numThreads;
		log.info("training with "+numThreads + " threads and learner "+learner.getClass());
	}

	@Override
	public Map<String, Double> trainParametersOnCookedIterator(
			Iterable<PosNegRWExample<T>> importCookedExamples, int numEpochs, boolean traceLosses) {
		return trainParametersOnCookedIterator(importCookedExamples, new ConcurrentHashMap<String,Double>(DEFAULT_CAPACITY,DEFAULT_LOAD,this.nthreads), numEpochs, traceLosses);
	}
	
	@Override
	protected void setUpEpochs(Map<String,Double> paramVec) {
		currentTrainingRun = new TrainingRun(paramVec);
	}
	
	@Override
	protected void setUpExamples(int epoch) {
		super.setUpExamples(epoch);
		if (currentTrainingRun.executor != null) {
			throw new IllegalStateException("template called out of order! Must clean up last example set using cleanUpExamples()");
		}
		
//		currentTrainingRun.trainers = new ArrayList<TrainerThread>();
		currentTrainingRun.queue = new ArrayList<TrainerExample>();
	}

	@Override
	protected void doExample(int k, PosNegRWExample<T> x, Map<String,Double> paramVec, boolean traceLosses) {
//		if (currentTrainingRun.trainers == null) {
//			throw new IllegalStateException("template called out of order! Call setUpExamples() first");
//		}
		currentTrainingRun.queue
			.add(new TrainerExample(this.learner, currentTrainingRun, x, traceLosses));
		
	}
	
	@Override
	protected void cleanUpExamples(int epoch) {
//		int n=0;
		int nX = currentTrainingRun.queue.size();
		currentTrainingRun.executor = Executors.newFixedThreadPool(nthreads);
		currentTrainingRun.futures = new ArrayDeque<Future>();
		int k=0;
		for (TrainerExample ex : currentTrainingRun.queue) {
			currentTrainingRun.futures.add(currentTrainingRun.executor.submit(new TrainerThread(ex,k)));
			k++;
		}
		
		currentTrainingRun.executor.shutdown();
		k=0;
		for(Future f; (f=currentTrainingRun.futures.poll()) != null; k++) {
			try {
				log.debug("Joining on example "+k);
				f.get();
				log.debug("Joining on example "+k+" ***joined");
			} catch (InterruptedException e) {
				log.warn("While waiting for example "+k,e);
			} catch (ExecutionException e) {
				log.warn("While waiting for example "+k,e);
			}
		}
		currentTrainingRun.executor = null;
	}
	
	public synchronized void traceLosses(SRW<PosNegRWExample<T>> learner, Map<String,Double> paramVec, PosNegRWExample<T> example) {
		totalLossThisEpoch += learner.empiricalLoss(paramVec, example); 
		numExamplesThisEpoch += example.length();
	}
	
	public class TrainingRun {
		public Queue<Future> futures;
		public ExecutorService executor;
		public TrainingRun(Map<String, Double> p) {
			paramVec = p;
		}
		public Map<String,Double> paramVec;
		public List<TrainerExample> queue = new ArrayList<TrainerExample>();
	}
	
	public class TrainerExample {
		public final SRW<PosNegRWExample<T>> learner;
		public final PosNegRWExample<T> example;
		public final boolean traceLosses;
		public final TrainingRun run;
		public TrainerExample(SRW<PosNegRWExample<T>> el, TrainingRun tr, PosNegRWExample<T> x, boolean t) {
			this.learner = el;
			this.run = tr;
			this.example = x;
			this.traceLosses = t;
		}
	}
	
	public class TrainerThread implements Runnable {
		TrainerExample example;
		int n=0;
		public TrainerThread(TrainerExample e, int exampleNumber) { 
			example = e;
			n=exampleNumber;
		}
		@Override
		public void run() {
			if (log.isInfoEnabled()) log.info("Training on example "+n);
			example.learner.trainOnExample(example.run.paramVec, example.example);

			if (example.traceLosses) {
				traceLosses(example.learner, example.run.paramVec, example.example);
			}
		}
	}
}