package edu.cmu.ml.praprolog;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import edu.cmu.ml.praprolog.Tester.TestResults;
import edu.cmu.ml.praprolog.graph.AnnotatedGraph;
import edu.cmu.ml.praprolog.graph.AnnotatedGraph.GraphFormatException;
import edu.cmu.ml.praprolog.graph.AnnotatedGraphFactory;
import edu.cmu.ml.praprolog.learn.L2PosNegLossTrainedSRW;
import edu.cmu.ml.praprolog.learn.PosNegRWExample;
import edu.cmu.ml.praprolog.learn.SRW;
import edu.cmu.ml.praprolog.util.Configuration;
import edu.cmu.ml.praprolog.util.Dictionary;
import edu.cmu.ml.praprolog.util.ExperimentConfiguration;
import edu.cmu.ml.praprolog.util.ParsedFile;

public class Trainer<T> {
	public static final String MAJOR_DELIM="\t";
	public static final String MINOR_DELIM=",";
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
	public Collection<PosNegRWExample<T>> importCookedExamples(String cookedExamplesFile, AnnotatedGraphFactory<T> factory) {
		log.info("Importing cooked examples from "+cookedExamplesFile);
		Collection<PosNegRWExample<T>> result = null;
		result = importCookedExamples(new ParsedFile(cookedExamplesFile), factory);
		log.info("Imported "+result.size()+" examples");
		return result;
	}
	public Collection<PosNegRWExample<T>> importCookedExamples(ParsedFile reader, AnnotatedGraphFactory<T> factory) {
		ArrayList<PosNegRWExample<T>> result = null;
		result = new ArrayList<PosNegRWExample<T>>();
		for(String line : reader){
			AnnotatedGraph<T> g = factory.create();

			String[] parts = line.trim().split(MAJOR_DELIM,5);

			TreeMap<T, Double> queryVec = new TreeMap<T,Double>();
			for(String u : parts[1].split(MINOR_DELIM)) queryVec.put(g.keyToId(u), 1.0);

			String[] rawPosList, rawNegList;
			if (parts[2].length()>0) rawPosList = parts[2].split(MINOR_DELIM);
			else rawPosList = new String[0];
			if (parts[3].length()>0) rawNegList = parts[3].split(MINOR_DELIM);
			else rawNegList = new String[0];
			if (rawPosList.length + rawNegList.length == 0) {
				log.warn("no labeled solutions for example on line "+reader.getAbsoluteLineNumber()+"; skipping");
				continue;
			}
			T[] posList = g.keyToId(rawPosList);
			T[] negList = g.keyToId(rawNegList);
			try {
				g = AnnotatedGraph.fromStringParts(parts[4],g);
				result.add(new PosNegRWExample<T>(g,queryVec,posList,negList));
			} catch (GraphFormatException e) {
				reader.parseError("["+e.getMessage()+"]");
			}

		}
		reader.close();
		return result;
	}

	public Map<String,Double> trainParametersOnCookedIterator(Collection<PosNegRWExample<T>> iteratorFactory) {
		return this.trainParametersOnCookedIterator(iteratorFactory, false);
	}
	public Map<String, Double> trainParametersOnCookedIterator(
			Collection<PosNegRWExample<T>> importCookedExamples, boolean traceLosses) {
		return trainParametersOnCookedIterator(importCookedExamples, 5, traceLosses);
	}
	public Map<String, Double> trainParametersOnCookedIterator(
			Collection<PosNegRWExample<T>> importCookedExamples, int numEpochs, boolean traceLosses) {
		return trainParametersOnCookedIterator(importCookedExamples, new TreeMap<String,Double>(), numEpochs, traceLosses);
	}
	public Map<String,Double> trainParametersOnCookedIterator(Collection<PosNegRWExample<T>> examples, 
			Map<String, Double> initialParamVec, 
			int numEpochs, 
			boolean traceLosses) {
		log.info("Training on cooked examples...");
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
			setUpExamples(i,examples);
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
			log.info(k+" examples processed");
			if(traceLosses) {
				System.out.println("training loss "+(totalLossThisEpoch / numExamplesThisEpoch) + " on "+ numExamplesThisEpoch +" examples");
				//if (this.testData)
			}
			if (log.isDebugEnabled()) {
				log.debug("After epoch "+epoch+": "+Dictionary.buildString(paramVec, new StringBuilder(), "\n\t").toString());
			}
		}
		//		cleanUpEpochs();
		return paramVec;
	}

	////////////////////////// Template methods /////////////////////////////////////

	protected double totalLossThisEpoch;
	protected int numExamplesThisEpoch;
	protected void doExample(int k, PosNegRWExample<T> x, Map<String,Double> paramVec, boolean traceLosses) {
		log.debug("example "+x.toString()+" ...");
		this.learner.trainOnExample(paramVec, x);
		if (traceLosses) {
			totalLossThisEpoch += this.learner.empiricalLoss(paramVec, x);
			numExamplesThisEpoch += x.length();
		}
	}

	protected void setUpExamples(int epoch, Collection<PosNegRWExample<T>> examples) {
		totalLossThisEpoch = 0;
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
					trainer.importCookedExamples(cookedFile), 
					c.epochs, 
					c.traceLosses);
		} else {
			Trainer<String> trainer = (Trainer<String>) c.trainer;
			paramVec = trainer.trainParametersOnCookedIterator(
					trainer.importCookedExamples(cookedFile, new AnnotatedGraphFactory<String>(AnnotatedGraphFactory.STRING)),
					c.epochs,
					c.traceLosses);
		}
		log.info("Finished training in "+(System.currentTimeMillis()-start)+" ms");

		if (c.paramsFile != null) {
			log.info("Saving parameters to "+c.paramsFile+"...");
			Dictionary.save(paramVec, c.paramsFile);
		}
	}
}
