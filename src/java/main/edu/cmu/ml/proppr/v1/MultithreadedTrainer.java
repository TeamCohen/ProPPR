package edu.cmu.ml.proppr.v1;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.graph.v1.AnnotatedGraphFactory;
import edu.cmu.ml.proppr.learn.L2PosNegLossTrainedSRW;
import edu.cmu.ml.proppr.learn.SRW;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.ParamVector;
import edu.cmu.ml.proppr.util.SimpleParamVector;

public class MultithreadedTrainer<T> extends Trainer<T> {
	private static final Logger log = Logger.getLogger(MultithreadedTrainer.class);
	public static final int DEFAULT_CAPACITY = 16;
	public static final float DEFAULT_LOAD = (float) 0.75;
	protected int nthreads = 1;
	protected TrainingRun currentTrainingRun;
	
	public MultithreadedTrainer(SRW<PosNegRWExample<T>> learner, int numThreads) {
		super(learner);
		nthreads = numThreads;
	}

	@Override
	public ParamVector trainParametersOnCookedIterator(
			Iterable<PosNegRWExample<T>> importCookedExamples, int numEpochs, boolean traceLosses) {
		return trainParametersOnCookedIterator(importCookedExamples, new SimpleParamVector(new ConcurrentHashMap<String,Double>(DEFAULT_CAPACITY,DEFAULT_LOAD,this.nthreads)), numEpochs, traceLosses);
	}
	
	@Override
	protected void setUpEpochs(ParamVector paramVec) {
		currentTrainingRun = new TrainingRun(paramVec);
	}
	
	@Override
	protected void setUpExamples(int epoch) {
		super.setUpExamples(epoch);
		if (currentTrainingRun.threads != null) {
			throw new IllegalStateException("template called out of order! Must clean up last example set using cleanUpExamples()");
		}
		
		currentTrainingRun.threads = new ArrayList<Thread>();
		currentTrainingRun.queues.clear();
		for (int k=0; k<nthreads; k++) {
			currentTrainingRun.queues.add(new ArrayList<TrainerExample>());
		}
	}

	@Override
	protected void doExample(int k, PosNegRWExample<T> x, ParamVector paramVec, boolean traceLosses) {
		if (currentTrainingRun.threads == null) {
			throw new IllegalStateException("template called out of order! Call setUpExamples() first");
		}
		currentTrainingRun.queues.get(k % nthreads)
			.add(new TrainerExample(this.learner, currentTrainingRun, x, traceLosses));
		
	}
	
	@Override
	protected void cleanUpExamples(int epoch, ParamVector paramVec) {
//		int n=0;
		for (int k=0; k<nthreads; k++) {
			if (currentTrainingRun.queues.get(k).size() > 0) {
//				n++;
				TrainerThread t = new TrainerThread(currentTrainingRun.queues.get(k), k);
				Thread th = new Thread(t,"xthread "+k);
				th.setDaemon(true);
				th.setUncaughtExceptionHandler(t);
				
				if (log.isDebugEnabled()) log.debug("Starting thread "+k);
				currentTrainingRun.threads.add(th);
				th.start();
			}
		}
		
		while(currentTrainingRun.threads.size() > 0) {
			try {
				Thread th = currentTrainingRun.threads.get(0);
				if (log.isDebugEnabled()) log.debug("Joining thread "+th.getName());
				th.join(); // will finish adding any new threads in uncaughtExceptionHandlers before returning
				currentTrainingRun.threads.remove(0);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		currentTrainingRun.threads = null;
		super.cleanUpExamples(epoch, paramVec);
	}
	
	public synchronized void traceLosses(SRW<PosNegRWExample<T>> learner, ParamVector paramVec, PosNegRWExample<T> example) {
//		totalLossThisEpoch += learner.empiricalLoss(paramVec, example); 
		numExamplesThisEpoch += example.length();
	}
	
	public class TrainingRun {
		public TrainingRun(ParamVector p) {
			paramVec = p;
		}
		public ParamVector paramVec;
		public List<Thread> threads = null;
		public List<List<TrainerExample>> queues = new ArrayList<List<TrainerExample>>();
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
	
	public class TrainerThread implements Runnable, UncaughtExceptionHandler {
		List<TrainerExample> queue;
		int id;
		public TrainerThread(List<TrainerExample> q, int i) {
			queue = new ArrayList<TrainerExample>();
			queue.addAll(q); // defensive copy
			id = i;
		}
		@Override
		public void run() {
			for (int n = queue.size() - 1; n>=0; n--) {
				if (log.isInfoEnabled()) log.info("Training on example "+id+"."+n);
				TrainerExample ex = queue.get(n);
				ex.learner.trainOnExample(ex.run.paramVec, ex.example);

				if (ex.traceLosses) {
//					log.debug(ex.paramVec);
					traceLosses(ex.learner, ex.run.paramVec, ex.example);
				}
				queue.remove(n);
			}
		}
		@Override
		public void uncaughtException(Thread t, Throwable thrw) {
			// log warning for the exception
			StringWriter s = new StringWriter();
			s.write("Uncaught exception in thread "+t.getName()+":\n");
			PrintWriter w = new PrintWriter(s);
			thrw.printStackTrace(w);
			log.warn(s.toString());
			try {
				s.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
			// resume work
			Thread th = new Thread(this,"Rxthread "+this.id);
			th.setDaemon(true);
			th.setUncaughtExceptionHandler(this);
			
			synchronized(currentTrainingRun.threads) {
				log.warn("Resuming thread "+t.getName()+" with "+th.getName());
				currentTrainingRun.threads.add(th); // don't want to join before thread has been started
				th.start();
			}
		}
	}
}