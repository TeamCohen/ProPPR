package edu.cmu.ml.proppr;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.graph.SimpleLearningGraph.SLGBuilder;
import edu.cmu.ml.proppr.learn.SRW;
import edu.cmu.ml.proppr.learn.tools.GroundedExampleStreamer;
import edu.cmu.ml.proppr.learn.tools.LossData;
import edu.cmu.ml.proppr.learn.tools.LossData.LOSS;
import edu.cmu.ml.proppr.util.Configuration;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.ModuleConfiguration;
import edu.cmu.ml.proppr.util.FileBackedIterable;
import edu.cmu.ml.proppr.util.ParamVector;
import edu.cmu.ml.proppr.util.ParamsFile;
import edu.cmu.ml.proppr.util.SimpleParamVector;
import edu.cmu.ml.proppr.util.multithreading.Cleanup;
import edu.cmu.ml.proppr.util.multithreading.Multithreading;
import edu.cmu.ml.proppr.util.multithreading.Transformer;

public class Trainer {
	private static final Logger log = Logger.getLogger(Trainer.class);
	public static final int DEFAULT_CAPACITY = 16;
	public static final float DEFAULT_LOAD = (float) 0.75;
	protected int nthreads = 1;
	protected int throttle;

	protected SRW<PosNegRWExample> learner;
	protected int epoch;
	LossData lossLastEpoch;

	public Trainer(SRW<PosNegRWExample> learner, int nthreads, int throttle) {
		this.learner = learner;
		this.nthreads = Math.max(1, nthreads);
		this.throttle = throttle;

		learner.untrainedFeatures().add("fixedWeight");
		learner.untrainedFeatures().add("id(trueLoop)");
		learner.untrainedFeatures().add("id(trueLoopRestart)");
		learner.untrainedFeatures().add("id(restart)");
		learner.untrainedFeatures().add("id(alphaBooster)");
	}

	public Trainer(SRW<PosNegRWExample> srw) {
		this(srw, 1, Multithreading.DEFAULT_THROTTLE);
	}

	private ParamVector createParamVector() {
		return new SimpleParamVector<String>(new ConcurrentHashMap<String,Double>(DEFAULT_CAPACITY,DEFAULT_LOAD,this.nthreads));
	}

	public void doExample(PosNegRWExample x, ParamVector<String,?> paramVec, boolean traceLosses) {
		this.learner.trainOnExample(paramVec, x);
	}

	public ParamVector train(Iterable<PosNegRWExample> examples, int numEpochs, boolean traceLosses) {
		return train(
				examples,
				createParamVector(),
				numEpochs,
				traceLosses
				);
	}

	public ParamVector train(Iterable<PosNegRWExample> examples, ParamVector initialParamVec, int numEpochs, boolean traceLosses) {
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
					new FQTrainingExampleStreamer(examples, paramVec), 
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
			this.learner.cleanupParams(paramVec);

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
								(diff.total()) + " - what's THAT about?");
					}
				} else 
					System.out.println();

				lossLastEpoch = lossThisEpoch;

			}
		}
		return paramVec;
	}

	public Map<String, Double> findGradient(
			GroundedExampleStreamer examples, ParamVector paramVec) {

		log.info("Computing gradient on cooked examples...");
		Map<String,Double> sumGradient = new TreeMap<String,Double>();
		if (paramVec==null) {
			paramVec = createParamVector();
			for (String f : this.learner.untrainedFeatures()) paramVec.put(f, 1.0); // FIXME: should this use the weighter default?
		}
		int k=0;

		//WW: accumulate example-size normalized gradient
		for (PosNegRWExample x : examples) {
			this.learner.addDefaultWeights(x.getGraph(),paramVec);
			this.learner.accumulateGradient(this.learner.gradient(paramVec, x),x.length(),sumGradient);
			k++;
		}

		//WW: renormalize by the total number of queries
		for (Iterator<String> it = sumGradient.keySet().iterator(); it.hasNext(); ) {
			String feature = it.next();
			double unnormf = sumGradient.get(feature);
			double norm = unnormf / k;
			sumGradient.put(feature, norm);
			//System.out.println("** GRADIENT\t" + feature + "\t" + sumGradient.get(feature));
		}

		return sumGradient;
	}

	/////////////////////// Multithreading scaffold ///////////////////////

	/**
	 * Stream over instances of this class
	 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
	 *
	 */
	private class FQTrainingExample {
		PosNegRWExample x;
		ParamVector paramVec;
		SRW learner;
		public FQTrainingExample(PosNegRWExample x, ParamVector paramVec, SRW learner) {
			this.x = x;
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
			log.debug("Training on example "+this.id);
			in.learner.trainOnExample(in.paramVec, in.x);
			return in.x.length();
		}
	}

	protected int numExamplesThisEpoch;
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
				log.debug("Cleaning up example "+this.id);
				numExamplesThisEpoch += this.in.get();
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
		Iterator<PosNegRWExample> examples;
		ParamVector paramVec;
		public FQTrainingExampleStreamer(Iterable<PosNegRWExample> examples, ParamVector paramVec) {
			this.examples = examples.iterator();
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
			PosNegRWExample example = examples.next();
			return new FQTrainingExample(example, paramVec, learner);
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("No removal of examples permitted during training!");
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
					new GroundedExampleStreamer(groundedFile, new SLGBuilder()), 
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
