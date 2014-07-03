package edu.cmu.ml.praprolog.trove;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
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

//	/**
//	 * Generate the cooked examples stored in this file.
//	 * @param cookedExamplesFile
//	 * @return
//	 */
//	public Collection<PosNegRWExample> importCookedExamples(String cookedExamplesFile) {
//		log.info("Importing cooked examples from "+cookedExamplesFile);
//		Collection<PosNegRWExample> result = null;
//		result = importCookedExamples(new ParsedFile(cookedExamplesFile));
//		log.info("Imported "+result.size()+" examples");
//		return result;
//	}
//	public Collection<PosNegRWExample> importCookedExamples(ParsedFile file) {
//		ArrayList<PosNegRWExample> result = null;
//		result = new ArrayList<PosNegRWExample>();
//		for(String line : file){
//			log.debug("Imporing example from line "+file.getLineNumber());
//			AnnotatedTroveGraph g = new AnnotatedTroveGraph();
//
//			String[] parts = line.trim().split(MAJOR_DELIM,5);
//
//			TreeMap<String, Double> queryVec = new TreeMap<String,Double>();
//			for(String u : parts[1].split(MINOR_DELIM)) queryVec.put(u, 1.0);
//
////			String[] rawPosList = parts[2].split(MINOR_DELIM);
////			String[] rawNegList = parts[3].split(MINOR_DELIM);
//			String[] rawPosList, rawNegList;
//			if (parts[2].length()>0) rawPosList = parts[2].split(MINOR_DELIM);
//			else rawPosList = new String[0];
//			if (parts[3].length()>0) rawNegList = parts[3].split(MINOR_DELIM);
//			else rawNegList = new String[0];
//			//				int[] posList = g.keyToId(rawPosList);
//			//				int[] negList = g.keyToId(rawNegList);
//			if (rawPosList.length + rawNegList.length == 0) {
//				log.warn("no labeled solutions for example on line "+file.getAbsoluteLineNumber()+"; skipping");
//				continue;
//			}
//			try {
//				g = AnnotatedTroveGraph.fromStringParts(parts[4],g);
//				result.add(new PosNegRWExample(g,queryVec,rawPosList,rawNegList));
//			} catch (GraphFormatException e) {
//				file.parseError("["+e.getMessage()+"]");
//			}
//
//		}
//		file.close();
//		log.debug(TroveGraph.nextIdPeek());
//		return result;
//	}

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
		for (PosNegRWExample x : examples) {
		    this.learner.addDefaultWeights(x.getGraph(),paramVec);
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
		double previousAvgLoss = Double.MAX_VALUE;
		long start = System.currentTimeMillis();
		this.epoch = 0;
		ParamVector paramVec = this.learner.setupParams(initialParamVec);
		if (paramVec.size() == 0) {
			for (String f : this.learner.untrainedFeatures()) paramVec.put(f, 1.0);
		}
		setUpEpochs(paramVec);
		for (int i=0; i<numEpochs; i++) {
			this.epoch++;
			learner.setEpoch(this.epoch); 
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
				//				SRW.addDefaultWeights(x.getGraph(), paramVec);
				doExample(k, x, paramVec, traceLosses);

				k++;
			}
			cleanUpExamples(i,paramVec);
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
	protected void doExample(int k, PosNegRWExample x, ParamVector paramVec, boolean traceLosses) {
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
	protected void cleanUpExamples(int epoch, ParamVector paramVec) {}

	protected void setUpEpochs(ParamVector paramVec) {}

	//////////////////////////// Running /////////////////////////////
//	private static final String USAGE = "Usage:\n\tcookedExampleFile outputParamFile [options]\n"
//			+"\t\t--epochs {int}\tNumber of epochs (default 5)\n"
//			+"\t\t--traceLosses\tTurn on traceLosses (default off)\n"
//			+"\t\t             \tNB: example count for losses is sum(x.length() for x in examples)\n"
//			+"\t\t             \tand won't match `wc -l cookedExampleFile`\n";
//	private static void usage() {
//		System.err.println(USAGE);
//		System.exit(0);
//	}
//	public static void main(String[] args) {
//		if (args.length < 2) {
//			usage();
//		}
//
//		String cookedExampleFile = args[0];
//		String outputParamFile   = args[1];
//		int epochs = 5;
//		boolean traceLosses = false;
//		if (args.length > 2) {
//			for (int i=2; i<args.length; i++) {
//				if ("--epochs".equals(args[i])) {
//					if (i+1<args.length) epochs = Integer.parseInt(args[++i]);
//					else usage();
//				} else if ("--traceLosses".equals(args[i])) {
//					traceLosses = true;
//				} else usage();
//			}
//		}
//
//		L2PosNegLossTrainedSRW srw = new L2PosNegLossTrainedSRW();
//		Trainer trainer = new Trainer(srw);
//		Map<String,Double> paramVec = trainer.trainParametersOnCookedIterator(
//				trainer.importCookedExamples(cookedExampleFile),
//				epochs,
//				traceLosses);
//		Dictionary.save(paramVec, outputParamFile);
//	}
}
