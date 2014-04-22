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
import edu.cmu.ml.praprolog.learn.CookedExampleStreamer;
import edu.cmu.ml.praprolog.learn.L2PosNegLossTrainedSRW;
import edu.cmu.ml.praprolog.learn.PosNegRWExample;
import edu.cmu.ml.praprolog.learn.SRW;
import edu.cmu.ml.praprolog.util.Configuration;
import edu.cmu.ml.praprolog.util.Dictionary;
import edu.cmu.ml.praprolog.util.ExperimentConfiguration;
import edu.cmu.ml.praprolog.util.FileBackedIterable;
import edu.cmu.ml.praprolog.util.ParamsFile;
import edu.cmu.ml.praprolog.util.ParsedFile;

public class Trainer<T> {
	protected SRW<PosNegRWExample<T>> learner;
	private int epoch;
	private static final Logger log = Logger.getLogger(Trainer.class);

	public Trainer(SRW<PosNegRWExample<T>> learner) {
		this.learner = learner;

		learner.untrainedFeatures().add("fixedWeight");
		learner.untrainedFeatures().add("id(trueLoop)");
		learner.untrainedFeatures().add("id(trueLoopRestart)");
		learner.untrainedFeatures().add("id(defaultRestart)");
		learner.untrainedFeatures().add("id(alphaBooster)"); // ?
	}

	/**
	 * Generate the cooked examples stored in this file.
	 * @param cookedExamplesFile
	 * @return
	 */
//	public Iterable<PosNegRWExample<T>> importCookedExamples(String cookedExamplesFile, AnnotatedGraphFactory<T> factory) {
//		log.info("Importing cooked examples from "+cookedExamplesFile);
//		Iterable<PosNegRWExample<T>> result = null;
//		result = importCookedExamples(new ParsedFile(cookedExamplesFile), factory);
////		log.info("Imported "+result.size()+" examples");
//		return result;
//	}
//	public Iterable<PosNegRWExample<T>> importCookedExamples(ParsedFile file, AnnotatedGraphFactory<T> factory) {
//		ArrayList<PosNegRWExample<T>> result = null;
//		result = new ArrayList<PosNegRWExample<T>>();
//		for(String line : file){
//			
//			log.debug("Imporing example from line "+file.getLineNumber());
//		
//			AnnotatedGraph<T> g = factory.create();
//
//			String[] parts = line.trim().split(MAJOR_DELIM,5);
//
//			TreeMap<T, Double> queryVec = new TreeMap<T,Double>();
//			for(String u : parts[1].split(MINOR_DELIM)) queryVec.put(g.keyToId(u), 1.0);
//
//			String[] rawPosList, rawNegList;
//			if (parts[2].length()>0) rawPosList = parts[2].split(MINOR_DELIM);
//			else rawPosList = new String[0];
//			if (parts[3].length()>0) rawNegList = parts[3].split(MINOR_DELIM);
//			else rawNegList = new String[0];
//			if (rawPosList.length + rawNegList.length == 0) {
//				log.warn("no labeled solutions for example on line "+file.getAbsoluteLineNumber()+"; skipping");
//				continue;
//			}
//			T[] posList = g.keyToId(rawPosList);
//			T[] negList = g.keyToId(rawNegList);
//			try {
//				g = AnnotatedGraph.fromStringParts(parts[4],g);
//				result.add(new PosNegRWExample<T>(g,queryVec,posList,negList));
//			} catch (GraphFormatException e) {
//				file.parseError("["+e.getMessage()+"]");
//			}
//
//		}
//		file.close();
//		return result;
//	}
	
    /** Return the batch gradient of the data
     */
    public Map<String,Double> findGradient(Collection<PosNegRWExample<T>> examples,Map<String,Double> paramVec) {
		log.info("Computing gradient on cooked examples...");
		Map<String,Double> sumGradient = new TreeMap<String,Double>();
		if (paramVec==null) {
		    paramVec = new TreeMap<String,Double>();
		    for (String f : this.learner.untrainedFeatures()) paramVec.put(f, 1.0);
		}
		int k=0;
		for (PosNegRWExample<T> x : examples) {
		    SRW.addDefaultWeights(x.getGraph(),paramVec);
		    this.learner.accumulateGradient(this.learner.gradient(paramVec, x),sumGradient);
		    k++;
		}
		return sumGradient;
		/*
		for (Iterator<String> it = sumGradient.keySet().iterator(); it.hasNext(); ) {
		    String feature = it.next();
		    System.out.println("** GRADIENT\t" + feature + "\t" + sumGradient.get(feature));
		}
		*/
	}

	public Map<String,Double> trainParametersOnCookedIterator(Iterable<PosNegRWExample<T>> examples) {
		return this.trainParametersOnCookedIterator(examples, false);
	}
	public Map<String, Double> trainParametersOnCookedIterator(
			Iterable<PosNegRWExample<T>> examples, boolean traceLosses) {
		return trainParametersOnCookedIterator(examples, 5, traceLosses);
	}
	public Map<String, Double> trainParametersOnCookedIterator(
			Iterable<PosNegRWExample<T>> examples, int numEpochs, boolean traceLosses) {
		return trainParametersOnCookedIterator(examples, new TreeMap<String,Double>(), numEpochs, traceLosses);
	}
	public Map<String,Double> trainParametersOnCookedIterator(Iterable<PosNegRWExample<T>> examples, 
			Map<String, Double> initialParamVec, 
			int numEpochs, 
			boolean traceLosses) {
		log.info("Training on cooked examples...");
		double previousAvgLoss = Double.MAX_VALUE;
		long start = System.currentTimeMillis();
		this.epoch = 0;
		Map<String,Double> paramVec = initialParamVec;
		if (paramVec.size() == 0) {
			for (String f : this.learner.untrainedFeatures()) paramVec.put(f, 1.0);
		}
		setUpEpochs(paramVec);
		for (int i=0; i<numEpochs; i++) {
			this.epoch++;
			log.info("epoch "+epoch+" ...");
			int k=0; long starttime = System.currentTimeMillis(); long lasttime = starttime;
			setUpExamples(i);
			if (examples instanceof FileBackedIterable) ((FileBackedIterable) examples).wrap();
			for (PosNegRWExample<T> x : examples) {
				if (System.currentTimeMillis() - lasttime > 30000) {
					lasttime = System.currentTimeMillis();
					log.info(k+" examples processed");
				}
				//				SRW.addDefaultWeights(x.getGraph(), paramVec);
				doExample(k, x, paramVec, traceLosses);

				k++;
			}
			cleanUpExamples(i);
//			log.info(k+" examples processed");
			if(traceLosses) {
				// wwc - added some more tracing here
			    double avgLoss = (totalLossThisEpoch / numExamplesThisEpoch); 
			    System.out.print("avg training loss " + avgLoss
					     + " on "+ numExamplesThisEpoch +" examples");
			    System.out.print(" avg pos training loss " + (totalPosLossThisEpoch/numExamplesThisEpoch));
			    System.out.print(" avg neg training loss " + (totalNegLossThisEpoch/numExamplesThisEpoch));
			    if (totalNegLossThisEpoch>0) {
				System.out.print(" ratio of pos/neg training loss " + (totalPosLossThisEpoch/totalNegLossThisEpoch));
			    }
			    if (epoch>1) {
				System.out.println(" improved by " + (previousAvgLoss-avgLoss));
			    } else 
				System.out.println();
			    if (previousAvgLoss-avgLoss < 0.0) {
				System.out.println("WARNING: loss INCREASED by " + 
						   (avgLoss-previousAvgLoss) + " - what's THAT about?");
			    }
			    previousAvgLoss = avgLoss;
			    //if (this.testData)
				
			}
		}
		//		cleanUpEpochs();
		log.info("Finished in "+(System.currentTimeMillis() - start)+" ms");
		
		return paramVec;
	}

	////////////////////////// Template methods /////////////////////////////////////

	protected double totalLossThisEpoch;
	protected double totalPosLossThisEpoch;
	protected double totalNegLossThisEpoch;
	protected int numExamplesThisEpoch;
	protected void doExample(int k, PosNegRWExample<T> x, Map<String,Double> paramVec, boolean traceLosses) {
		log.debug("example "+x.toString()+" ...");
		this.learner.trainOnExample(paramVec, x);
		if (traceLosses) {
			totalLossThisEpoch += this.learner.empiricalLoss(paramVec, x);
			totalPosLossThisEpoch += this.learner.empiricalLoss(paramVec, x.posOnly());
			totalNegLossThisEpoch += this.learner.empiricalLoss(paramVec, x.negOnly());
			numExamplesThisEpoch += x.length();
		}
	}

	protected void setUpExamples(int epoch) {
		totalLossThisEpoch = 0;
		totalPosLossThisEpoch = 0;
		totalNegLossThisEpoch = 0;
		numExamplesThisEpoch = 0;
	}
	protected void cleanUpExamples(int epoch) {}

	protected void setUpEpochs(Map<String,Double> paramVec) {}
	//	protected void cleanUpEpochs() {}

	////////////////////////// Run ////////////////////////////////////////

	public static void main(String[] args) {
		int flags = Configuration.USE_DEFAULTS | Configuration.USE_TRAIN | Configuration.USE_PARAMS;
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
					new edu.cmu.ml.praprolog.trove.learn.CookedExampleStreamer(cookedFile), 
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
