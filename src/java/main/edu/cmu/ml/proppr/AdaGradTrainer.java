package edu.cmu.ml.proppr;

import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.Trainer.ExampleStats;
import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.graph.LearningGraphBuilder;
import edu.cmu.ml.proppr.learn.AdaGradSRW;
import edu.cmu.ml.proppr.learn.SRW;
import edu.cmu.ml.proppr.learn.tools.LossData;
import edu.cmu.ml.proppr.learn.tools.StoppingCriterion;
import edu.cmu.ml.proppr.util.SymbolTable;
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
public class AdaGradTrainer extends Trainer {
	private static final Logger log = Logger.getLogger(AdaGradTrainer.class);

	public AdaGradTrainer(SRW srw, int nthreads, int throttle) {
		super(srw, nthreads, throttle);
		if (!(srw instanceof AdaGradSRW))
			throw new IllegalArgumentException("AdaGradTrainer requires matching AdaGradSRW; received "+srw.getClass().getName()+" instead");
	}

	public AdaGradTrainer(SRW agSRW) {
		this(agSRW, 1, Multithreading.DEFAULT_THROTTLE);
	}

	@Override
	public ParamVector<String,?> train(SymbolTable<String> masterFeatures, Iterable<String> examples, LearningGraphBuilder builder, ParamVector<String,?> initialParamVec, int numEpochs, boolean traceLosses) {
		ParamVector<String,?> paramVec = this.masterLearner.setupParams(initialParamVec);

		//@rck AG
		//create a concurrent hash map to store the running total of the squares of the gradient
		SimpleParamVector<String> totSqGrad = new SimpleParamVector<String>(new ConcurrentHashMap<String,Double>(DEFAULT_CAPACITY,DEFAULT_LOAD,this.nthreads)); 

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
			int countdown=-1; AdaGradTrainer notify = null;
			for (String s : examples) {
				if (log.isDebugEnabled()) log.debug("Queue size "+(workingPool.getTaskCount()-workingPool.getCompletedTaskCount()));
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
							while(workingPool.getTaskCount()-workingPool.getCompletedTaskCount() > this.nthreads)
								this.wait();
							if (log.isDebugEnabled()) log.debug("Queue cleared.");
						}
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else if (workingPool.getTaskCount()-workingPool.getCompletedTaskCount() > 1.5*this.nthreads) {
					if (log.isDebugEnabled()) log.debug("Starting countdown");
					countdown=this.nthreads;
					notify = this;
				}
				Future<PosNegRWExample> parsed = workingPool.submit(new Parse(s, builder, id));
				Future<ExampleStats> trained = workingPool.submit(new AdaGradTrain(parsed, paramVec, totSqGrad, id, notify));
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

	public ParamVector<String,?> findGradient(SymbolTable<String> masterFeatures, Iterable<String> examples, LearningGraphBuilder builder, ParamVector paramVec, SimpleParamVector<String> totSqGrad) {
		log.info("Computing gradient on cooked examples...");
		ParamVector<String,?> sumGradient = new SimpleParamVector<String>();
		if (paramVec==null) {
			paramVec = createParamVector();
		}
		paramVec = this.masterLearner.setupParams(paramVec);
		if (masterFeatures != null && masterFeatures.size()>0) LearningGraphBuilder.setFeatures(masterFeatures);

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

		//@rck AG
		//create a concurrent hash map to store the running total of the squares of the gradient
//		SimpleParamVector<String> totSqGrad = new SimpleParamVector<String>(new ConcurrentHashMap<String,Double>(DEFAULT_CAPACITY,DEFAULT_LOAD,this.nthreads)); 

		
		// run examples
		int id=1;
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
			Future<ExampleStats> gradfound = gradPool.submit(new Grad(parsed, paramVec, sumGradient, totSqGrad, id, notify));
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

	/////////////////////// Multithreading scaffold ///////////////////////

	/**
	 * Transforms from inputs to outputs
	 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
	 *
	 */
	protected class AdaGradTrain implements Callable<ExampleStats> {
		Future<PosNegRWExample> in;
		ParamVector<String,?> paramVec;
		SimpleParamVector<String> totSqGrad;
		int id;
		AdaGradTrainer notify;
		public AdaGradTrain(Future<PosNegRWExample> parsed, ParamVector<String,?> paramVec, 
				SimpleParamVector<String> totSqGrad, int id, AdaGradTrainer notify) {
			this.in = parsed;
			this.id = id;
			this.totSqGrad = totSqGrad;
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
			((AdaGradSRW)learner).trainOnExample(paramVec, totSqGrad, ex);
			statistics.updateTrainingStatistics(System.currentTimeMillis()-start);
			if (log.isDebugEnabled()) log.debug("Training done "+this.id);

			if (log.isDebugEnabled()){
				//rosecatherinek: testing P values at the end of training
				int[] posList = ex.getPosList();
				int[] negList = ex.getNegList();
				int[] seedList = ex.getQueryVec().keys();
				double[] p = ex.p;

				log.debug("P of pos examples: ");
				for(int i : posList){
					log.debug(p[i]);
				}
				log.debug("P of neg examples:");
				for(int i : negList){
					log.debug(p[i]);
				}
				log.debug("P of seeds:");
				for(int i : seedList){
					log.debug(i + ": " + p[i]);
				}
			}

			return new ExampleStats(ex.length(),ex.getGraph().nodeSize());
		}
	}

	protected class Grad extends AdaGradTrain {
		ParamVector<String,?> sumGradient;
		public Grad(Future<PosNegRWExample> parsed, ParamVector<String,?> paramVec, ParamVector<String,?> sumGradient, 
				SimpleParamVector<String> totSqGrad, int id, AdaGradTrainer notify) {
			super(parsed, paramVec, totSqGrad, id, notify);
			this.sumGradient = sumGradient;
		}
		@Override
		public ExampleStats call() throws Exception {
			PosNegRWExample ex = in.get();
			SRW learner = learners.get(Thread.currentThread().getName());
			if (notify != null) synchronized(notify) { notify.notify(); }
			if (log.isDebugEnabled()) log.debug("Gradient start "+this.id);
			((AdaGradSRW)learner).accumulateGradient(paramVec, ex, sumGradient);
			if (log.isDebugEnabled()) log.debug("Gradient done "+this.id);
			return new ExampleStats(1,-1); 
			// ^^^^ this is the equivalent of k++ from before;
			// the total sum (query count) will be stored in numExamplesThisEpoch
			// by TraceLosses. It's a hack but it works
		}
	}


}
