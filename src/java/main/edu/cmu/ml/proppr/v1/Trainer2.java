package edu.cmu.ml.proppr.v1;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.learn.SRW;
import edu.cmu.ml.proppr.learn.tools.LossData;
import edu.cmu.ml.proppr.learn.tools.LossData.LOSS;
import edu.cmu.ml.proppr.util.FileBackedIterable;
import edu.cmu.ml.proppr.util.ParamVector;
import edu.cmu.ml.proppr.util.SimpleParamVector;
import edu.cmu.ml.proppr.util.multithreading.Cleanup;
import edu.cmu.ml.proppr.util.multithreading.Multithreading;
import edu.cmu.ml.proppr.util.multithreading.Transformer;

/**
 * Example multithreaded trainer class showing a use case for the Multithreading 
 * utility/scaffolding.
 * 
 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
 */
public class Trainer2<T> extends Trainer<T> {
	private static final Logger log = Logger.getLogger(Trainer2.class);
	public static final int DEFAULT_CAPACITY = 16;
	public static final float DEFAULT_LOAD = (float) 0.75;
	protected int nthreads = 1;
	protected int throttle;

	public Trainer2(SRW<PosNegRWExample<T>> learner, int nthreads) {
		this(learner,nthreads,Multithreading.DEFAULT_THROTTLE);
	}
	public Trainer2(SRW<PosNegRWExample<T>> learner, int nthreads, int throttle) {
		super(learner);
		this.nthreads = nthreads;
		this.throttle = throttle;
	}

	/**
	 * Overridden to set the default ParamVector.
	 */
	@Override
	public ParamVector trainParametersOnCookedIterator(
			Iterable<PosNegRWExample<T>> importCookedExamples, int numEpochs, boolean traceLosses) {
		return trainParametersOnCookedIterator(
				importCookedExamples, 
				new SimpleParamVector(new ConcurrentHashMap<String,Double>(DEFAULT_CAPACITY,DEFAULT_LOAD,this.nthreads)), 
				numEpochs, 
				traceLosses);
	}
	/**
	 * Overridden to use the Multithreading scaffold.
	 */
	@Override
	public ParamVector trainParametersOnCookedIterator(Iterable<PosNegRWExample<T>> examples, 
			ParamVector initialParamVec, 
			int numEpochs, 
			boolean traceLosses) {
		ParamVector paramVec = this.learner.setupParams(initialParamVec);
		if (paramVec.size() == 0) {
			for (String f : this.learner.untrainedFeatures()) paramVec.put(f, this.learner.getWeightingScheme().defaultWeight());
		}

		Multithreading<FQTrainingExample,Integer> m = new Multithreading<FQTrainingExample,Integer>(log);
		// loop over epochs
		for (int i=0; i<numEpochs; i++) {
			// set up current epoch
			this.epoch++;
			this.learner.setEpoch(epoch);
			log.info("epoch "+epoch+" ...");
			setUpExamples(i);
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
			cleanUpExamples(i, paramVec);
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
	
	/**
	 * Stream over instances of this class
	 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
	 *
	 */
	private class FQTrainingExample {
		PosNegRWExample<T> x;
		ParamVector paramVec;
		SRW learner;
		public FQTrainingExample(PosNegRWExample<T> x, ParamVector paramVec, SRW learner) {
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
		Iterator<PosNegRWExample<T>> examples;
		ParamVector paramVec;
		public FQTrainingExampleStreamer(Iterable<PosNegRWExample<T>> examples, ParamVector paramVec) {
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
			PosNegRWExample<T> example = examples.next();
			return new FQTrainingExample(example, paramVec, learner);
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("No removal of examples permitted during training!");
		}
		
	}
	

	public void setThreads(int nthreads) {
		this.nthreads = nthreads;
	}
	public void setThrottle(int throttle) {
		this.throttle = throttle;
	}
}
