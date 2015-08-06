package edu.cmu.ml.proppr;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.graph.ArrayLearningGraphBuilder;
import edu.cmu.ml.proppr.graph.LearningGraphBuilder;
import edu.cmu.ml.proppr.learn.AdaGradSRW;
import edu.cmu.ml.proppr.learn.tools.Exp;
import edu.cmu.ml.proppr.learn.tools.LossData;
import edu.cmu.ml.proppr.learn.tools.LossData.LOSS;
import edu.cmu.ml.proppr.learn.tools.RWExampleParser;
import edu.cmu.ml.proppr.learn.tools.StoppingCriterion;
import edu.cmu.ml.proppr.util.Configuration;
import edu.cmu.ml.proppr.util.ModuleConfiguration;
import edu.cmu.ml.proppr.util.ParamsFile;
import edu.cmu.ml.proppr.util.ParsedFile;
import edu.cmu.ml.proppr.util.SRWOptions;
import edu.cmu.ml.proppr.util.math.ParamVector;
import edu.cmu.ml.proppr.util.math.SimpleParamVector;
import edu.cmu.ml.proppr.util.multithreading.Multithreading;
import edu.cmu.ml.proppr.util.multithreading.NamedThreadFactory;

/**
 * Version of the Trainer class which uses Adaptive Sub Gradient method (AdaGrad) instead of 
 * Stochastic Gradient Descent 
 * 
 * @author rosecatherinek
 *
 */
public class AdaGradTrainer {
	private static final Logger log = Logger.getLogger(AdaGradTrainer.class);
	public static final int DEFAULT_CAPACITY = 16;
	public static final float DEFAULT_LOAD = (float) 0.75;
	protected int nthreads = 1;
	protected int throttle;

	protected AdaGradSRW agLearner;
	protected int epoch;
	LossData lossLastEpoch;
	TrainingStatistics statistics=new TrainingStatistics();

	public AdaGradTrainer(AdaGradSRW agLearner, int nthreads, int throttle) {
		this.agLearner = agLearner;
		this.nthreads = Math.max(1, nthreads);
		this.throttle = throttle;

		agLearner.untrainedFeatures().add("fixedWeight");
		agLearner.untrainedFeatures().add("id(trueLoop)");
		agLearner.untrainedFeatures().add("id(trueLoopRestart)");
		agLearner.untrainedFeatures().add("id(restart)");
	}

	public AdaGradTrainer(AdaGradSRW agSRW) {
		this(agSRW, 1, Multithreading.DEFAULT_THROTTLE);
	}

	public class TrainingStatistics {
		int numExamplesThisEpoch = 0;
		int exampleSetSize = 0;
		//		long minReadTime = Integer.MAX_VALUE;
		//		long maxReadTime = 0;
		long readTime = 0;
		//		long minParseTime = Integer.MAX_VALUE;
		//		long maxParseTime = 0;
		long parseTime = 0;
		//		long minTrainTime = Integer.MAX_VALUE;
		//		long maxTrainTime = 0;
		long trainTime = 0;
		void updateReadingStatistics(long time) {
			//			minReadTime = Math.min(time, minReadTime);
			//			maxReadTime = Math.max(time, maxReadTime);
			readTime += time;
		}
		void updateParsingStatistics(long time) {
			//			minParseTime = Math.min(time, minParseTime);
			//			maxParseTime = Math.max(time, maxParseTime);
			parseTime += time;
		}
		void updateTrainingStatistics(long time) {
			//			minTrainTime = Math.min(time, minTrainTime);
			//			maxTrainTime = Math.max(time, maxTrainTime);
			trainTime += time;
			exampleSetSize++;
		}
		synchronized void updateNumExamples(int length) {	
			numExamplesThisEpoch+=length;
		}
		void checkStatistics() {
			int poolSize = Math.max(1,nthreads/2);
			readTime = Math.max(1, readTime);
			parseTime = Math.max(1, parseTime);
			trainTime = Math.max(1, trainTime);
			// we can keep the parsing pool full if we can read $poolSize examples
			// in the time it takes to parse 1 example
			int parseFull = (int) Math.ceil(parseTime / readTime);
			// we can keep the training pool full if parsing takes less time than training
			int trainFull = (int) Math.ceil(trainTime * (nthreads-poolSize) / parseTime);
			if (parseFull < poolSize) log.warn((poolSize-parseFull)+" parsing threads went unused; reading from disk is slow. :(");
			if (trainFull < poolSize) log.warn((poolSize-trainFull)+" training threads went unused; parsing is slow. Ask Katie to enable parsing vs training pool size adjustments.");
		}
	}

	protected ParamVector createParamVector() {
		return new SimpleParamVector<String>(new ConcurrentHashMap<String,Double>(DEFAULT_CAPACITY,DEFAULT_LOAD,this.nthreads));
	}

	public void doExample(PosNegRWExample x, ParamVector<String,?> paramVec, boolean traceLosses) {
		this.agLearner.trainOnExample(paramVec, x);
	}

	public ParamVector train(Iterable<String> examples, LearningGraphBuilder builder, int numEpochs, boolean traceLosses) {
		return train(
				examples,
				builder,
				createParamVector(),
				numEpochs,
				traceLosses
				);
	}

	public ParamVector train(Iterable<String> examples, LearningGraphBuilder builder, ParamVector initialParamVec, int numEpochs, boolean traceLosses) {
		ParamVector paramVec = this.agLearner.setupParams(initialParamVec);
		if (paramVec.size() == 0){
			for (String f : this.agLearner.untrainedFeatures()) paramVec.put(f, this.agLearner.getSquashingFunction().defaultValue());
		}
		
		//@rck AG
		//create a cuncurrent hash map to store the running total of the squares of the gradient
		SimpleParamVector<String> totSqGrad = new SimpleParamVector<String>(new ConcurrentHashMap<String,Double>(DEFAULT_CAPACITY,DEFAULT_LOAD,this.nthreads)); 
		
		NamedThreadFactory parseThreads = new NamedThreadFactory("parse-");
		NamedThreadFactory trainThreads = new NamedThreadFactory("train-");
		int poolSize = Math.max(this.nthreads/2, 1);
		ThreadPoolExecutor parsePool, trainPool;
		ExecutorService cleanPool; 
		TrainingStatistics total = new TrainingStatistics();
		StoppingCriterion stopper = new StoppingCriterion(numEpochs, 1.0, 2);

		// repeat until ready to stop
		while (!stopper.satisified()) {
			// set up current epoch
			this.epoch++;
			this.agLearner.setEpoch(epoch);
			log.info("epoch "+epoch+" ...");

			// reset counters & file pointers
			this.agLearner.clearLoss();
			this.statistics = new TrainingStatistics();
			parseThreads.reset();
			trainThreads.reset();

			// set up separate pools for parsing, training, and tracing losses
			parsePool = new ThreadPoolExecutor(this.nthreads-poolSize,Integer.MAX_VALUE,10,TimeUnit.SECONDS,new LinkedBlockingQueue<Runnable>(),parseThreads);
			//Executors.newFixedThreadPool(this.nthreads-poolSize, parseThreads);
			trainPool = new ThreadPoolExecutor(              poolSize,Integer.MAX_VALUE,10,TimeUnit.SECONDS,new LinkedBlockingQueue<Runnable>(),trainThreads); 
			//Executors.newFixedThreadPool(poolSize, trainThreads);
			cleanPool = Executors.newSingleThreadExecutor();

			// run examples
			int id=1;
			long start = System.currentTimeMillis();
//			for (String s : examples) {
//				statistics.updateReadingStatistics(System.currentTimeMillis()-start);
//				Future<PosNegRWExample> parsed = parsePool.submit(new Parse(s, builder, id));
//				Future<Integer> trained = trainPool.submit(new AdaGradTrain(parsed, paramVec, totSqGrad, agLearner, id));
//				cleanPool.submit(new TraceLosses(trained, id));
//				start = System.currentTimeMillis();
//			}
			int countdown=-1; AdaGradTrainer notify = null;
			for (String s : examples) {
				if (log.isDebugEnabled()) log.debug("Queue size "+(trainPool.getTaskCount()-trainPool.getCompletedTaskCount()));
				statistics.updateReadingStatistics(System.currentTimeMillis()-start);
				if (countdown>0) {
					if (log.isDebugEnabled()) log.debug("Countdown "+countdown);
					countdown--;
				} else if (countdown == 0) {
					if (log.isDebugEnabled()) log.debug("Countdown "+countdown +"; throttling:");
					countdown--;
					notify = null;
					try {
						synchronized(this) {
							if (log.isDebugEnabled()) log.debug("Clearing training queue...");
							while(trainPool.getTaskCount()-trainPool.getCompletedTaskCount() > this.nthreads)
								this.wait();
							if (log.isDebugEnabled()) log.debug("Queue cleared.");
						}
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else if (trainPool.getTaskCount()-trainPool.getCompletedTaskCount() > 1.5*this.nthreads) {
					if (log.isDebugEnabled()) log.debug("Starting countdown");
					countdown=this.nthreads;
					notify = this;
				}
				Future<PosNegRWExample> parsed = parsePool.submit(new Parse(s, builder, id));
				Future<Integer> trained = trainPool.submit(new AdaGradTrain(parsed, paramVec, totSqGrad, agLearner, id, notify));
				cleanPool.submit(new TraceLosses(trained, id));
				id++;
				start = System.currentTimeMillis();
			}
			parsePool.shutdown();
			try {
				parsePool.awaitTermination(7,TimeUnit.DAYS);
				// allocate the threads parsePool was using to finishing off training for this epoch
				//log.info("Reclaiming parser threads...");
				trainPool.setCorePoolSize(this.nthreads); 
				// by default ThreadPoolExecutor only creates new threads on submit() calls, so
				// we must start up our new threads by hand.
				while(trainPool.getActiveCount()>0) { if (!trainPool.prestartCoreThread()) break; }
				trainPool.shutdown();
				trainPool.awaitTermination(7, TimeUnit.DAYS);
				cleanPool.shutdown();
				cleanPool.awaitTermination(7, TimeUnit.DAYS);
			} catch (InterruptedException e) {
				log.error("Interrupted?",e);
			}

			// finish any trailing updates for this epoch
			this.agLearner.cleanupParams(paramVec,paramVec);

			// loss status and signalling the stopper
			if(traceLosses) {
				LossData lossThisEpoch = this.agLearner.cumulativeLoss();
				lossThisEpoch.convertCumulativesToAverage(statistics.numExamplesThisEpoch);
				printLossOutput(lossThisEpoch);
				if (epoch>1) {
					stopper.recordConsecutiveLosses(lossThisEpoch,lossLastEpoch);
				}
				lossLastEpoch = lossThisEpoch;
			}
			stopper.recordEpoch();
			statistics.checkStatistics();
			total.updateReadingStatistics(statistics.readTime);
			total.updateParsingStatistics(statistics.parseTime);
			total.updateTrainingStatistics(statistics.trainTime);
		}
		log.info("Reading: "+total.readTime+" Parsing: "+total.parseTime+" Training: "+total.trainTime + " Num Epochs: " + this.epoch);
		return paramVec;
	}


	protected void printLossOutput(LossData lossThisEpoch) {
		System.out.print("avg training loss " + lossThisEpoch.total()
				+ " on "+ statistics.numExamplesThisEpoch +" examples");
		System.out.print(" =log:reg " + lossThisEpoch.loss.get(LOSS.LOG));
		System.out.print(" : " + lossThisEpoch.loss.get(LOSS.REGULARIZATION));
		if (epoch>1) {
			LossData diff = lossLastEpoch.diff(lossThisEpoch);
			System.out.println(" improved by " + diff.total()
					+ " (log:reg "+diff.loss.get(LOSS.LOG) +":"+diff.loss.get(LOSS.REGULARIZATION)+")");
			double percentImprovement = 100 * diff.total()/lossThisEpoch.total();
			System.out.println("pct reduction in training loss "+percentImprovement);
			// warn if there is a more than 1/2 of 1 percent increase in loss
			if (percentImprovement < -0.5) { 
				System.out.println("WARNING: loss INCREASED by " + percentImprovement +" pct, i.e. total of "+
						(-diff.total()) + " - what's THAT about?");
			}
		} else 
			System.out.println();
	}

	public ParamVector findGradient(Iterable<String> examples, LearningGraphBuilder builder, ParamVector paramVec, SimpleParamVector<String> totSqGrad) {
		log.info("Computing gradient on cooked examples...");
		ParamVector sumGradient = new SimpleParamVector<String>();
		if (paramVec==null) {
			paramVec = createParamVector();
			for (String f : this.agLearner.untrainedFeatures()) paramVec.put(f, 1.0); // FIXME: should this use the weighter default?
		}
		paramVec = this.agLearner.setupParams(paramVec);

		//		
		//		//WW: accumulate example-size normalized gradient
		//		for (PosNegRWExample x : examples) {
		////			this.learner.initializeFeatures(paramVec,x.getGraph());
		//			this.learner.accumulateGradient(paramVec, x, sumGradient);
		//			k++;
		//		}

		NamedThreadFactory parseThreads = new NamedThreadFactory("parse-");
		NamedThreadFactory gradThreads = new NamedThreadFactory("grad-");
		int nthreadsper = Math.max(this.nthreads/2, 1);
		ExecutorService parsePool, gradPool, cleanPool; 

		parsePool = Executors.newFixedThreadPool(nthreadsper, parseThreads);
		gradPool = Executors.newFixedThreadPool(nthreadsper, gradThreads);
		cleanPool = Executors.newSingleThreadExecutor();

		// run examples
		int id=1;
//		for (String s : examples) {
//			Future<PosNegRWExample> parsed = parsePool.submit(new Parse(s, builder, id));
//			Future<Integer> gradfound = gradPool.submit(new Grad(parsed, paramVec, sumGradient, agLearner, id));
//			cleanPool.submit(new TraceLosses(gradfound, id));
//		}
		int countdown=-1; AdaGradTrainer notify = null;
		for (String s : examples) {
			long queueSize = (((ThreadPoolExecutor) gradPool).getTaskCount()-((ThreadPoolExecutor) gradPool).getCompletedTaskCount());
			if (log.isDebugEnabled()) log.debug("Queue size "+queueSize);
			if (countdown>0) {
				if (log.isDebugEnabled()) log.debug("Countdown "+countdown);
				countdown--;
			} else if (countdown == 0) {
				if (log.isDebugEnabled()) log.debug("Countdown "+countdown +"; throttling:");
				countdown--;
				notify = null;
				try {
					synchronized(this) {
						if (log.isDebugEnabled()) log.debug("Clearing training queue...");
						while((((ThreadPoolExecutor) gradPool).getTaskCount()-((ThreadPoolExecutor) gradPool).getCompletedTaskCount()) > this.nthreads)
							this.wait();
						if (log.isDebugEnabled()) log.debug("Queue cleared.");
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else if (queueSize > 1.5*this.nthreads) {
				if (log.isDebugEnabled()) log.debug("Starting countdown");
				countdown=this.nthreads;
				notify = this;
			}
			Future<PosNegRWExample> parsed = parsePool.submit(new Parse(s, builder, id));
			Future<Integer> gradfound = gradPool.submit(new Grad(parsed, paramVec, sumGradient, totSqGrad, agLearner, id, notify));
			cleanPool.submit(new TraceLosses(gradfound, id));
		}
		parsePool.shutdown();
		try {
			parsePool.awaitTermination(7,TimeUnit.DAYS);
			gradPool.shutdown();
			gradPool.awaitTermination(7, TimeUnit.DAYS);
			cleanPool.shutdown();
			cleanPool.awaitTermination(7, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			log.error("Interrupted?",e);
		}

		this.agLearner.cleanupParams(paramVec, sumGradient);

		//WW: renormalize by the total number of queries
		for (Iterator<String> it = sumGradient.keySet().iterator(); it.hasNext(); ) {
			String feature = it.next();
			double unnormf = sumGradient.get(feature);
			// query count stored in numExamplesThisEpoch, as noted above
			double norm = unnormf / this.statistics.numExamplesThisEpoch;
			sumGradient.put(feature, norm);
		}

		return sumGradient;
	}

	public ParamVector findGradient(ArrayList<PosNegRWExample> examples,
			SimpleParamVector<String> simpleParamVector) {
		// TODO Auto-generated method stub
		return null;
	}

	/////////////////////// Multithreading scaffold ///////////////////////

	protected class Parse implements Callable<PosNegRWExample> {
		String in;
		LearningGraphBuilder builder;
		int id;
		public Parse(String in, LearningGraphBuilder builder, int id) {
			this.in=in;
			this.id=id;
			this.builder = builder;
		}
		@Override
		public PosNegRWExample call() throws Exception {
			long start = System.currentTimeMillis();
			if (log.isDebugEnabled()) log.debug("Parsing start "+this.id);
			PosNegRWExample ex = new RWExampleParser().parse(in, builder.copy(), agLearner);
			if (log.isDebugEnabled()) log.debug("Parsing done "+this.id);
			statistics.updateParsingStatistics(System.currentTimeMillis()-start);
			return ex;
		}

	}

	/**
	 * Transforms from inputs to outputs
	 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
	 *
	 */
	protected class AdaGradTrain implements Callable<Integer> {
		Future<PosNegRWExample> in;
		ParamVector paramVec;
		AdaGradSRW agLearner;
		SimpleParamVector<String> totSqGrad;
		int id;
		AdaGradTrainer notify;
		public AdaGradTrain(Future<PosNegRWExample> parsed, ParamVector paramVec, SimpleParamVector<String> totSqGrad, AdaGradSRW agLearner, int id, AdaGradTrainer notify) {
			this.in = parsed;
			this.id = id;
			this.agLearner = agLearner;
			this.totSqGrad = totSqGrad;
			this.paramVec = paramVec;
			this.notify = notify;
		}
		@Override
		public Integer call() throws Exception {
			PosNegRWExample ex = in.get();
			if (notify != null) synchronized(notify) { notify.notify(); }
			long start = System.currentTimeMillis();
			if (log.isDebugEnabled()) log.debug("Training start "+this.id);
			agLearner.trainOnExample(paramVec, totSqGrad, ex);
			if (log.isDebugEnabled()) log.debug("Training done "+this.id);
			statistics.updateTrainingStatistics(System.currentTimeMillis()-start);
			
			//rosecatherinek: testing P values at the end of training
			int[] posList = ex.getPosList();
			int[] negList = ex.getNegList();
			int[] seedList = ex.getQueryVec().keys();
			double[] p = ex.p;
			
			System.out.println("P of pos examples: ");
			for(int i : posList){
				System.out.println(p[i]);
			}
			System.out.println("P of neg examples:");
			for(int i : negList){
				System.out.println(p[i]);
			}
			System.out.println("P of seeds:");
			for(int i : seedList){
				System.out.println(i + ": " + p[i]);
			}
			
			
			return ex.length();
		}
	}

	protected class Grad extends AdaGradTrain {
		ParamVector sumGradient;
		public Grad(Future<PosNegRWExample> parsed, ParamVector paramVec, ParamVector sumGradient, SimpleParamVector<String> totSqGrad, AdaGradSRW agLearner, int id, AdaGradTrainer notify) {
			super(parsed, paramVec, totSqGrad, agLearner, id, notify);
			this.sumGradient = sumGradient;
		}
		@Override
		public Integer call() throws Exception {
			PosNegRWExample ex = in.get();
			if (notify != null) synchronized(notify) { notify.notify(); }
			if (log.isDebugEnabled()) log.debug("Gradient start "+this.id);
			agLearner.accumulateGradient(paramVec, ex, sumGradient);
			if (log.isDebugEnabled()) log.debug("Gradient done "+this.id);
			return 1; 
			// ^^^^ this is the equivalent of k++ from before;
			// the total sum (query count) will be stored in numExamplesThisEpoch
			// by TraceLosses. It's a hack but it works
		}
	}


	/**
	 * Cleans up outputs from training (tracks some info for traceLosses)
	 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
	 *
	 */
	protected class TraceLosses implements Runnable {
		Future<Integer> in;
		int id;
		public TraceLosses(Future<Integer> in, int id) {
			this.in = in;
			this.id = id;
		}
		@Override
		public void run() {
			try {
				int n = this.in.get();
				if (log.isDebugEnabled()) log.debug("Cleaning start "+this.id);
				statistics.updateNumExamples(n);
				if (log.isDebugEnabled()) log.debug("Cleaning done "+this.id);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				log.error("Trouble with #"+id,e);
			}
		}
	}

	public static void main(String[] args) {
		try {
			int inputFiles = Configuration.USE_TRAIN;
			int outputFiles = Configuration.USE_PARAMS;
			int constants = Configuration.USE_EPOCHS | Configuration.USE_TRACELOSSES | Configuration.USE_FORCE | Configuration.USE_THREADS;
			int modules = Configuration.USE_TRAINER | Configuration.USE_SRW | Configuration.USE_SQUASHFUNCTION;
			ModuleConfiguration c = new ModuleConfiguration(args,inputFiles,outputFiles,constants,modules);
			log.info(c.toString());

			String groundedFile=c.queryFile.getPath();
			if (!c.queryFile.getName().endsWith(Grounder.GROUNDED_SUFFIX)) {
				throw new IllegalStateException("Run Grounder on "+c.queryFile.getName()+" first. Ground+Train in one go is not supported yet.");
			}
			log.info("Training model parameters on "+groundedFile+"...");
			long start = System.currentTimeMillis();
			
			//@rck AG
			SRWOptions srwOpt = c.srw.getOptions();
			srwOpt.eta = 1;
			srwOpt.mu = 1;
			srwOpt.squashingFunction = new Exp();
			c.squashingFunction = new Exp();
			AdaGradSRW agSRW = new AdaGradSRW(srwOpt);
			AdaGradTrainer agTrainer = new AdaGradTrainer(agSRW, c.nthreads, c.throttle);
			
			
			ParamVector params = agTrainer.train(
					new ParsedFile(groundedFile), 
					new ArrayLearningGraphBuilder(), 
					c.epochs, 
					c.traceLosses);
			System.out.println("Training time: "+(System.currentTimeMillis()-start));

			if (c.paramsFile != null) {
				log.info("Saving parameters to "+c.paramsFile+"...");
				ParamsFile.save(params,c.paramsFile, c);
			}
		} catch (Throwable t) {
			t.printStackTrace();
			System.exit(-1);
		}
	}




}
