package edu.cmu.ml.praprolog;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import edu.cmu.ml.praprolog.Tester.TestResults;
import edu.cmu.ml.praprolog.graph.AnnotatedGraph;
import edu.cmu.ml.praprolog.graph.AnnotatedGraph.GraphFormatException;
import edu.cmu.ml.praprolog.graph.AnnotatedGraphFactory;
import edu.cmu.ml.praprolog.learn.L2PosNegLossTrainedSRW;
import edu.cmu.ml.praprolog.learn.SRW;
import edu.cmu.ml.praprolog.learn.tools.CookedExampleStreamer;
import edu.cmu.ml.praprolog.learn.tools.LossData;
import edu.cmu.ml.praprolog.learn.tools.PosNegRWExample;
import edu.cmu.ml.praprolog.learn.tools.LossData.LOSS;
import edu.cmu.ml.praprolog.util.Configuration;
import edu.cmu.ml.praprolog.util.Dictionary;
import edu.cmu.ml.praprolog.util.ExperimentConfiguration;
import edu.cmu.ml.praprolog.util.FileBackedIterable;
import edu.cmu.ml.praprolog.util.ParamsFile;
import edu.cmu.ml.praprolog.util.ParsedFile;
import edu.cmu.ml.praprolog.util.ParamVector;
import edu.cmu.ml.praprolog.util.SimpleParamVector;

public class Trainer<T> {
	protected SRW<PosNegRWExample<T>> learner;
	protected int epoch;
	private static final Logger log = Logger.getLogger(Trainer.class);

	public Trainer(SRW<PosNegRWExample<T>> learner) {
		this.learner = learner;

		learner.untrainedFeatures().add("fixedWeight");
		learner.untrainedFeatures().add("id(trueLoop)");
		learner.untrainedFeatures().add("id(trueLoopRestart)");
		learner.untrainedFeatures().add("id(defaultRestart)");
		learner.untrainedFeatures().add("id(alphaBooster)"); // ?
	}

	
    /** Return the batch gradient of the data
     */
    public Map<String,Double> findGradient(Collection<PosNegRWExample<T>> examples,ParamVector paramVec) {
		log.info("Computing gradient on cooked examples...");
		Map<String,Double> sumGradient = new TreeMap<String,Double>();
		if (paramVec==null) {
		    paramVec = new SimpleParamVector();
		    for (String f : this.learner.untrainedFeatures()) paramVec.put(f, 1.0);
		}
		int k=0;

              //WW: accumulate example-size normalized gradient
		for (PosNegRWExample<T> x : examples) {
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

	public ParamVector trainParametersOnCookedIterator(Iterable<PosNegRWExample<T>> examples) {
		return this.trainParametersOnCookedIterator(examples, false);
	}
	public ParamVector trainParametersOnCookedIterator(
			Iterable<PosNegRWExample<T>> examples, boolean traceLosses) {
		return trainParametersOnCookedIterator(examples, 5, traceLosses);
	}
	public ParamVector trainParametersOnCookedIterator(
			Iterable<PosNegRWExample<T>> examples, int numEpochs, boolean traceLosses) {
		return trainParametersOnCookedIterator(examples, new SimpleParamVector(), numEpochs, traceLosses);
	}
	public ParamVector trainParametersOnCookedIterator(Iterable<PosNegRWExample<T>> examples, 
			ParamVector initialParamVec, 
			int numEpochs, 
			boolean traceLosses) {
		log.info("Training on cooked examples...");
		long start = System.currentTimeMillis();
		this.epoch = 0;
		ParamVector paramVec = this.learner.setupParams(initialParamVec);
		if (paramVec.size() == 0) {
			for (String f : this.learner.untrainedFeatures()) paramVec.put(f, 1.0);
		}
		setUpEpochs(paramVec);
		for (int i=0; i<numEpochs; i++) {
			this.epoch++;
			this.learner.setEpoch(epoch);
			log.info("epoch "+epoch+" ...");
			int k=0; long starttime = System.currentTimeMillis(); long lasttime = starttime;
			setUpExamples(i);
			if (examples instanceof FileBackedIterable) ((FileBackedIterable) examples).wrap();
			for (PosNegRWExample<T> x : examples) {
				if (System.currentTimeMillis() - lasttime > 30000) {
					lasttime = System.currentTimeMillis();
					log.info(k+" examples processed");
				}
				doExample(k, x, paramVec, traceLosses);

				k++;
			}
			cleanUpExamples(i, paramVec);
			log.info(k+" examples processed");
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
		log.info("Finished in "+(System.currentTimeMillis() - start)+" ms");
		
		return paramVec;
	}

	////////////////////////// Template methods /////////////////////////////////////

//	protected double totalLossThisEpoch;
//	protected double totalPosLossThisEpoch;
//	protected double totalNegLossThisEpoch;
	LossData lossLastEpoch;
	protected int numExamplesThisEpoch;
	protected void doExample(int k, PosNegRWExample<T> x, ParamVector paramVec, boolean traceLosses) {
		log.debug("example "+x.toString()+" ...");
		this.learner.trainOnExample(paramVec, x);
		if (traceLosses) {
////			totalLossThisEpoch += this.learner.empiricalLoss(paramVec, x);
////			totalPosLossThisEpoch += this.learner.empiricalLoss(paramVec, x.posOnly());
////			totalNegLossThisEpoch += this.learner.empiricalLoss(paramVec, x.negOnly());
			numExamplesThisEpoch += x.length();
		}
	}

	protected void setUpExamples(int epoch) {
		this.learner.clearLoss();
//		totalLossThisEpoch = 0;
//		totalPosLossThisEpoch = 0;
//		totalNegLossThisEpoch = 0;
		numExamplesThisEpoch = 0;
	}
	protected void cleanUpExamples(int epoch, ParamVector paramVec) {
		this.learner.cleanupParams(paramVec);
	}

	protected void setUpEpochs(ParamVector paramVec) {}

	////////////////////////// Run ////////////////////////////////////////

	public static void main(String[] args) {
		int flags = Configuration.USE_DEFAULTS | Configuration.USE_TRAIN | Configuration.USE_SRW | Configuration.USE_LEARNINGSET | Configuration.USE_PARAMS | Configuration.USE_DEFERREDPROGRAM;
		log.info(String.format("flags: 0x%x",flags));
		ExperimentConfiguration c = new ExperimentConfiguration(args,flags);

		String cookedFile=c.dataFile.getPath();
		if (!c.dataFile.getName().endsWith(ExampleCooker.COOKED_SUFFIX)) {
			// then we have to cook first
			log.info("Cooking "+c.dataFile+"...");
			if (c.outputFile == null) 
				throw new IllegalArgumentException("If you specify an uncooked file for --data, "
						+"you have to use --output to tell me where to put the cooked version. "
						+"Use "+ExampleCooker.COOKED_SUFFIX+" for cooked files so I can tell the difference.");
			long start = System.currentTimeMillis();
			c.cooker.cookExamples(c.dataFile, c.outputFile);
			log.info("Finished cooking in "+(System.currentTimeMillis()-start)+" ms");
			cookedFile = c.outputFile;
		}

		// train parameters on the cooked training data
		log.info("Training model parameters on "+cookedFile+"...");
		long start = System.currentTimeMillis();
		Map<String,Double> paramVec = null;
		if (c.trove) {
			edu.cmu.ml.praprolog.trove.Trainer trainer = (edu.cmu.ml.praprolog.trove.Trainer) c.trainer;
			paramVec = trainer.trainParametersOnCookedIterator(
					new edu.cmu.ml.praprolog.trove.learn.tools.CookedExampleStreamer(cookedFile), 
					c.epochs, 
					c.traceLosses);
		} else {
			Trainer<String> trainer = (Trainer<String>) c.trainer;
			paramVec = trainer.trainParametersOnCookedIterator(
					new CookedExampleStreamer<String>(cookedFile, new AnnotatedGraphFactory<String>(AnnotatedGraphFactory.STRING)),
					c.epochs,
					c.traceLosses);
		}
		log.info("Finished training in "+(System.currentTimeMillis()-start)+" ms");

		if (c.paramsFile != null) {
			log.info("Saving parameters to "+c.paramsFile+"...");
			ParamsFile.save(paramVec,new File(c.paramsFile), c);
//			Dictionary.save(paramVec, c.paramsFile);
		}
	}


}
