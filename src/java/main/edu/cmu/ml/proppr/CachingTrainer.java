package edu.cmu.ml.proppr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.graph.GraphFormatException;
import edu.cmu.ml.proppr.graph.LearningGraphBuilder;
import edu.cmu.ml.proppr.learn.SRW;
import edu.cmu.ml.proppr.learn.tools.RWExampleParser;
import edu.cmu.ml.proppr.learn.tools.LossData;
import edu.cmu.ml.proppr.learn.tools.StoppingCriterion;
import edu.cmu.ml.proppr.util.SymbolTable;
import edu.cmu.ml.proppr.util.math.ParamVector;
import edu.cmu.ml.proppr.util.multithreading.NamedThreadFactory;

public class CachingTrainer extends Trainer {
	private static final Logger log = Logger.getLogger(CachingTrainer.class);
	public static final boolean DEFAULT_SHUFFLE = true;
	private boolean shuffle;

	public CachingTrainer(SRW learner, int nthreads, int throttle, boolean shuffle) {
		super(learner, nthreads, throttle);
		this.shuffle = shuffle;
	}

	@Override
	public ParamVector train(SymbolTable<String> masterFeatures, Iterable<String> exampleFile, LearningGraphBuilder builder, ParamVector initialParamVec, int numEpochs, boolean traceLosses) {
		ArrayList<PosNegRWExample> examples = new ArrayList<PosNegRWExample>();
		RWExampleParser parser = new RWExampleParser();
		if (masterFeatures.size()>0) LearningGraphBuilder.setFeatures(masterFeatures);
		int id=0;
		long start = System.currentTimeMillis();
		TrainingStatistics total = new TrainingStatistics();
		for (String s : exampleFile) {
			total.updateReadingStatistics(System.currentTimeMillis()-start);
			id++;
			try {
				long before = System.currentTimeMillis();
				PosNegRWExample ex = parser.parse(s, builder, masterLearner);
				total.updateParsingStatistics(System.currentTimeMillis()-before);
				examples.add(ex);
			} catch (GraphFormatException e) {
				log.error("Trouble with #"+id,e);
			}
			start = System.currentTimeMillis();
		}
		return trainCached(examples,builder,initialParamVec,numEpochs,traceLosses,total);
	}
	
	public ParamVector trainCached(List<PosNegRWExample> examples, LearningGraphBuilder builder, ParamVector initialParamVec, int numEpochs, boolean traceLosses, TrainingStatistics total) {
		ParamVector paramVec = this.masterLearner.setupParams(initialParamVec);
		if (paramVec.size() == 0)
			for (String f : this.masterLearner.untrainedFeatures()) paramVec.put(f, this.masterLearner.getSquashingFunction().defaultValue());
		NamedThreadFactory trainThreads = new NamedThreadFactory("work-");
		ExecutorService trainPool;
		ExecutorService cleanPool; 
		StoppingCriterion stopper = new StoppingCriterion(numEpochs, this.stoppingPercent, this.stoppingEpoch);
		boolean graphSizesStatusLog = true;
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
			trainThreads.reset();

			trainPool = Executors.newFixedThreadPool(this.nthreads, trainThreads);
			cleanPool = Executors.newSingleThreadExecutor();

			// run examples
			int id=1;
			if (this.shuffle) Collections.shuffle(examples);
			for (PosNegRWExample s : examples) {
				Future<ExampleStats> trained = trainPool.submit(new Train(new PretendParse(s), paramVec, id, null));
				cleanPool.submit(new TraceLosses(trained, id));
				id++;
			}

			cleanEpoch(trainPool, cleanPool, paramVec, traceLosses, stopper, id, total);
			if(graphSizesStatusLog) {
				log.info("Dataset size stats: "+statistics.totalGraphSize+" total nodes / max "+statistics.maxGraphSize+" / avg "+(statistics.totalGraphSize / id));
				graphSizesStatusLog = false;
			}
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
