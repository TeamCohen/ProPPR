package edu.cmu.ml.praprolog;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import edu.cmu.ml.praprolog.graph.AnnotatedGraph;
import edu.cmu.ml.praprolog.graph.AnnotatedGraph.GraphFormatException;
import edu.cmu.ml.praprolog.graph.AnnotatedGraphFactory;
import edu.cmu.ml.praprolog.learn.L2PosNegLossTrainedSRW;
import edu.cmu.ml.praprolog.learn.PosNegRWExample;
import edu.cmu.ml.praprolog.learn.SRW;
import edu.cmu.ml.praprolog.util.Dictionary;

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
		try {
			result = importCookedExamples(new LineNumberReader(new FileReader(cookedExamplesFile)), factory);
			log.info("Imported "+result.size()+" examples");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}
	public Collection<PosNegRWExample<T>> importCookedExamples(BufferedReader reader, AnnotatedGraphFactory<T> factory) {
		ArrayList<PosNegRWExample<T>> result = null;
		try {
			result = new ArrayList<PosNegRWExample<T>>();
			int linenum=0;
			for(String line; (line=reader.readLine()) != null; linenum++){
				AnnotatedGraph<T> g = factory.create();
				
				String[] parts = line.trim().split(MAJOR_DELIM,5);
				
				TreeMap<T, Double> queryVec = new TreeMap<T,Double>();
				for(String u : parts[1].split(MINOR_DELIM)) queryVec.put(g.keyToId(u), 1.0);
				
				String[] rawPosList = parts[2].split(MINOR_DELIM);
				String[] rawNegList = parts[3].split(MINOR_DELIM);
				T[] posList = g.keyToId(rawPosList);
				T[] negList = g.keyToId(rawNegList);
				try {
					g = AnnotatedGraph.fromStringParts(parts[4],g);
					result.add(new PosNegRWExample<T>(g,queryVec,posList,negList));
				} catch (GraphFormatException e) {
					throw new IllegalArgumentException("["+e.getMessage()+"] at line "+linenum);
				}
					
			}
			reader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
	
	private static final String USAGE = "SINGLETHREADED TRAINING:\nUsage:\n\tcookedExampleFile outputParamFile [options]\n"
			+"\t\t--epochs {int}\tNumber of epochs (default 5)\n"
			+"\t\t--traceLosses\tTurn on traceLosses (default off)\n"
			+"\t\t             \tNB: example count for losses is sum(x.length() for x in examples)\n"
			+"\t\t             \tand won't match `wc -l cookedExampleFile`\n";
	private static void usage() {
		System.err.println(USAGE);
		System.exit(0);
	}
	public static void main(String[] args) {
		if (args.length < 2) {
			usage();
		}
		
		String cookedExampleFile = args[0];
		String outputParamFile   = args[1];
		int epochs = 5;
		boolean traceLosses = false;
		if (args.length > 2) {
			for (int i=2; i<args.length; i++) {
				if ("--epochs".equals(args[i])) {
					if (i+1<args.length) epochs = Integer.parseInt(args[++i]);
					else usage();
				} else if ("--traceLosses".equals(args[i])) {
					traceLosses = true;
				} else usage();
			}
		}
		
		L2PosNegLossTrainedSRW<String> srw = new L2PosNegLossTrainedSRW<String>();
		Trainer<String> trainer = new Trainer<String>(srw);
		Map<String,Double> paramVec = trainer.trainParametersOnCookedIterator(
				trainer.importCookedExamples(cookedExampleFile, new AnnotatedGraphFactory<String>(AnnotatedGraphFactory.STRING)),
				epochs,
				traceLosses);
		Dictionary.save(paramVec, outputParamFile);
	}
}
