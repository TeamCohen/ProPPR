package edu.cmu.ml.proppr;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
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
import edu.cmu.ml.proppr.learn.SRW;
import edu.cmu.ml.proppr.learn.SRW.ZeroGradientData;
import edu.cmu.ml.proppr.learn.tools.LossData;
import edu.cmu.ml.proppr.learn.tools.LossData.LOSS;
import edu.cmu.ml.proppr.learn.tools.RWExampleParser;
import edu.cmu.ml.proppr.learn.tools.StoppingCriterion;
import edu.cmu.ml.proppr.util.Configuration;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.ModuleConfiguration;
import edu.cmu.ml.proppr.util.ParamsFile;
import edu.cmu.ml.proppr.util.ParsedFile;
import edu.cmu.ml.proppr.util.SimpleSymbolTable;
import edu.cmu.ml.proppr.util.SymbolTable;
import edu.cmu.ml.proppr.util.math.ParamVector;
import edu.cmu.ml.proppr.util.math.SimpleParamVector;
import edu.cmu.ml.proppr.util.multithreading.Multithreading;
import edu.cmu.ml.proppr.util.multithreading.NamedThreadFactory;

public class Trainer {
	private static final double MAX_PCT_ZERO_GRADIENT = 0.2;
	private static final Logger log = Logger.getLogger(Trainer.class);
	public static final int DEFAULT_CAPACITY = 16;
	public static final float DEFAULT_LOAD = (float) 0.75;
	protected int nthreads = 1;
	protected int throttle;

	protected Map<String,SRW> learners;
	protected SRW masterLearner;
	protected int epoch;
	protected LossData lossLastEpoch;
	protected TrainingStatistics statistics=new TrainingStatistics();
	
	protected int stoppingEpoch = 3;
	protected double stoppingPercent = 1.0;


	public Trainer(SRW learner, int nthreads, int throttle) {
		this.masterLearner = learner;
		this.nthreads = Math.max(1, nthreads);
		this.throttle = throttle;

		learner.untrainedFeatures().add("id(trueLoop)");
		learner.untrainedFeatures().add("id(trueLoopRestart)");
		learner.untrainedFeatures().add("id(restart)");

		this.learners = new HashMap<String,SRW>();
		for (int i=0;i<this.nthreads;i++) {
			this.learners.put("work-"+(i+1), learner.copy());
		}
	}

	public Trainer(SRW srw) {
		this(srw, 1, Multithreading.DEFAULT_THROTTLE);
	}

	public class TrainingStatistics {
		int numExamplesThisEpoch = 0;
		int exampleSetSize = 0;
				long minReadTime = Integer.MAX_VALUE;
				long maxReadTime = 0;
		long readTime = 0;
				long minParseTime = Integer.MAX_VALUE;
				long maxParseTime = 0;
		long parseTime = 0;
				long minTrainTime = Integer.MAX_VALUE;
				long maxTrainTime = 0;
		long trainTime = 0;
		int maxGraphSize = 0;
		int totalGraphSize = 0;
		void updateReadingStatistics(long time) {
						minReadTime = Math.min(time, minReadTime);
						maxReadTime = Math.max(time, maxReadTime);
			readTime += time;
		}
		void updateParsingStatistics(long time) {
						minParseTime = Math.min(time, minParseTime);
						maxParseTime = Math.max(time, maxParseTime);
			parseTime += time;
		}
		void updateTrainingStatistics(long time) {
						minTrainTime = Math.min(time, minTrainTime);
						maxTrainTime = Math.max(time, maxTrainTime);
			trainTime += time;
			exampleSetSize++;
		}
		void updateSummaryStatistics(TrainingStatistics stats) {
			readTime += stats.readTime;
			minReadTime = Math.min(stats.minReadTime, minReadTime);
			maxReadTime = Math.max(stats.maxReadTime, maxReadTime);
			parseTime += stats.parseTime;
			minParseTime = Math.min(stats.minParseTime, minParseTime);
			maxParseTime = Math.max(stats.maxParseTime, maxParseTime);
			trainTime += stats.trainTime;
			minTrainTime = Math.min(stats.minTrainTime, minTrainTime);
			maxTrainTime = Math.max(stats.maxTrainTime, maxTrainTime);
			
		}
		synchronized void updateExampleStats(ExampleStats n) {	
			numExamplesThisEpoch+=n.length;
			maxGraphSize = Math.max(n.nodes,maxGraphSize);
			totalGraphSize += n.nodes;
		}
		void checkStatistics() {
			int poolSize = nthreads;
			readTime = Math.max(1, readTime);
			parseTime = Math.max(1, parseTime);
			trainTime = Math.max(1, trainTime);
			// we can keep the working pool full if we can read $poolSize examples
			// in the time it takes to parse + train 1 example
			int workFull = (int) Math.ceil( (parseTime+trainTime) / readTime);
			if (poolSize - workFull > 1) log.warn((poolSize-workFull)+" working threads went unused; reading from disk is slow. :(");
		}
	}

	protected ParamVector<String,?> createParamVector() {
		return new SimpleParamVector<String>(new ConcurrentHashMap<String,Double>(DEFAULT_CAPACITY,DEFAULT_LOAD,this.nthreads));
	}

	public void doExample(PosNegRWExample x, ParamVector<String,?> paramVec, boolean traceLosses) {
		this.learners.get(Thread.currentThread().getName()).trainOnExample(paramVec, x);
	}

	public ParamVector<String,?> train(SymbolTable<String> masterFeatures, Iterable<String> examples, LearningGraphBuilder builder, File initialParamVecFile, int numEpochs, boolean traceLosses) {
		ParamVector<String,?> initParams = null;
		if (initialParamVecFile != null) {
			log.info("loading initial params from "+initialParamVecFile);
			initParams = new SimpleParamVector<String>(Dictionary.load(new ParsedFile(initialParamVecFile), new ConcurrentHashMap<String,Double>()));
		} else {
			initParams = createParamVector();
		}
		return train(
				masterFeatures,
				examples,
				builder,
				initParams,
				numEpochs,
				traceLosses
				);
	}

	public ParamVector<String,?> train(SymbolTable<String> masterFeatures, Iterable<String> examples, LearningGraphBuilder builder, ParamVector<String,?> initialParamVec, int numEpochs, boolean traceLosses) {
		ParamVector<String,?> paramVec = this.masterLearner.setupParams(initialParamVec);
		if (paramVec.size() == 0)
			for (String f : this.masterLearner.untrainedFeatures()) paramVec.put(f, this.masterLearner.getSquashingFunction().defaultValue());
		if (masterFeatures.size()>0) LearningGraphBuilder.setFeatures(masterFeatures);
		NamedThreadFactory workingThreads = new NamedThreadFactory("work-");
		NamedThreadFactory cleaningThreads = new NamedThreadFactory("cleanup-");
		ThreadPoolExecutor workingPool;
		ExecutorService cleanPool; 
		TrainingStatistics total = new TrainingStatistics();
		StoppingCriterion stopper = new StoppingCriterion(numEpochs, this.stoppingPercent, this.stoppingEpoch);
		boolean graphSizesStatusLog=true;
		// repeat until ready to stop
		while (!stopper.satisified()) {
			// set up current epoch
			this.epoch++;
			for (SRW learner : this.learners.values()) {
				learner.setEpoch(epoch);
				learner.clearLoss();
			}
			log.info("epoch "+epoch+" ...");

			// reset counters & file pointers
			this.statistics = new TrainingStatistics();
			workingThreads.reset();
			cleaningThreads.reset();

			workingPool = new ThreadPoolExecutor(this.nthreads,Integer.MAX_VALUE,10,TimeUnit.SECONDS,new LinkedBlockingQueue<Runnable>(),workingThreads);
			cleanPool = Executors.newSingleThreadExecutor(cleaningThreads);

			// run examples
			int id=1;
			long start = System.currentTimeMillis();
			int countdown=-1; Trainer notify = null;
			for (String s : examples) {
				if (log.isDebugEnabled()) log.debug("Queue size "+(workingPool.getTaskCount()-workingPool.getCompletedTaskCount()));
				statistics.updateReadingStatistics(System.currentTimeMillis()-start);
				/*
				 * Throttling behavior:
				 * Once the number of unfinished tasks exceeds 1.5x the number of threads,
				 * we add a 'notify' object to the next nthreads training tasks. Then, the
				 * master thread gathers 'notify' signals until the number of unfinished tasks 
				 * is no longer greater than the number of threads. Then we start adding tasks again.
				 * 
				 * This works more or less fine, since the master thread stops pulling examples
				 * from disk when there are then a maximum of 2.5x training examples in the queue (that's
				 * the original 1.5x, which could represent a maximum of 1.5x training examples,
				 * plus the nthreads training tasks with active 'notify' objects. There's an 
				 * additional nthreads parsing tasks in the queue but those don't take up much 
				 * memory so we don't care). This lets us read in a good-sized buffer without
				 * blowing up the heap.
				 * 
				 * Worst-case: None of the backlog is cleared before the master thread enters
				 * the synchronized block. nthreads-1 threads will be training long jobs, and 
				 * the one free thread works through the 0.5x backlog and all nthreads countdown 
				 * examples. The notify() sent by the final countdown example will occur when 
				 * there are nthreads unfinished tasks in the queue, and the master thread will exit
				 * the synchronized block and proceed.
				 * 
				 * Best-case: The backlog is already cleared by the time the master thread enters
				 * the synchronized block. The while() loop immediately exits, and the notify()
				 * signals from the countdown examples have no effect.
				 */
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
							while(workingPool.getTaskCount()-workingPool.getCompletedTaskCount() > this.nthreads)
								this.wait();
							if (log.isDebugEnabled()) log.debug("Queue cleared.");
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				} else if (workingPool.getTaskCount()-workingPool.getCompletedTaskCount() > 1.5*this.nthreads) {
					if (log.isDebugEnabled()) log.debug("Starting countdown");
					countdown=this.nthreads;
					notify = this;
				}
				Future<PosNegRWExample> parsed = workingPool.submit(new Parse(s, builder, id));
				Future<ExampleStats> trained = workingPool.submit(new Train(parsed, paramVec, id, notify));
				cleanPool.submit(new TraceLosses(trained, id));
				id++;
				start = System.currentTimeMillis();
			}

			cleanEpoch(workingPool, cleanPool, paramVec, traceLosses, stopper, id, total);
			if(graphSizesStatusLog) {
				log.info("Dataset size stats: "+statistics.totalGraphSize+" total nodes / max "+statistics.maxGraphSize+" / avg "+(statistics.totalGraphSize / id));
				graphSizesStatusLog = false;
			}
		}
		log.info("Reading  statistics: min "+total.minReadTime+" / max "+total.maxReadTime+" / total "+total.readTime);
		log.info("Parsing  statistics: min "+total.minParseTime+" / max "+total.maxParseTime+" / total "+total.parseTime);
		log.info("Training statistics: min "+total.minTrainTime+" / max "+total.maxTrainTime+" / total "+total.trainTime);
		return paramVec;
	}
	
	/**
	 * End-of-epoch cleanup routine shared by Trainer, CachingTrainer. 
	 * Shuts down working thread, cleaning thread, regularizer, loss calculations, stopper calculations, 
	 * training statistics, and zero gradient statistics.
	 * @param workingPool
	 * @param cleanPool
	 * @param paramVec
	 * @param traceLosses
	 * @param stopper
	 * @param n - number of examples
	 * @param stats
	 */
	protected void cleanEpoch(ExecutorService workingPool, ExecutorService cleanPool,
			ParamVector<String,?> paramVec, boolean traceLosses, StoppingCriterion stopper, int n, TrainingStatistics stats) {
		workingPool.shutdown();
		try {
			workingPool.awaitTermination(7, TimeUnit.DAYS);
			cleanPool.shutdown();
			cleanPool.awaitTermination(7, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		// finish any trailing updates for this epoch
		// finish any trailing updates for this epoch
		this.masterLearner.cleanupParams(paramVec,paramVec);

			// loss status and signalling the stopper
			if(traceLosses) {

				LossData lossThisEpoch = new LossData();
				for (SRW learner : this.learners.values()) {
					lossThisEpoch.add(learner.cumulativeLoss());
				}				lossThisEpoch.convertCumulativesToAverage(statistics.numExamplesThisEpoch);
				printLossOutput(lossThisEpoch);
				if (epoch>1) {
					stopper.recordConsecutiveLosses(lossThisEpoch,lossLastEpoch);
				}
				lossLastEpoch = lossThisEpoch;
			}

		ZeroGradientData zeros = this.masterLearner.new ZeroGradientData();
		for (SRW learner : this.learners.values()) {
			zeros.add(learner.getZeroGradientData());
		}
		if (zeros.numZero > 0) {
			log.info(zeros.numZero + " / "+n+" examples with 0 gradient");
			if (zeros.numZero / (float) n > MAX_PCT_ZERO_GRADIENT) 
				log.warn("Having this many 0 gradients is unusual. Try a different squashing function?");
		}
		stopper.recordEpoch();
		statistics.checkStatistics();
		stats.updateReadingStatistics(statistics.readTime);
		stats.updateParsingStatistics(statistics.parseTime);
		stats.updateTrainingStatistics(statistics.trainTime);
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

	public ParamVector<String,?> findGradient(Iterable<String> examples, LearningGraphBuilder builder, ParamVector<String,?> paramVec) {
		log.info("Computing gradient on cooked examples...");
		ParamVector<String,?> sumGradient = new SimpleParamVector<String>();
		if (paramVec==null) {
			paramVec = createParamVector();
			for (String f : this.masterLearner.untrainedFeatures()) paramVec.put(f, 1.0); // FIXME: should this use the weighter default?
		}
		paramVec = this.masterLearner.setupParams(paramVec);

		//		
		//		//WW: accumulate example-size normalized gradient
		//		for (PosNegRWExample x : examples) {
		////			this.learner.initializeFeatures(paramVec,x.getGraph());
		//			this.learner.accumulateGradient(paramVec, x, sumGradient);
		//			k++;
		//		}

		NamedThreadFactory workThreads = new NamedThreadFactory("work-");
		ExecutorService workPool, cleanPool; 

		workPool = Executors.newFixedThreadPool(this.nthreads, workThreads);
		cleanPool = Executors.newSingleThreadExecutor();

		// run examples
		int id=1;
		int countdown=-1; Trainer notify = null;
		for (String s : examples) {
			long queueSize = (((ThreadPoolExecutor) workPool).getTaskCount()-((ThreadPoolExecutor) workPool).getCompletedTaskCount());
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
						while((((ThreadPoolExecutor) workPool).getTaskCount()-((ThreadPoolExecutor) workPool).getCompletedTaskCount()) > this.nthreads)
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
			Future<PosNegRWExample> parsed = workPool.submit(new Parse(s, builder, id));
			Future<ExampleStats> gradfound = workPool.submit(new Grad(parsed, paramVec, sumGradient, id, notify));
			cleanPool.submit(new TraceLosses(gradfound, id));
		}
		workPool.shutdown();
		try {
			workPool.awaitTermination(7,TimeUnit.DAYS);
			cleanPool.shutdown();
			cleanPool.awaitTermination(7, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			log.error("Interrupted?",e);
		}

		this.masterLearner.cleanupParams(paramVec, sumGradient);

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

	public ParamVector<String,?> findGradient(ArrayList<PosNegRWExample> examples,
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
			SRW learner = learners.get(Thread.currentThread().getName());
			if (log.isDebugEnabled()) log.debug("Parsing start "+this.id);
			long start = System.currentTimeMillis();
			PosNegRWExample ex = new RWExampleParser().parse(in, builder.copy(), learner);
			statistics.updateParsingStatistics(System.currentTimeMillis()-start);
			if (log.isDebugEnabled()) log.debug("Parsing done "+this.id);
			return ex;
		}

	}

	/**
	 * Transforms from inputs to outputs
	 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
	 *
	 */
	protected class Train implements Callable<ExampleStats> {
		Future<PosNegRWExample> in;
		ParamVector<String,?> paramVec;
		int id;
		Trainer notify;
		public Train(Future<PosNegRWExample> parsed, ParamVector<String,?> paramVec, int id, Trainer notify) {
			this.in = parsed;
			this.id = id;
			this.paramVec = paramVec;
			this.notify = notify;
		}
		@Override
		public ExampleStats call() throws Exception {
			PosNegRWExample ex = in.get();
			SRW learner = learners.get(Thread.currentThread().getName());
			if (notify != null) synchronized(notify) { notify.notify(); }
			if (log.isDebugEnabled()) log.debug("Training start "+this.id);
			long start = System.currentTimeMillis();
			learner.trainOnExample(paramVec, ex);
			statistics.updateTrainingStatistics(System.currentTimeMillis()-start);
			if (log.isDebugEnabled()) log.debug("Training done "+this.id);
			return new ExampleStats(ex.length(),ex.getGraph().nodeSize());
		}
	}

	protected class Grad extends Train {
		ParamVector<String,?> sumGradient;
		public Grad(Future<PosNegRWExample> parsed, ParamVector<String,?> paramVec, ParamVector<String,?> sumGradient, int id, Trainer notify) {
			super(parsed, paramVec, id, notify);
			this.sumGradient = sumGradient;
		}
		@Override
		public ExampleStats call() throws Exception {
			PosNegRWExample ex = in.get();
			SRW learner = learners.get(Thread.currentThread().getName());
			if (notify != null) synchronized(notify) { notify.notify(); }
			if (log.isDebugEnabled()) log.debug("Gradient start "+this.id);
			learner.accumulateGradient(paramVec, ex, sumGradient);
			if (log.isDebugEnabled()) log.debug("Gradient done "+this.id);
			return new ExampleStats(1,-1); 
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
		Future<ExampleStats> in;
		int id;
		public TraceLosses(Future<ExampleStats> in, int id) {
			this.in = in;
			this.id = id;
		}
		@Override
		public void run() {
			try {
				ExampleStats n = this.in.get();
				if (log.isDebugEnabled()) log.debug("Cleaning start "+this.id);
				statistics.updateExampleStats(n);
				if (log.isDebugEnabled()) log.debug("Cleaning done "+this.id);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				log.error("Trouble with #"+id,e);
			}
		}
	}
	
	protected class ExampleStats {
		public int length;
		public int nodes;
		public ExampleStats(int el, int n) {
			this.length = el;
			this.nodes = n;
		}
	}

	public static void main(String[] args) {
		try {
			int inputFiles = Configuration.USE_TRAIN | Configuration.USE_INIT_PARAMS;
			int outputFiles = Configuration.USE_PARAMS;
			int constants = Configuration.USE_EPOCHS | Configuration.USE_TRACELOSSES | Configuration.USE_FORCE | Configuration.USE_THREADS;
			int modules = Configuration.USE_TRAINER | Configuration.USE_SRW | Configuration.USE_SQUASHFUNCTION;
			ModuleConfiguration c = new ModuleConfiguration(args,inputFiles,outputFiles,constants,modules);
			log.info(c.toString());

			String groundedFile=c.queryFile.getPath();
			if (!c.queryFile.getName().endsWith(Grounder.GROUNDED_SUFFIX)) {
				throw new IllegalStateException("Run Grounder on "+c.queryFile.getName()+" first. Ground+Train in one go is not supported yet.");
			}
			SymbolTable<String> masterFeatures = new SimpleSymbolTable<String>();
			File featureIndex = new File(groundedFile+Grounder.FEATURE_INDEX_EXTENSION);
			if (featureIndex.exists()) {
				log.info("Reading feature index from "+featureIndex.getName()+"...");
				for (String line : new ParsedFile(featureIndex)) {
					masterFeatures.insert(line.trim());
				}
			}
			log.info("Training model parameters on "+groundedFile+"...");
			long start = System.currentTimeMillis();
			ParamVector<String,?> params = c.trainer.train(
					masterFeatures,
					new ParsedFile(groundedFile), 
					new ArrayLearningGraphBuilder(), 
					c.initParamsFile,
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

	public void setStoppingCriteria(int stoppingEpochs, double percent) {
		this.stoppingEpoch = stoppingEpochs;
		this.stoppingPercent = percent;

	}




}
