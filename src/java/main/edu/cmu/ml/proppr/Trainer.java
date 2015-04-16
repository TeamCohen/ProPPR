package edu.cmu.ml.proppr;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

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

	public Trainer(SRW learner, int nthreads, int throttle) {
		this.learner = learner;
		this.nthreads = Math.max(1, nthreads);
		this.throttle = throttle;

		learner.untrainedFeatures().add("fixedWeight");
		learner.untrainedFeatures().add("id(trueLoop)");
		learner.untrainedFeatures().add("id(trueLoopRestart)");
		learner.untrainedFeatures().add("id(restart)");
//		learner.untrainedFeatures().add("id(alphaBooster)");
	}

	public Trainer(SRW srw) {
		this(srw, 1, Multithreading.DEFAULT_THROTTLE);
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
		Multithreading<FQTrainingExample,Integer> m = new Multithreading<FQTrainingExample,Integer>(log);
		// loop over epochs
		for (int i=0; i<numEpochs; i++) {
			// set up current epoch
			this.epoch++;
			this.learner.setEpoch(epoch);
			log.info("epoch "+epoch+" ...");

			this.learner.clearLoss();
			numExamplesThisEpoch = 0;

			if (examples instanceof FileBackedIterable) ((FileBackedIterable) examples).wrap();

			// run examples through Multithreading
			m.executeJob(
					this.nthreads, 
					new FQTrainingExampleStreamer(examples, builder, paramVec), 
					new Transformer<FQTrainingExample,Integer>() {
						@Override
						public Callable<Integer> transformer(
								FQTrainingExample in, int id) {
							return new Train(in,id);
						}
					}, 
					new Cleanup<Integer>() {
						@Override
						public Runnable cleanup(Future<Integer> in, int id) {
							return new TraceLosses(in,id);
						}
					}, 
					this.throttle);

			//
			this.learner.cleanupParams(paramVec,paramVec);

			if(traceLosses) {
				LossData lossThisEpoch = this.learner.cumulativeLoss();
				for(Map.Entry<LOSS,Double> e : lossThisEpoch.loss.entrySet()) e.setValue(e.getValue() / numExamplesThisEpoch);
				System.out.print("avg training loss " + lossThisEpoch.total()
						+ " on "+ numExamplesThisEpoch +" examples");
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

				lossLastEpoch = lossThisEpoch;

			}
		}
		return paramVec;
	}

	public ParamVector findGradient(Iterable<String> examples, LearningGraphBuilder builder, ParamVector paramVec) {

		log.info("Computing gradient on cooked examples...");
		final ParamVector sumGradient = new SimpleParamVector<String>();
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

		// instead let's run the examples through Multithreading
		final GroundedExampleParser parser = new GroundedExampleParser();
		Multithreading<FQTrainingExample,Integer> m = new Multithreading<FQTrainingExample,Integer>(log);
		m.executeJob(
				this.nthreads, 
				new FQTrainingExampleStreamer(examples, builder, paramVec), 
				new Transformer<FQTrainingExample,Integer>() {
					@Override
					public Callable<Integer> transformer(
							final FQTrainingExample in, final int id) {
						return new Callable<Integer>() {
							@Override
							public Integer call() throws Exception {
								PosNegRWExample x = parser.parse(in.g, in.builderFactory.getBuilder(Thread.currentThread()));
								in.learner.accumulateGradient(in.paramVec, x, sumGradient);
								
								return 1; 
								// ^^^^ this is the equivalent of k++ from before;
								// the total sum (query count) will be stored in numExamplesThisEpoch
								// by TraceLosses. It's a hack but it works
							}};
					}
				}, 
				new Cleanup<Integer>() {
					@Override
					public Runnable cleanup(Future<Integer> in, int id) {
						return new TraceLosses(in,id);
					}
				}, 
				this.throttle);

		this.learner.cleanupParams(paramVec, sumGradient);
		
		//WW: renormalize by the total number of queries
		for (Iterator<String> it = sumGradient.keySet().iterator(); it.hasNext(); ) {
			String feature = it.next();
			double unnormf = sumGradient.get(feature);
			// query count stored in numExamplesThisEpoch, as noted above
			double norm = unnormf / this.numExamplesThisEpoch.intValue();
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

	/**
	 * Stream over instances of this class
	 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
	 *
	 */
	private class FQTrainingExample {
		String g;
		ParamVector paramVec;
		SRW learner;
		FQTrainingExampleStreamer builderFactory;
		public FQTrainingExample(String g, FQTrainingExampleStreamer builderFactory, ParamVector paramVec, SRW learner) {
			this.g = g;
			this.builderFactory = builderFactory;
			this.paramVec = paramVec;
			this.learner = learner;
		}
	}

	/**
	 * Transforms from inputs to outputs
	 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
	 *
	 */
	private class Train implements Callable<Integer> {
		FQTrainingExample in;
		int id;
		public Train(FQTrainingExample in, int id) {
			this.in = in;
			this.id = id;
		}
		@Override
		public Integer call() throws Exception {
			if (log.isDebugEnabled()) log.debug("Parsing start "+this.id);
			PosNegRWExample ex = new GroundedExampleParser().parse(in.g, in.builderFactory.getBuilder(Thread.currentThread()));
			if (log.isDebugEnabled()) log.debug("Parsing done "+this.id);
			if (log.isDebugEnabled()) log.debug("Training start "+this.id);
			in.learner.trainOnExample(in.paramVec, ex);
			if (log.isDebugEnabled()) log.debug("Training done "+this.id);
			return in.g.length();
		}
	}

	protected Integer numExamplesThisEpoch = 0;
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
				if (log.isDebugEnabled()) log.debug("Cleaning start "+this.id);
				int n = this.in.get();
				synchronized(numExamplesThisEpoch) {
					numExamplesThisEpoch += n;
				}
				if (log.isDebugEnabled()) log.debug("Cleaning done "+this.id);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				log.error("Trouble with #"+id,e);
			}
		}
	}
	/**
	 * Builds the streamer of all training inputs from the streamer of training examples. 
	 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
	 *
	 */
	private class FQTrainingExampleStreamer implements Iterable<FQTrainingExample>,Iterator<FQTrainingExample> {
		Iterator<String> examples;
		ParamVector paramVec;
		HashMap<String,LearningGraphBuilder> builderSource = new HashMap<String,LearningGraphBuilder>();
		LearningGraphBuilder builder;
		public FQTrainingExampleStreamer(Iterable<String> examples, LearningGraphBuilder builder, ParamVector paramVec) {
			this.examples = examples.iterator();
			this.builder = builder;
			this.paramVec = paramVec;
		}
		@Override
		public Iterator<FQTrainingExample> iterator() {
			return this;
		}

		@Override
		public boolean hasNext() {
			return examples.hasNext();
		}

		@Override
		public FQTrainingExample next() {
			String example = examples.next();
			return new FQTrainingExample(example, this, paramVec, learner);
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("No removal of examples permitted during training!");
		}
		
		public LearningGraphBuilder getBuilder(Thread t) {
			if (!builderSource.containsKey(t.getName())) builderSource.put(t.getName(),builder.copy());
			return builderSource.get(t.getName());
		}
	}

	public static void main(String[] args) {
		try {
			int inputFiles = Configuration.USE_TRAIN;
			int outputFiles = Configuration.USE_PARAMS;
			int constants = Configuration.USE_EPOCHS | Configuration.USE_TRACELOSSES | Configuration.USE_FORCE | Configuration.USE_THREADS;
			int modules = Configuration.USE_TRAINER | Configuration.USE_SRW | Configuration.USE_WEIGHTINGSCHEME;
			//		int flags = Configuration.USE_DEFAULTS 
			//				| Configuration.USE_TRAIN 
			//				| Configuration.USE_SRW 
			//				| Configuration.USE_LEARNINGSET 
			//				| Configuration.USE_PARAMS 
			//				| Configuration.USE_DEFERREDPROGRAM
			//				| Configuration.USE_MAXT;
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
