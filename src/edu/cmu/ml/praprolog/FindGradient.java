package edu.cmu.ml.praprolog;

import java.util.Map;

import org.apache.log4j.Logger;

import edu.cmu.ml.praprolog.Tester.TestResults;
import edu.cmu.ml.praprolog.graph.AnnotatedGraphFactory;
import edu.cmu.ml.praprolog.learn.L2PosNegLossTrainedSRW;
import edu.cmu.ml.praprolog.prove.InnerProductWeighter;
import edu.cmu.ml.praprolog.trove.Trainer;
import edu.cmu.ml.praprolog.util.Configuration;
import edu.cmu.ml.praprolog.util.Dictionary;
import edu.cmu.ml.praprolog.util.ExperimentConfiguration;

public class FindGradient {
	private static final Logger log = Logger.getLogger(Experiment.class);
	public static void main(String[] args) {
		ExperimentConfiguration c = new ExperimentConfiguration(args, 
				Configuration.USE_DEFAULTS | Configuration.USE_TRAIN | Configuration.USE_PARAMS);
		
		System.out.println(c.toString());
		
		log.info("Cooking training examples from "+c.dataFile+"...");
		long start = System.currentTimeMillis();
		c.cooker.cookExamples(c.dataFile, c.outputFile);
		
		// find gradient on the cooked training data
		log.info("Training model parameters...");
		Map<String,Double> batchGradient;
		if (c.trove) {
			Trainer trainer = (Trainer) c.trainer;
			batchGradient = trainer.findGradient(trainer.importCookedExamples(c.outputFile));
		} else {
		    throw new UnsupportedOperationException("non-trove implementation? it's in the mail.");
		}
		Dictionary.save(batchGradient,c.paramsFile);
	}
}
