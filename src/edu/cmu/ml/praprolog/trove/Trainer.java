package edu.cmu.ml.praprolog.trove;

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

import edu.cmu.ml.praprolog.trove.graph.AnnotatedTroveGraph;
import edu.cmu.ml.praprolog.trove.graph.TroveGraph;
import edu.cmu.ml.praprolog.trove.graph.AnnotatedTroveGraph.GraphFormatException;
import edu.cmu.ml.praprolog.trove.learn.L2PosNegLossTrainedSRW;
import edu.cmu.ml.praprolog.trove.learn.PosNegRWExample;
import edu.cmu.ml.praprolog.trove.learn.SRW;
import edu.cmu.ml.praprolog.util.Dictionary;

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
	
	/**
	 * Generate the cooked examples stored in this file.
	 * @param cookedExamplesFile
	 * @return
	 */
	public Collection<PosNegRWExample> importCookedExamples(String cookedExamplesFile) {
		log.info("Importing cooked examples from "+cookedExamplesFile);
		Collection<PosNegRWExample> result = null;
		try {
			result = importCookedExamples(new LineNumberReader(new FileReader(cookedExamplesFile)));
			log.info("Imported "+result.size()+" examples");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}
	public Collection<PosNegRWExample> importCookedExamples(BufferedReader reader) {
		ArrayList<PosNegRWExample> result = null;
		try {
			result = new ArrayList<PosNegRWExample>();
			int linenum=0;
			for(String line; (line=reader.readLine()) != null; linenum++){
				log.debug("Imporing example from line "+linenum);
				AnnotatedTroveGraph g = new AnnotatedTroveGraph();
				
				String[] parts = line.trim().split(MAJOR_DELIM,5);
				
				TreeMap<String, Double> queryVec = new TreeMap<String,Double>();
				for(String u : parts[1].split(MINOR_DELIM)) queryVec.put(u, 1.0);
				
				String[] rawPosList = parts[2].split(MINOR_DELIM);
				String[] rawNegList = parts[3].split(MINOR_DELIM);
//				int[] posList = g.keyToId(rawPosList);
//				int[] negList = g.keyToId(rawNegList);
				try {
					g = AnnotatedTroveGraph.fromStringParts(parts[4],g);
					result.add(new PosNegRWExample(g,queryVec,rawPosList,rawNegList));
				} catch (GraphFormatException e) {
					throw new IllegalArgumentException("["+e.getMessage()+"] at line "+linenum);
				}
					
			}
			reader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log.debug(TroveGraph.nextIdPeek());
		return result;
	}
	
	public Map<String,Double> trainParametersOnCookedIterator(Collection<PosNegRWExample> iteratorFactory) {
		return this.trainParametersOnCookedIterator(iteratorFactory, false);
	}
	public Map<String, Double> trainParametersOnCookedIterator(
			Collection<PosNegRWExample> importCookedExamples, boolean traceLosses) {
		return trainParametersOnCookedIterator(importCookedExamples, 5, traceLosses);
	}
	public Map<String, Double> trainParametersOnCookedIterator(
			Collection<PosNegRWExample> importCookedExamples, int numEpochs, boolean traceLosses) {
		return trainParametersOnCookedIterator(importCookedExamples, new TreeMap<String,Double>(), numEpochs, traceLosses);
	}
	public Map<String,Double> trainParametersOnCookedIterator(Collection<PosNegRWExample> examples, 
			Map<String, Double> initialParamVec, 
			int numEpochs, 
			boolean traceLosses) {
		log.info("Training on cooked examples...");
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
			setUpExamples(i,examples);
			for (PosNegRWExample x : examples) {
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
				System.out.println("training loss "+(totalLossThisEpoch / numExamplesThisEpoch) + " on "+ numExamplesThisEpoch +" examples");
				//if (this.testData)
			}
		}
//		cleanUpEpochs();
		log.info("Finished in "+(System.currentTimeMillis() - start)+" ms");
		return paramVec;
	}

	////////////////////////// Template methods /////////////////////////////////////

	protected double totalLossThisEpoch;
	protected int numExamplesThisEpoch;
	protected void doExample(int k, PosNegRWExample x, Map<String,Double> paramVec, boolean traceLosses) {
		log.debug("example "+x.toString()+" ...");
		this.learner.trainOnExample(paramVec, x);
		if (traceLosses) {
			totalLossThisEpoch += this.learner.empiricalLoss(paramVec, x);
			numExamplesThisEpoch += x.length();
		}
	}
	
	protected void setUpExamples(int epoch, Collection<PosNegRWExample> examples) {
		totalLossThisEpoch = 0;
		numExamplesThisEpoch = 0;
	}
	protected void cleanUpExamples(int epoch) {}
	
	protected void setUpEpochs(Map<String,Double> paramVec) {}

	//////////////////////////// Running /////////////////////////////
	private static final String USAGE = "Usage:\n\tcookedExampleFile outputParamFile [options]\n"
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
		
		L2PosNegLossTrainedSRW srw = new L2PosNegLossTrainedSRW();
		Trainer trainer = new Trainer(srw);
		Map<String,Double> paramVec = trainer.trainParametersOnCookedIterator(
				trainer.importCookedExamples(cookedExampleFile),
				epochs,
				traceLosses);
		Dictionary.save(paramVec, outputParamFile);
	}
}
