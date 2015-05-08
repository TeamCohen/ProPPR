package edu.cmu.ml.proppr;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.graph.LearningGraphBuilder;
import edu.cmu.ml.proppr.graph.LearningGraph.GraphFormatException;
import edu.cmu.ml.proppr.learn.SRW;
import edu.cmu.ml.proppr.learn.tools.GroundedExampleParser;
import edu.cmu.ml.proppr.learn.tools.LossData;
import edu.cmu.ml.proppr.util.ParamVector;
import edu.cmu.ml.proppr.util.multithreading.NamedThreadFactory;

public class CachingTrainer extends Trainer {
	private static final Logger log = Logger.getLogger(CachingTrainer.class);

	public CachingTrainer(SRW learner, int nthreads, int throttle) {
		super(learner, nthreads, throttle);
	}

	@Override
	public ParamVector train(Iterable<String> exampleFile, LearningGraphBuilder builder, ParamVector initialParamVec, int numEpochs, boolean traceLosses) {
		ArrayList<PosNegRWExample> examples = new ArrayList<PosNegRWExample>();
		GroundedExampleParser parser = new GroundedExampleParser();
		TrainingStatistics total = new TrainingStatistics();
		int id=0;
		long start = System.currentTimeMillis();
		for (String s : exampleFile) {
			total.updateReadingStatistics(System.currentTimeMillis()-start);
			id++;
			try {
				long before = System.currentTimeMillis();
				PosNegRWExample ex = parser.parse(s, builder.copy());
				total.updateParsingStatistics(System.currentTimeMillis()-before);
				examples.add(ex);
			} catch (GraphFormatException e) {
				log.error("Trouble with #"+id,e);
			}
			start = System.currentTimeMillis();
		}
		return trainCached(examples,builder,initialParamVec,numEpochs,traceLosses,total);
	}
	
	public ParamVector trainCached(Iterable<PosNegRWExample> examples, LearningGraphBuilder builder, ParamVector initialParamVec, int numEpochs, boolean traceLosses, TrainingStatistics total) {
		ParamVector paramVec = this.learner.setupParams(initialParamVec);
		if (paramVec.size() == 0)
			for (String f : this.learner.untrainedFeatures()) paramVec.put(f, this.learner.getWeightingScheme().defaultWeight());
		NamedThreadFactory trainThreads = new NamedThreadFactory("train-");
		ExecutorService trainPool;
		ExecutorService cleanPool; 
		// loop over epochs
		for (int i=0; i<numEpochs; i++) {
			// set up current epoch
			this.epoch++;
			this.learner.setEpoch(epoch);
			log.info("epoch "+epoch+" ...");

			// reset counters & file pointers
			this.learner.clearLoss();
			this.statistics = new TrainingStatistics();
			trainThreads.reset();

			trainPool = Executors.newFixedThreadPool(this.nthreads, trainThreads);
			cleanPool = Executors.newSingleThreadExecutor();

			// run examples
			int id=1;
			for (PosNegRWExample s : examples) {
				Future<Integer> trained = trainPool.submit(new Train(new PretendParse(s), paramVec, learner, id));
				cleanPool.submit(new TraceLosses(trained, id));
			}
			try {
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

			total.updateTrainingStatistics(statistics.trainTime);
		}
		
		log.info("Reading: "+total.readTime+" Parsing: "+total.parseTime+" Training: "+total.trainTime);
		return paramVec;
	}

	private class PretendParse implements Future<PosNegRWExample> {
		PosNegRWExample e;
		public PretendParse(PosNegRWExample e) {
			this.e=e;
		}
		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			return false;
		}

		@Override
		public boolean isCancelled() {
			return false;
		}

		@Override
		public boolean isDone() {
			return true;
		}

		@Override
		public PosNegRWExample get() throws InterruptedException,
		ExecutionException {
			return this.e;
		}

		@Override
		public PosNegRWExample get(long timeout, TimeUnit unit)
				throws InterruptedException, ExecutionException,
				TimeoutException {
			return this.e;
		}

	}
}
