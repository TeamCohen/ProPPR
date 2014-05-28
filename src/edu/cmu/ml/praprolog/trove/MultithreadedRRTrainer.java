package edu.cmu.ml.praprolog.trove;

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

import edu.cmu.ml.praprolog.trove.learn.PosNegRWExample;
import edu.cmu.ml.praprolog.trove.learn.SRW;
import edu.cmu.ml.praprolog.util.ParamVector;
import edu.cmu.ml.praprolog.util.SimpleParamVector;

public class MultithreadedRRTrainer extends Trainer {
	private static final Logger log = Logger.getLogger(MultithreadedRRTrainer.class);
	protected int nthreads = 1;
	protected TrainingRun currentTrainingRun;
	
	public MultithreadedRRTrainer(SRW<PosNegRWExample> learner, int numThreads) {
		super(learner);
		nthreads = numThreads;
	}

	@Override
	public ParamVector trainParametersOnCookedIterator(
			Iterable<PosNegRWExample> importCookedExamples, int numEpochs, boolean traceLosses) {
		return trainParametersOnCookedIterator(importCookedExamples, 
				new SimpleParamVector(new ConcurrentHashMap<String,Double>(
						edu.cmu.ml.praprolog.MultithreadedRRTrainer.DEFAULT_CAPACITY,
						edu.cmu.ml.praprolog.MultithreadedRRTrainer.DEFAULT_LOAD,
						this.nthreads)), 
				numEpochs, traceLosses);
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
	protected void doExample(int k, PosNegRWExample x, Map<String,Double> paramVec, boolean traceLosses) {
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
		int warningCounter = 0;
		final int MAX_WARNINGS = 3;
		for(Future f; (f=currentTrainingRun.futures.poll()) != null; k++) {
			try {
				log.debug("Joining on example "+k);
				f.get();
				log.debug("Joining on example "+k+" ***joined");
			} catch (InterruptedException e) {
			    if (++warningCounter<=MAX_WARNINGS) {
				log.warn("While waiting for example "+k,e);
				if (warningCounter==MAX_WARNINGS) log.warn("that's your last of those warnings....");
			    }
			} catch (ExecutionException e) {
			    if (++warningCounter<=MAX_WARNINGS) {
				log.warn("While waiting for example "+k,e);
				if (warningCounter==MAX_WARNINGS) log.warn("that's your last of those warnings....");
			    }
			}
		}
		currentTrainingRun.executor = null;
	}
	
	public synchronized void traceLosses(SRW<PosNegRWExample> learner, Map<String,Double> paramVec, PosNegRWExample example) {
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
		public final SRW<PosNegRWExample> learner;
		public final PosNegRWExample example;
		public final boolean traceLosses;
		public final TrainingRun run;
		public TrainerExample(SRW<PosNegRWExample> el, TrainingRun tr, PosNegRWExample x, boolean t) {
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