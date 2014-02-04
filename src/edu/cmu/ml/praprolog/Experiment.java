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

public class Experiment {
	private static final Logger log = Logger.getLogger(Experiment.class);
	public static void main(String[] args) {
		ExperimentConfiguration c = new ExperimentConfiguration(args, 
				Configuration.USE_DEFAULTS | Configuration.USE_TRAINTEST | Configuration.USE_LEARNINGSET);
		
		System.out.println(c.toString());
		
		if (c.pretest) {
			log.info("Pre-test...");
			TestResults results = c.tester.testExamples(c.testFile,c.strict);
			if(!log.isInfoEnabled()) System.out.println("result= pairs "+ results.pairTotal+" errors "+results.pairErrors+" errorRate "+results.errorRate+" map "+results.map);
			else log.info("result= pairs "+ results.pairTotal+" errors "+results.pairErrors+" errorRate "+results.errorRate+" map "+results.map);
		}
		
		// first cook the training data
//		if (c.nthreads < 0) cooker = new ExampleCooker(c.prover,c.programFiles,c.alpha);
//		else cooker = new ModularMultiExampleCooker(c.prover, c.programFiles, c.alpha, c.nthreads);
		// wait until after program is loaded to start timing
		log.info("Cooking training examples from "+c.dataFile+"...");
		long start = System.currentTimeMillis();
		c.cooker.cookExamples(c.dataFile, c.outputFile);
		
		// train parameters on the cooked training data
		log.info("Training model parameters...");
		Map<String,Double> paramVec = null;
		if (c.trove) {
			Trainer trainer = (Trainer) c.trainer;
			paramVec = trainer.trainParametersOnCookedIterator(trainer.importCookedExamples(c.outputFile), c.epochs, c.traceLosses);
		} else {
			edu.cmu.ml.praprolog.Trainer<String> trainer = (edu.cmu.ml.praprolog.Trainer<String>) c.trainer;
			paramVec = trainer.trainParametersOnCookedIterator(
				trainer.importCookedExamples(c.outputFile, new AnnotatedGraphFactory<String>(AnnotatedGraphFactory.STRING)),
				c.epochs,
				c.traceLosses);
		}
		//log.info("paramVec="+Dictionary.buildString(paramVec, new StringBuilder(), "\n").toString());
		if (c.paramsFile != null) {
			log.info("Saving parameters to "+c.paramsFile+"...");
			Dictionary.save(paramVec, c.paramsFile);
		}
		
		// test trained parameters
		log.info("Testing on "+c.testFile+"...");
		c.tester.setParams(paramVec);
		TestResults results = c.tester.testExamples(c.testFile,c.strict);
		if(!log.isInfoEnabled())  {
			System.out.println("result= running time "+(System.currentTimeMillis() - start));
			System.out.println("result= pairs "+ results.pairTotal+" errors "+results.pairErrors+" errorRate "+results.errorRate+" map "+results.map);
		} else {
			log.info("result= running time "+(System.currentTimeMillis() - start));
			log.info("result= pairs "+ results.pairTotal+" errors "+results.pairErrors+" errorRate "+results.errorRate+" map "+results.map);
		}
	}
}
