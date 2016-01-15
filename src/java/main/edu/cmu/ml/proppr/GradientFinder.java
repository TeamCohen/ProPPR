package edu.cmu.ml.proppr;

import java.io.File;
import java.io.IOException;
import java.io.File;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.graph.ArrayLearningGraphBuilder;
import edu.cmu.ml.proppr.learn.tools.FixedWeightRules;
import edu.cmu.ml.proppr.learn.tools.RWExampleParser;
import edu.cmu.ml.proppr.util.Configuration;
import edu.cmu.ml.proppr.util.CustomConfiguration;
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
			int inputFiles = Configuration.USE_GROUNDED | Configuration.USE_INIT_PARAMS;
			int outputFiles = Configuration.USE_GRADIENT | Configuration.USE_PARAMS;
			int modules = Configuration.USE_TRAINER | Configuration.USE_SRW | Configuration.USE_SQUASHFUNCTION;
			int constants = Configuration.USE_THREADS | Configuration.USE_EPOCHS | Configuration.USE_FORCE | Configuration.USE_FIXEDWEIGHTS;
			CustomConfiguration c = new CustomConfiguration(args, inputFiles, outputFiles, constants, modules) {
				boolean relax;
				
				@Override
				protected Option checkOption(Option o) {
					if (PARAMS_FILE_OPTION.equals(o.getLongOpt()) ||
							INIT_PARAMS_FILE_OPTION.equals(o.getLongOpt()))
						o.setRequired(false);
					return o;
				}

				@Override
				protected void addCustomOptions(Options options, int[] flags) {
				    options.addOption(Option.builder()
						      .longOpt("relaxFW")
						      .desc("Relax fixedWeight rules for gradient computation (used in ProngHorn)")
						      .optionalArg(true).build());
				}

				@Override
				protected void retrieveCustomSettings(CommandLine line,
						int[] flags, Options options) {
					if (groundedFile==null || !groundedFile.exists())
						usageOptions(options, flags, "Must specify grounded file using --"+Configuration.GROUNDED_FILE_OPTION);
					if (gradientFile==null) 
						usageOptions(options, flags, "Must specify gradient using --"+Configuration.GRADIENT_FILE_OPTION);
					// default to 0 epochs
					if (!options.hasOption("epochs")) this.epochs = 0;
					this.relax = false;
					if (options.hasOption("relaxFW")) this.relax = true;
				}

				@Override
				public Object getCustomSetting(String name) {
					if ("relaxFW".equals(name)) return this.relax;
					return null;
				}
			};
			System.out.println(c.toString());

			ParamVector<String,?> params = null;

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
			    log.info("Training for "+c.epochs+" epochs...");
				params = c.trainer.train(
						masterFeatures,
						new ParsedFile(c.groundedFile), 
						new ArrayLearningGraphBuilder(), 
						c.initParamsFile, // create a parameter vector
						c.epochs);
				if (c.paramsFile != null) ParamsFile.save(params, c.paramsFile, c);
			} else if (c.initParamsFile != null) {
				params = new SimpleParamVector<String>(Dictionary.load(new ParsedFile(c.initParamsFile)));
			} else if (c.paramsFile != null) {
				params = new SimpleParamVector<String>(Dictionary.load(new ParsedFile(c.paramsFile)));
			} else {
				params = new SimpleParamVector<String>();
			}
			
			// turn off any fixed-weight settings for computing the gradient
			// this lets prongHorn hold external features fixed for training, but still compute their gradient
			if (((Boolean) c.getCustomSetting("relaxFW"))) {
			    log.info("Turning off fixedWeight rules");
			    c.trainer.setFixedWeightRules(new FixedWeightRules());
			}
			
			ParamVector<String,?> batchGradient = c.trainer.findGradient(
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
