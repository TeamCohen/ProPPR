package edu.cmu.ml.praprolog.trove;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import edu.cmu.ml.praprolog.learn.tools.LossData;
import edu.cmu.ml.praprolog.learn.tools.LossData.LOSS;
import edu.cmu.ml.praprolog.trove.graph.AnnotatedTroveGraph;
import edu.cmu.ml.praprolog.trove.graph.TroveGraph;
import edu.cmu.ml.praprolog.trove.graph.AnnotatedTroveGraph.GraphFormatException;
import edu.cmu.ml.praprolog.trove.learn.L2PosNegLossTrainedSRW;
import edu.cmu.ml.praprolog.trove.learn.SRW;
import edu.cmu.ml.praprolog.trove.learn.tools.PosNegRWExample;
import edu.cmu.ml.praprolog.util.Dictionary;
import edu.cmu.ml.praprolog.util.FileBackedIterable;
import edu.cmu.ml.praprolog.util.ParamVector;
import edu.cmu.ml.praprolog.util.ParsedFile;
import edu.cmu.ml.praprolog.util.SimpleParamVector;

public class Trainer {
	public static final String MAJOR_DELIM="\t";
	public static final String MINOR_DELIM=",";
	protected SRW<PosNegRWExample> learner;
	private int epoch;
	private static final Logger log = Logger.getLogger(Trainer.class);

	public Trainer(SRW<PosNegRWExample> learner) {
		this.learner = learner;

		learner.untrainedFeatures().add("fixedWeight");
		learner.untrainedFeatures().add("id(trueLoop)");
		learner.untrainedFeatures().add("id(trueLoopRestart)");
		learner.untrainedFeatures().add("id(defaultRestart)");
		learner.untrainedFeatures().add("id(alphaBooster)");
	}


    /** Return the batch gradient of the data
     */
    public Map<String,Double> findGradient(Iterable<PosNegRWExample> examples,ParamVector paramVec) {
		log.info("Computing gradient on cooked examples...");
		Map<String,Double> sumGradient = new TreeMap<String,Double>();
		if (paramVec==null) {
		    paramVec = new SimpleParamVector();
		    for (String f : this.learner.untrainedFeatures()) paramVec.put(f, 1.0);
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
		}

		return sumGradient;
		/*
		for (Iterator<String> it = sumGradient.keySet().iterator(); it.hasNext(); ) {
		    String feature = it.next();
		    System.out.println("** GRADIENT\t" + feature + "\t" + sumGradient.get(feature));
		}
		*/
	}


	public ParamVector trainParametersOnCookedIterator(Iterable<PosNegRWExample> iteratorFactory) {
		return this.trainParametersOnCookedIterator(iteratorFactory, false);
	}
	public ParamVector trainParametersOnCookedIterator(
			Iterable<PosNegRWExample> importCookedExamples, boolean traceLosses) {
		return trainParametersOnCookedIterator(importCookedExamples, 5, traceLosses);
	}
	public ParamVector trainParametersOnCookedIterator(
			Iterable<PosNegRWExample> importCookedExamples, int numEpochs, boolean traceLosses) {
		return trainParametersOnCookedIterator(importCookedExamples, new SimpleParamVector(new TreeMap<String,Double>()), numEpochs, traceLosses);
	}
	public ParamVector trainParametersOnCookedIterator(Iterable<PosNegRWExample> examples, 
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
			this.learner.setEpoch(this.epoch); 
			// wwc does NOT seem to help: TODO why not?
			log.info("epoch "+epoch+" ...");
			int k=0; long starttime = System.currentTimeMillis(); long lasttime = starttime;
			setUpExamples(i);
			if (examples instanceof FileBackedIterable) ((FileBackedIterable) examples).wrap();
			for (PosNegRWExample x : examples) {
				if (System.currentTimeMillis() - lasttime > 30000) {
					lasttime = System.currentTimeMillis();
					log.info(k+" examples processed");
				}
				doExample(k, x, paramVec, traceLosses);

				k++;
			}
			cleanUpExamples(i,paramVec);
			log.info(k+" examples processed");
			if(traceLosses) {
				LossData lossThisEpoch = this.learner.cumulativeLoss();
				for(Map.Entry<LOSS,Double> e : lossThisEpoch.loss.entrySet()) e.setValue(e.getValue() / numExamplesThisEpoch);
			    System.out.print("avg training loss " + lossThisEpoch.total()
			     + " on "+ numExamplesThisEpoch +" examples");
			    System.out.print(" =log:reg " + lossThisEpoch.loss.get(LOSS.LOG));
			    System.out.print(" : " + lossThisEpoch.loss.get(LOSS.REGULARIZATION));
				// wwc - added some more tracing here
//			    double avgLoss = (totalLossThisEpoch / numExamplesThisEpoch); 
//			    System.out.print("avg training loss " + avgLoss
//					     + " on "+ numExamplesThisEpoch +" examples");
//			    System.out.print(" avg pos training loss " + (totalPosLossThisEpoch/numExamplesThisEpoch));
//			    System.out.print(" avg neg training loss " + (totalNegLossThisEpoch/numExamplesThisEpoch));
//			    if (totalNegLossThisEpoch>0) {
//				System.out.print(" ratio of pos/neg training loss " + (totalPosLossThisEpoch/totalNegLossThisEpoch));
//			    }
//			    if (epoch>1) {
//				System.out.println(" improved by " + (previousAvgLoss-avgLoss));
//			    } else 
//				System.out.println();
//			    if (previousAvgLoss-avgLoss < 0.0) {
//				System.out.println("WARNING: loss INCREASED by " + 
//						   (avgLoss-previousAvgLoss) + " - what's THAT about?");
//			    }
//			    previousAvgLoss = avgLoss;
			    //if (this.testData)
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
		//		cleanUpEpochs();
		log.info("Finished in "+(System.currentTimeMillis() - start)+" ms");
		return paramVec;
	}

	////////////////////////// Template methods /////////////////////////////////////

//	protected double totalLossThisEpoch;
//	protected double totalPosLossThisEpoch;
//	protected double totalNegLossThisEpoch;
	LossData lossLastEpoch;
	protected int numExamplesThisEpoch;
	protected void doExample(int k, PosNegRWExample x, ParamVector paramVec, boolean traceLosses) {
		log.debug("example "+x.toString()+" ...");
		this.learner.trainOnExample(paramVec, x);
		if (traceLosses) {
//			totalLossThisEpoch += this.learner.empiricalLoss(paramVec, x);
//			totalPosLossThisEpoch += this.learner.empiricalLoss(paramVec, x.posOnly());
//			totalNegLossThisEpoch += this.learner.empiricalLoss(paramVec, x.negOnly());
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
}
