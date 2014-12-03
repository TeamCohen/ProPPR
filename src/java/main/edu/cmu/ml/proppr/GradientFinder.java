package edu.cmu.ml.proppr;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.graph.SimpleLearningGraph;
import edu.cmu.ml.proppr.learn.tools.GroundedExampleStreamer;
import edu.cmu.ml.proppr.util.Configuration;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.ModuleConfiguration;
import edu.cmu.ml.proppr.util.ParamVector;
import edu.cmu.ml.proppr.util.ParamsFile;
import edu.cmu.ml.proppr.util.ParsedFile;
import edu.cmu.ml.proppr.util.SimpleParamVector;

/**
 * A routine to ground a set of queries, optionally train the model, then output the gradient of each parameter.
 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
 *
 */
public class GradientFinder {
	private static final Logger log = Logger.getLogger(GradientFinder.class);

	public static void main(String[] args) {
		int inputFiles = 0;
		int outputFiles = Configuration.USE_QUERIES | Configuration.USE_GROUNDED | Configuration.USE_PARAMS | Configuration.USE_GRADIENT;
		int modules = Configuration.USE_TRAINER | Configuration.USE_SRW | Configuration.USE_GROUNDER | Configuration.USE_PROVER | Configuration.USE_WEIGHTINGSCHEME;
		int constants = Configuration.USE_EPOCHS | Configuration.USE_THREADS | Configuration.USE_TRACELOSSES | Configuration.USE_WAM;
		ModuleConfiguration c = new ModuleConfiguration(args, inputFiles, outputFiles, constants, modules) {
			@Override
			protected void retrieveSettings(CommandLine line, int[] allFlags, Options options) throws IOException {
				super.retrieveSettings(line, allFlags, options);
				if (epochs < 0 && (paramsFile==null || !paramsFile.exists())) usageOptions(options, allFlags, "You specified no training (epochs<0) but params file does not exist! @"+paramsFile.getAbsolutePath());
				if (!( (queryFile != null && queryFile.exists()) || (groundedFile != null && groundedFile.exists()))) 
					usageOptions(options, allFlags, "Must specify queries somehow using --"+Configuration.QUERIES_FILE_OPTION+" or --"+Configuration.GROUNDED_FILE_OPTION);
			}
		};
		System.out.println(c.toString());
		
		if (!c.groundedFile.exists()) {
			log.info("Grounding examples from "+c.queryFile.getName()+"...");
			c.grounder.groundExamples(c.queryFile, c.groundedFile);
		}
		
		ParamVector params = null;
		if (c.epochs > 0) {
			params = c.trainer.train(
					new GroundedExampleStreamer(new ParsedFile(c.groundedFile), new SimpleLearningGraph.SLGBuilder()), 
					c.epochs, 
					c.traceLosses);
		} else {
			params = new SimpleParamVector<String>(Dictionary.load(new ParsedFile(c.paramsFile)));
		}
		
		Map<String,Double> batchGradient = c.trainer.findGradient(
					new GroundedExampleStreamer(new ParsedFile(c.groundedFile), new SimpleLearningGraph.SLGBuilder()), 
					params);
		
		ParamsFile.save(batchGradient, c.gradientFile, c);
	}
}
