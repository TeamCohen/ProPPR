package edu.cmu.ml.proppr;

import java.io.File;
import java.io.IOException;
import java.io.File;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.graph.ArrayLearningGraphBuilder;
import edu.cmu.ml.proppr.learn.tools.RWExampleParser;
import edu.cmu.ml.proppr.util.Configuration;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.ModuleConfiguration;
import edu.cmu.ml.proppr.util.ParamsFile;
import edu.cmu.ml.proppr.util.ParsedFile;
import edu.cmu.ml.proppr.util.SimpleSymbolTable;
import edu.cmu.ml.proppr.util.SymbolTable;
import edu.cmu.ml.proppr.util.math.ParamVector;
import edu.cmu.ml.proppr.util.math.SimpleParamVector;
import gnu.trove.map.TObjectDoubleMap;

/**
 * A routine to ground a set of queries, optionally train the model, then output the gradient of each parameter.
 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
 *
 */
public class GradientFinder {
	private static final Logger log = Logger.getLogger(GradientFinder.class);

	public static void main(String[] args) {
		try {
			int inputFiles = Configuration.USE_GROUNDED | Configuration.USE_PARAMS;
			int outputFiles = Configuration.USE_GRADIENT;
			int modules = Configuration.USE_TRAINER | Configuration.USE_SRW | Configuration.USE_SQUASHFUNCTION;
			int constants = Configuration.USE_THREADS | Configuration.USE_TRACELOSSES | Configuration.USE_EPOCHS | Configuration.USE_FORCE;
			ModuleConfiguration c = new ModuleConfiguration(args, inputFiles, outputFiles, constants, modules) {
				@Override
				protected void retrieveSettings(CommandLine line, int[] allFlags, Options options) throws IOException {
					super.retrieveSettings(line, allFlags, options);
					if (groundedFile==null || !groundedFile.exists())
						usageOptions(options, allFlags, "Must specify grounded file using --"+Configuration.GROUNDED_FILE_OPTION);
					if (gradientFile==null) 
						usageOptions(options, allFlags, "Must specify gradient using --"+Configuration.GRADIENT_FILE_OPTION);
					//					epochs = 1;
				}
				@Override
				protected void addOptions(Options options, int[] allFlags) {
					super.addOptions(options, allFlags);
					options.getOption(PARAMS_FILE_OPTION).setRequired(false);
				}
			};
			System.out.println(c.toString());

			ParamVector params = null;
			File nullFile = null;

			SymbolTable<String> masterFeatures = new SimpleSymbolTable<String>();
			File featureIndex = new File(c.groundedFile.getParent(),c.groundedFile.getName()+Grounder.FEATURE_INDEX_EXTENSION);
			if (featureIndex.exists()) {
				log.info("Reading feature index from "+featureIndex.getName()+"...");
				for (String line : new ParsedFile(featureIndex)) {
					masterFeatures.insert(line.trim());
				}
			}
			
			if (c.epochs > 0) {
				// train first
				params = c.trainer.train(
						masterFeatures,
						new ParsedFile(c.groundedFile), 
						new ArrayLearningGraphBuilder(), 
						nullFile, // create a parameter vector
						c.epochs, 
						c.traceLosses);
			} else if (c.paramsFile != null) {
				params = new SimpleParamVector<String>(Dictionary.load(new ParsedFile(c.paramsFile)));
			} else {
				params = new SimpleParamVector<String>();
			}

			ParamVector batchGradient = c.trainer.findGradient(
					masterFeatures,
					new ParsedFile(c.groundedFile), 
					new ArrayLearningGraphBuilder(), 
					params);

			ParamsFile.save(batchGradient, c.gradientFile, c);

		} catch (Throwable t) {
			t.printStackTrace();
			System.exit(-1);
		}
	}
}
