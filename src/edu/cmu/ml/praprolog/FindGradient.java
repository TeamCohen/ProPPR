package edu.cmu.ml.praprolog;

import java.io.File;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.cmu.ml.praprolog.Tester.TestResults;
import edu.cmu.ml.praprolog.graph.AnnotatedGraphFactory;
import edu.cmu.ml.praprolog.trove.learn.CookedExampleStreamer;
import edu.cmu.ml.praprolog.learn.L2PosNegLossTrainedSRW;
import edu.cmu.ml.praprolog.prove.InnerProductWeighter;
import edu.cmu.ml.praprolog.trove.Trainer;
import edu.cmu.ml.praprolog.util.Configuration;
import edu.cmu.ml.praprolog.util.Dictionary;
import edu.cmu.ml.praprolog.util.ExperimentConfiguration;
import edu.cmu.ml.praprolog.util.ParamVector;
import edu.cmu.ml.praprolog.util.ParamsFile;

public class FindGradient {
	private static final Logger log = Logger.getLogger(FindGradient.class);
	public static void main(String[] args) {
		ExperimentConfiguration c = new ExperimentConfiguration(args, 
				Configuration.USE_DEFAULTS | Configuration.USE_TRAIN | Configuration.USE_PARAMS | Configuration.USE_LEARNINGSET);
		
		System.out.println(c.toString());
		
		log.info("Cooking training examples from "+c.dataFile+"...");
		long start = System.currentTimeMillis();
		c.cooker.cookExamples(c.dataFile, c.outputFile);
		
		// find gradient on the cooked training data
		log.info("Training model parameters...");
		Map<String,Double> batchGradient;
		if (c.trove) {
			ParamVector paramVec = null;
			Trainer trainer = (Trainer) c.trainer;
			if (c.epochs>0) {
			    paramVec = trainer.trainParametersOnCookedIterator(new CookedExampleStreamer(c.outputFile), c.epochs, c.traceLosses);
			}
			batchGradient = trainer.findGradient(new CookedExampleStreamer(c.outputFile),paramVec);
		} else {
		    throw new UnsupportedOperationException("non-trove implementation? it's in the mail.");
		}
		ParamsFile.save(batchGradient,new File(c.paramsFile),c);
//		Dictionary.save(batchGradient,c.paramsFile);
	}
}
