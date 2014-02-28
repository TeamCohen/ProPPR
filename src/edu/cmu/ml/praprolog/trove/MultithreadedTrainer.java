package edu.cmu.ml.praprolog.trove;

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

import edu.cmu.ml.praprolog.trove.learn.L2PosNegLossTrainedSRW;
import edu.cmu.ml.praprolog.trove.learn.PosNegRWExample;
import edu.cmu.ml.praprolog.trove.learn.SRW;
import edu.cmu.ml.praprolog.util.Dictionary;

public class MultithreadedTrainer extends Trainer {
	private static final Logger log = Logger.getLogger(MultithreadedTrainer.class);
	protected int nthreads = 1;
	protected TrainingRun currentTrainingRun;
	
	public MultithreadedTrainer(SRW<PosNegRWExample> learner, int numThreads) {
		super(learner);
		nthreads = numThreads;
	}

	@Override
	public Map<String, Double> trainParametersOnCookedIterator(
			Collection<PosNegRWExample> importCookedExamples, int numEpochs, boolean traceLosses) {
		return trainParametersOnCookedIterator(importCookedExamples, new ConcurrentHashMap<String,Double>(), numEpochs, traceLosses);
	}
	
	@Override
	protected void setUpEpochs(Map<String,Double> paramVec) {
		currentTrainingRun = new TrainingRun(paramVec);
	}
	
	@Override
	protected void setUpExamples(int epoch, Collection<PosNegRWExample> examples) {
		super.setUpExamples(epoch, examples);
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
	protected void doExample(int k, PosNegRWExample x, Map<String,Double> paramVec, boolean traceLosses) {
		if (currentTrainingRun.threads == null) {
			throw new IllegalStateException("template called out of order! Call setUpExamples() first");
		}
		currentTrainingRun.queues.get(k % nthreads)
			.add(new TrainerExample(this.learner, currentTrainingRun, x, traceLosses));
		
	}
	
	@Override
	protected void cleanUpExamples(int epoch) {
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
				th.join(); // will finish adding any new threads in uncaughtexceptionhandlers before returning
				currentTrainingRun.threads.remove(0);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		currentTrainingRun.threads = null;
	}
	
	public synchronized void traceLosses(SRW<PosNegRWExample> learner, Map<String,Double> paramVec, PosNegRWExample example) {
		totalLossThisEpoch += learner.empiricalLoss(paramVec, example); 
		numExamplesThisEpoch += example.length();
	}
	
	public class TrainingRun {
		public TrainingRun(Map<String, Double> p) {
			paramVec = p;
		}
		public Map<String,Double> paramVec;
		public List<Thread> threads = null;
		public List<List<TrainerExample>> queues = new ArrayList<List<TrainerExample>>();
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
	
	/////////////////////////// Running ////////////////////////////////
	
//	private static final String USAGE = "Usage:\n\tcookedExampleFile outputParamFile [options]\n"
//			+"\t\t--epochs {int}   Number of epochs (default 5)\n"
//			+"\t\t--traceLosses    Turn on traceLosses (default off)\n"
//			+"\t\t                   NB: example count for losses is sum(x.length() for x in examples)\n"
//			+"\t\t                   and won't match `wc -l cookedExampleFile`\n"
//			+"\t\t--threads {int}  Number of threads (default 4)\n"
//			+"\t\t--rr             Use round-robin scheduling (default:queue)\n";
//	private static void usage() {
//		System.err.println(USAGE);
//		System.exit(0);
//	}
//	public static void main(String[] args) {
//		if (args.length < 2) {
//			usage();
//		}
//		
//		String cookedExampleFile = args[0];
//		String outputParamFile   = args[1];
//		int epochs = 5;
//		int threads = 4;
//		boolean traceLosses = false;
//		boolean roundRobin = false;
//		if (args.length > 2) {
//			for (int i=2; i<args.length; i++) {
//				if ("--epochs".equals(args[i])) {
//					if (i+1<args.length) epochs = Integer.parseInt(args[++i]);
//					else usage();
//				} else if ("--traceLosses".equals(args[i])) {
//					traceLosses = true;
//				} else if ("--threads".equals(args[i])) {
//					if (i+1<args.length) threads = Integer.parseInt(args[++i]);
//					else usage();
//				} else if ("--rr".equals(args[i])) {
//					roundRobin = true;
//				} else usage();
//			}
//		}
//		
////		L2PosNegLossTrainedSRW<String> srw = new L2PosNegLossTrainedSRW<String>();
////		Trainer<String> trainer = new MultithreadedTrainer<String>(srw,threads);
////		Map<String,Double> paramVec = trainer.trainParametersOnCookedIterator(
////				trainer.importCookedExamples(cookedExampleFile, new AnnotatedGraphFactory<String>(AnnotatedGraphFactory.STRING)),
////				epochs,
////				traceLosses);
//		
//		
//		//wait for keypress
////		System.out.println("Press any key to start.");
////		Reader in = new InputStreamReader(System.in);
////		try {
////			in.read();
////		} catch (IOException e) {
////			// TODO Auto-generated catch block
////			e.printStackTrace();
////			System.exit(1);
////		}
//		
//		L2PosNegLossTrainedSRW srw = new L2PosNegLossTrainedSRW();
//		Trainer trainer = null;
//		if (roundRobin) {
//			trainer = new MultithreadedRRTrainer(srw,threads);
//		} else {
//			trainer = new MultithreadedTrainer(srw,threads);
//		}
////		Trainer trainer = new MultithreadedTrainer(srw,threads);
//		Map<String,Double> paramVec = trainer.trainParametersOnCookedIterator(
//				trainer.importCookedExamples(cookedExampleFile),
//				epochs,
//				traceLosses);
//		Dictionary.save(paramVec, outputParamFile);
//	}
}