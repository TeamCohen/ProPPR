package edu.cmu.ml.proppr;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.graph.ArrayLearningGraphBuilder;
import edu.cmu.ml.proppr.graph.LearningGraphBuilder;
import edu.cmu.ml.proppr.learn.SRW;
import edu.cmu.ml.proppr.learn.tools.GroundedExampleParser;
import edu.cmu.ml.proppr.learn.tools.LossData;
import edu.cmu.ml.proppr.learn.tools.LossData.LOSS;
import edu.cmu.ml.proppr.util.Configuration;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.ModuleConfiguration;
import edu.cmu.ml.proppr.util.FileBackedIterable;
import edu.cmu.ml.proppr.util.ParamVector;
import edu.cmu.ml.proppr.util.ParamsFile;
import edu.cmu.ml.proppr.util.ParsedFile;
import edu.cmu.ml.proppr.util.SimpleParamVector;
import edu.cmu.ml.proppr.util.multithreading.Cleanup;
import edu.cmu.ml.proppr.util.multithreading.Multithreading;
import edu.cmu.ml.proppr.util.multithreading.NamedThreadFactory;
import edu.cmu.ml.proppr.util.multithreading.Transformer;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;

public class Trainer {
	private static final Logger log = Logger.getLogger(Trainer.class);
	public static final int DEFAULT_CAPACITY = 16;
	public static final float DEFAULT_LOAD = (float) 0.75;
	protected int nthreads = 1;
	protected int throttle;

	protected SRW learner;
	protected int epoch;
	LossData lossLastEpoch;
	TrainingStatistics statistics=new TrainingStatistics();

	public Trainer(SRW learner, int nthreads, int throttle) {
		this.learner = learner;
		this.nthreads = Math.max(1, nthreads);
		this.throttle = throttle;

		learner.untrainedFeatures().add("fixedWeight");
		learner.untrainedFeatures().add("id(trueLoop)");
		learner.untrainedFeatures().add("id(trueLoopRestart)");
		learner.untrainedFeatures().add("id(restart)");
	}

	public Trainer(SRW srw) {
		this(srw, 1, Multithreading.DEFAULT_THROTTLE);
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

	private ParamVector createParamVector() {
		return new SimpleParamVector<String>(new ConcurrentHashMap<String,Double>(DEFAULT_CAPACITY,DEFAULT_LOAD,this.nthreads));
	}

	public void doExample(PosNegRWExample x, ParamVector<String,?> paramVec, boolean traceLosses) {
		this.learner.trainOnExample(paramVec, x);
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
		ParamVector paramVec = this.learner.setupParams(initialParamVec);
		if (paramVec.size() == 0)
			for (String f : this.learner.untrainedFeatures()) paramVec.put(f, this.learner.getWeightingScheme().defaultWeight());
		NamedThreadFactory parseThreads = new NamedThreadFactory("parse-");
		NamedThreadFactory trainThreads = new NamedThreadFactory("train-");
		int poolSize = Math.max(this.nthreads/2, 1);
		ExecutorService parsePool, trainPool, cleanPool; 
		// loop over epochs
		for (int i=0; i<numEpochs; i++) {
			// set up current epoch
			this.epoch++;
			this.learner.setEpoch(epoch);
			log.info("epoch "+epoch+" ...");

			// reset counters & file pointers
			this.learner.clearLoss();
			this.statistics = new TrainingStatistics();
			parseThreads.reset();
			trainThreads.reset();
			if (examples instanceof FileBackedIterable) ((FileBackedIterable) examples).wrap();

			// set up separate pools for parsing, training, and tracing losses
			parsePool = Executors.newFixedThreadPool(this.nthreads-poolSize, parseThreads);
			trainPool = Executors.newFixedThreadPool(poolSize, trainThreads);
			cleanPool = Executors.newSingleThreadExecutor();
			
			// run examples
			int id=1;
			long start = System.currentTimeMillis();
			for (String s : examples) {
				statistics.updateReadingStatistics(System.currentTimeMillis()-start);
				Future<PosNegRWExample> parsed = parsePool.submit(new Parse(s, builder, id));
				Future<Integer> trained = trainPool.submit(new Train(parsed, paramVec, learner, id));
				cleanPool.submit(new TraceLosses(trained, id));
				start = System.currentTimeMillis();
			}
			parsePool.shutdown();
			try {
				parsePool.awaitTermination(7,TimeUnit.DAYS);
				trainPool.shutdown();
				trainPool.awaitTermination(7, TimeUnit.DAYS);
				cleanPool.shutdown();
				cleanPool.awaitTermination(7, TimeUnit.DAYS);
			} catch (InterruptedException e) {
				log.error("Interrupted?",e);
			}
			
			// finish any trailing updates for this epoch
			this.learner.cleanupParams(paramVec,paramVec);

			// loss status
			if(traceLosses) {
				LossData lossThisEpoch = this.learner.cumulativeLoss();
				printLossOutput(lossThisEpoch);
				lossLastEpoch = lossThisEpoch;
			}
			statistics.checkStatistics();
		}
		return paramVec;
	}
	
	private void printLossOutput(LossData lossThisEpoch) {
		for(Map.Entry<LOSS,Double> e : lossThisEpoch.loss.entrySet()) e.setValue(e.getValue() / statistics.numExamplesThisEpoch);
		System.out.print("avg training loss " + lossThisEpoch.total()
				+ " on "+ statistics.numExamplesThisEpoch +" examples");
		System.out.print(" =log:reg " + lossThisEpoch.loss.get(LOSS.LOG));
		System.out.print(" : " + lossThisEpoch.loss.get(LOSS.REGULARIZATION));
		if (epoch>1) {
			LossData diff = lossLastEpoch.diff(lossThisEpoch);
			System.out.println(" improved by " + diff.total()
					+ " (log:reg "+diff.loss.get(LOSS.LOG) +":"+diff.loss.get(LOSS.REGULARIZATION)+")");
			if (diff.total() < 0.0) {
				System.out.println("WARNING: loss INCREASED by " + 
						(-diff.total()) + " - what's THAT about?");
			}
		} else 
			System.out.println();
	}

	public ParamVector findGradient(Iterable<String> examples, LearningGraphBuilder builder, ParamVector paramVec) {
		log.info("Computing gradient on cooked examples...");
		ParamVector sumGradient = new SimpleParamVector<String>();
		if (paramVec==null) {
			paramVec = createParamVector();
			for (String f : this.learner.untrainedFeatures()) paramVec.put(f, 1.0); // FIXME: should this use the weighter default?
		}
		paramVec = this.learner.setupParams(paramVec);
		
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
		for (String s : examples) {
			Future<PosNegRWExample> parsed = parsePool.submit(new Parse(s, builder, id));
			Future<Integer> gradfound = gradPool.submit(new Grad(parsed, paramVec, sumGradient, learner, id));
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

		this.learner.cleanupParams(paramVec, sumGradient);
		
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
	
	private class Parse implements Callable<PosNegRWExample> {
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
			PosNegRWExample ex = new GroundedExampleParser().parse(in, builder.copy());
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
	private class Train implements Callable<Integer> {
		Future<PosNegRWExample> in;
		ParamVector paramVec;
		SRW learner;
		int id;
		public Train(Future<PosNegRWExample> parsed, ParamVector paramVec, SRW learner, int id) {
			this.in = parsed;
			this.id = id;
			this.learner = learner;
			this.paramVec = paramVec;
		}
		@Override
		public Integer call() throws Exception {
			PosNegRWExample ex = in.get();
			long start = System.currentTimeMillis();
			if (log.isDebugEnabled()) log.debug("Training start "+this.id);
			learner.trainOnExample(paramVec, ex);
			if (log.isDebugEnabled()) log.debug("Training done "+this.id);
			statistics.updateTrainingStatistics(System.currentTimeMillis()-start);
			return ex.length();
		}
	}
	
	private class Grad implements Callable<Integer> {
		Future<PosNegRWExample> in;
		ParamVector paramVec;
		ParamVector sumGradient;
		SRW learner;
		int id;
		public Grad(Future<PosNegRWExample> parsed, ParamVector paramVec, ParamVector sumGradient, SRW learner, int id) {
			this.in = parsed;
			this.id = id;
			this.learner = learner;
			this.paramVec = paramVec;
			this.sumGradient = sumGradient;
		}
		@Override
		public Integer call() throws Exception {
			PosNegRWExample ex = in.get();
			if (log.isDebugEnabled()) log.debug("Gradient start "+this.id);
			learner.accumulateGradient(paramVec, ex, sumGradient);
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
	private class TraceLosses implements Runnable {
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
			int modules = Configuration.USE_TRAINER | Configuration.USE_SRW | Configuration.USE_WEIGHTINGSCHEME;
			ModuleConfiguration c = new ModuleConfiguration(args,inputFiles,outputFiles,constants,modules);
			log.info(c.toString());

			String groundedFile=c.queryFile.getPath();
			if (!c.queryFile.getName().endsWith(Grounder.GROUNDED_SUFFIX)) {
				throw new IllegalStateException("Run Grounder on "+c.queryFile.getName()+" first. Ground+Train in one go is not supported yet.");
			}
			log.info("Training model parameters on "+groundedFile+"...");
			long start = System.currentTimeMillis();
			ParamVector params = c.trainer.train(
					new ParsedFile(groundedFile), 
					new ArrayLearningGraphBuilder(), 
					c.epochs, 
					c.traceLosses);
			log.info("Finished training in "+(System.currentTimeMillis()-start)+" ms");

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
