package edu.cmu.ml.proppr.v1;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.graph.v1.AnnotatedGraphFactory;
import edu.cmu.ml.proppr.learn.L2PosNegLossTrainedSRW;
import edu.cmu.ml.proppr.learn.tools.GroundedExampleStreamer;
import edu.cmu.ml.proppr.prove.v1.InnerProductWeighter;
import edu.cmu.ml.proppr.trove.Trainer;
import edu.cmu.ml.proppr.util.Configuration;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.ExperimentConfiguration;
import edu.cmu.ml.proppr.util.ParamVector;
import edu.cmu.ml.proppr.util.ParamsFile;
import edu.cmu.ml.proppr.v1.Tester.TestResults;

public class Experiment {
	private static final Logger log = Logger.getLogger(Experiment.class);
	public static void main(String[] args) {
		ExperimentConfiguration c = new ExperimentConfiguration(args, 
				Configuration.USE_DEFAULTS | Configuration.USE_TRAINTEST | Configuration.USE_LEARNINGSET | ExperimentConfiguration.USE_QUERYANSWERER);
		
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
		ParamVector paramVec = null;
		if (c.trove) {
			Trainer trainer = (Trainer) c.trainer;
			paramVec = trainer.trainParametersOnCookedIterator(new edu.cmu.ml.proppr.trove.learn.tools.CookedExampleStreamer(c.outputFile), c.epochs, c.traceLosses);
		} else {
			edu.cmu.ml.proppr.v1.Trainer<String> trainer = (edu.cmu.ml.proppr.v1.Trainer<String>) c.trainer;
			paramVec = trainer.trainParametersOnCookedIterator(
				new GroundedExampleStreamer<String>(c.outputFile, new AnnotatedGraphFactory<String>(AnnotatedGraphFactory.STRING)),
				c.epochs,
				c.traceLosses);
		}
		//log.info("paramVec="+Dictionary.buildString(paramVec, new StringBuilder(), "\n").toString());
		if (c.paramsFile != null) {
			log.info("Saving parameters to "+c.paramsFile+"...");
			ParamsFile.save(paramVec, new File(c.paramsFile), c);
//			Dictionary.save(paramVec, c.paramsFile);
		}
		
		if (c.tester != null) {
			// test trained parameters
			log.info("Testing on "+c.testFile+"...");
			c.tester.setParams(paramVec, c.weightingScheme);
			TestResults results = c.tester.testExamples(c.testFile,c.strict);
			if(!log.isInfoEnabled())  {
				System.out.println("result= running time "+(System.currentTimeMillis() - start));
				System.out.println("result= pairs "+ results.pairTotal+" errors "+results.pairErrors+" errorRate "+results.errorRate+" map "+results.map);
			} else {
				log.info("result= running time "+(System.currentTimeMillis() - start));
				log.info("result= pairs "+ results.pairTotal+" errors "+results.pairErrors+" errorRate "+results.errorRate+" map "+results.map);
			}
		}
		
		if (c.queryAnswerer != null) {
	        log.info("Running queries from " + c.queryFile + "; saving results to " + c.solutionsFile);

            c.queryAnswerer.addParams(c.program, paramVec, c.weightingScheme);

            try {
				c.queryAnswerer.findSolutions(c.program, c.prover, c.queryFile, c.solutionsFile, c.normalize);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
