package edu.cmu.ml.praprolog.util;

import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import edu.cmu.ml.praprolog.ExampleCooker;
import edu.cmu.ml.praprolog.ModularMultiExampleCooker;
import edu.cmu.ml.praprolog.MultithreadedExampleCooker;
import edu.cmu.ml.praprolog.MultithreadedTester;
import edu.cmu.ml.praprolog.QueryAnswerer;
import edu.cmu.ml.praprolog.RerankingQueryAnswerer;
import edu.cmu.ml.praprolog.learn.LinearWeightingScheme;
import edu.cmu.ml.praprolog.learn.PosNegRWExample;
import edu.cmu.ml.praprolog.learn.SRW;
import edu.cmu.ml.praprolog.learn.SigmoidWeightingScheme;
import edu.cmu.ml.praprolog.learn.ReLUWeightingScheme;
import edu.cmu.ml.praprolog.learn.TanhWeightingScheme;
import edu.cmu.ml.praprolog.learn.WeightingScheme;
import edu.cmu.ml.praprolog.prove.Component;
import edu.cmu.ml.praprolog.prove.LogicProgram;
import edu.cmu.ml.praprolog.prove.feat.ComplexFeatureLibrary;
import edu.cmu.ml.praprolog.trove.MultithreadedRRTrainer;
import edu.cmu.ml.praprolog.trove.MultithreadedTrainer;
import edu.cmu.ml.praprolog.Tester;
import edu.cmu.ml.praprolog.trove.Trainer;
import edu.cmu.ml.praprolog.trove.learn.L2PosNegLossTrainedSRW;

public class ExperimentConfiguration extends Configuration {
	/** queries, notest, srw **/
	public static final int USE_QUERYANSWERER = USE_QUERIES | USE_SOLUTIONS | USE_NOTEST | USE_SRW;
	public ExampleCooker cooker;
	public Object srw;
	public Object trainer;
	public Tester tester;
	public boolean trove;//=true;
	public boolean pretest;//=false;
	public boolean strict;//=false;
	public LogicProgram program;
	public QueryAnswerer queryAnswerer;
	public boolean normalize;
	public String solutionsFile;
	public ExperimentConfiguration(String[] args, int flags) {
		super(args, flags);
	}

	@Override
	protected void addOptions(Options options, int flags) {
		super.addOptions(options, flags);
		options.addOption(
				OptionBuilder
				.withLongOpt("strict")
				.withDescription("Fail immediately if an unexpected state occurs (default off)")
				.create());
		options.addOption(
				OptionBuilder
				.withLongOpt("pretest")
				.withDescription("Run and report accuracy on test examples before training")
				.create());
		options.addOption(
				OptionBuilder
				.withLongOpt("cooker")
				.withArgName("class[:arg]")
				.hasArgs()
				.withValueSeparator(':')
				.withDescription("Default: mmc:3\n"
						+ "Available options:\n"
						+ "ec\n"
						+ "mec[:threads] (default threads=3)\n"
						+ "mmc[:threads] (default threads=3)")
						.create());
		options.addOption(
				OptionBuilder
				.withLongOpt("trainer")
				.withArgName("class[:arg]")
				.hasArgs()
				.withValueSeparator(':')
				.withDescription("Default: trove.mrr:3\n"
						+ "Available options:\n"
						+ "t\n"
						+ "mt[:threads] (default threads=3)\n"
						+ "mrr[:threads] (default threads=3)\n"
						+ "trove.t\n"
						+ "trove.mt[:threads] (default threads=3)\n"
						+ "trove.mrr[:threads] (default threads=3)")
						.create());
		options.addOption(
				OptionBuilder
				.withLongOpt("srw")
				.withArgName("class")
				.hasArgs()
				.withValueSeparator(':')
				.withDescription("Default: l2p (L2PosNegLossTrainedSRW)\n"
						+ "Default mu=.001\n"
						+ "Default eta=1.0\n"
						+ "Available options:\n"
						+ "l2p[:mu[:eta]] (L2PosNegLossTrainedSRW)\n"
						+ "l2plocal[:mu[:eta]] (LocalL2PosNegLossTrainedSRW)")
						.create());
		options.addOption(
				OptionBuilder
				.withLongOpt("weightingScheme")
				.withArgName("scheme")
				.hasArg()
				.withDescription("Default: tanh\n"
						+ "Available options:\n"
						+ "tanh\n"
						+ "sigmoid\n"
						+ "ReLU")
						.create());
		options.addOption(
				OptionBuilder
				.withLongOpt("tester")
				.withArgName("class[:arg]")
				.hasArgs()
				.withValueSeparator(':')
				.withDescription("Default: t\n"
						+ "Available options:\n"
						+ "t Tester\n"
						+ "mt[:threads] (default threads=3) MultithreadedTester\n"
						+ "rt RerankingTester")
						.create());
		options.addOption(
				OptionBuilder
				.withLongOpt("seed")
				.withArgName("s")
				.hasArg()
				.withDescription("Seed the SRW random number generator")
				.create());
		if (isOn(flags, USE_QUERYANSWERER)) {
			options.addOption(
					OptionBuilder
					.withLongOpt("queryAnswerer")
					.withArgName("class[:arg]")
					.hasArgs()
					.withValueSeparator(':')
					.withDescription("Runs QA after training. Default: none\n"
							+"Available options:\n"
							+"qa[:{norm|unnorm}] QueryAnswerer, normalized (default) or unnormalized\n"
							+"rqa[:{norm|unnorm}] RerankingQueryAnswerer")
							.create());
			options.addOption(
					OptionBuilder
					.withLongOpt("notest")
					.withDescription("Don't run Tester on test examples")
					.create());
			options.addOption(
					OptionBuilder
					.withLongOpt("solutions")
					.withArgName("file.txt")
					.hasArg()
					.withDescription("Save QA solutions here")
					.create());
		}
	}

	private void vanillaSeed(CommandLine line) {
		if (!line.hasOption("seed")) return;
		long seed = Long.parseLong(line.getOptionValue("seed"));
		edu.cmu.ml.praprolog.learn.SRW.seed(seed);
	}

	private void troveSeed(CommandLine line) {
		if (!line.hasOption("seed")) return;
		long seed = Long.parseLong(line.getOptionValue("seed"));
		edu.cmu.ml.praprolog.trove.learn.SRW.seed(seed);
	}

	@Override
	protected void retrieveSettings(CommandLine line, int flags, Options options) {
		super.retrieveSettings(line, flags, options);

		this.pretest=false;
		if (line.hasOption("pretest")) this.pretest = true;
		this.strict=false;
		if (line.hasOption("strict")) this.strict = true;

		if (isOn(flags,Configuration.USE_PROGRAMFILES)) {
			if (this.programFiles != null) 
			this.program = new LogicProgram(Component.loadComponents(programFiles, this.alpha));
			else if (!isOn(flags,Configuration.USE_DEFERREDPROGRAM)) missing(Configuration.USE_PROGRAMFILES, flags);
		}
		
		// TODO: There are likely other logic errors below for things that need a program if we've deferred it

		if (isOn(flags, USE_COMPLEX_FEATURES) && line.hasOption("complexFeatures")) {
			ComplexFeatureLibrary.init(this.program, this.complexFeatureConfigFile);
		}

		int threads = 3;
		if(line.hasOption("threads")) threads = this.nthreads;
		if (line.hasOption("cooker")) {
			String[] values = line.getOptionValues("cooker");
			if (values[0].equals("ec")) {
				this.cooker = new ExampleCooker(this.prover,this.program);
			} else {
				if (values.length > 1) threads = Integer.parseInt(values[1]);
				if (values[0].equals("mec")) {
					this.cooker = new MultithreadedExampleCooker(this.prover, this.program, threads);
				} else if (values[0].equals("mmc")) {
					this.cooker = new ModularMultiExampleCooker(this.prover, this.program, threads);
				}
			}
		} else this.cooker = new ModularMultiExampleCooker(this.prover, this.program, threads);

		this.trove=true;
		threads = 3;
		if(line.hasOption("threads")) threads = this.nthreads;
		if (line.hasOption("trainer")) {
			String[] values = line.getOptionValues("trainer");
			if (values[0].startsWith("trove")) {
				this.trove = true;
				this.setupSRW(line, flags, options);
				troveSeed(line);
			} else {
				this.trove = false;
				this.setupSRW(line, flags, options);
				vanillaSeed(line);
			}
			if (values.length > 1) {
				threads = Integer.parseInt(values[1]);
			}
			if (values[0].equals("t")) {
				this.trainer = new edu.cmu.ml.praprolog.Trainer<String>((edu.cmu.ml.praprolog.learn.L2PosNegLossTrainedSRW<String>) this.srw);
			} else if (values[0].equals("mt")) {
				this.trainer = new edu.cmu.ml.praprolog.MultithreadedTrainer<String>((edu.cmu.ml.praprolog.learn.L2PosNegLossTrainedSRW<String>) this.srw, threads);
			} else if (values[0].equals("mrr")) {
				this.trainer = new edu.cmu.ml.praprolog.MultithreadedRRTrainer<String>((edu.cmu.ml.praprolog.learn.L2PosNegLossTrainedSRW<String>) this.srw, threads);
			} else if (values[0].equals("trove.t")) {
				this.trainer = new Trainer( (L2PosNegLossTrainedSRW) this.srw);
			} else if (values[0].equals("trove.mt")) {
				this.trainer = new MultithreadedTrainer( (L2PosNegLossTrainedSRW) this.srw, threads);				
			} else if (values[0].equals("trove.mrr")) {
				this.trainer = new MultithreadedRRTrainer( (L2PosNegLossTrainedSRW) this.srw, threads);		
			}
		} else {
			this.trove = true;
			this.setupSRW(line, flags, options);
			troveSeed(line);
			this.trainer = new MultithreadedRRTrainer( (L2PosNegLossTrainedSRW) this.srw, threads);	
		}

		threads = 3;
		if(line.hasOption("threads")) threads = this.nthreads;
		if (isOn(flags,USE_NOTEST)) { // if NOTEST is available...
			if (line.hasOption("notest")) { // ..and the user has engaged it...
				this.tester = null; // don't use a Tester
			} else { // ...and we need to test,
				if (isOn(flags,USE_TEST) && !line.hasOption("test")) { // ...but the user hasn't specified a file...
					System.err.println("Missing required option: one of\n\t--test <file>\n\t--notest"); // give up
					usageOptions(options, flags);
				}
			}
		}
		if (!isOn(flags,USE_NOTEST) || !line.hasOption("notest")) {
			if (line.hasOption("tester")) {
				String[] values = line.getOptionValues("tester");
				if (values[0].equals("t")) {
					this.tester = new Tester(this.prover, this.program);
				} else {
					if (values.length > 1) threads = Integer.parseInt(values[1]);
					if (values[0].equals("mt")) {
						this.tester = new MultithreadedTester(this.prover, this.program, threads);
					} else if (values[0].equals("rt")) {
						if (this.srw == null) this.setupSRW(line,flags,options);
						if (this.trove) {
							this.tester = new edu.cmu.ml.praprolog.trove.RerankingTester(
									this.prover, 
									this.program, 
									(edu.cmu.ml.praprolog.trove.learn.L2PosNegLossTrainedSRW) this.srw);
						} else {
							this.tester = new edu.cmu.ml.praprolog.RerankingTester(
									this.prover, 
									this.program, 
									(edu.cmu.ml.praprolog.learn.L2PosNegLossTrainedSRW<String>) this.srw);
						}
					} 
				}
			} else this.tester = new Tester(this.prover, this.program);
		}

		if (isOn(flags, USE_SRW) && this.srw==null) this.setupSRW(line,flags,options);

		this.normalize = true;
		if (line.hasOption("queryAnswerer")) {
			String[] values = line.getOptionValues("queryAnswerer");
			if (values[0].equals("qa"))
				this.queryAnswerer = new QueryAnswerer();
			else if (values[0].equals("rqa"))
				this.queryAnswerer = new RerankingQueryAnswerer((SRW<PosNegRWExample<String>>) this.srw);
			else {
				System.err.println("No queryAnswerer option '"+values[0]+"'");
				usageOptions(options, flags);
			}
			if (values.length > 1) {
				this.normalize = values[1].equals("norm");
			}

			if (line.hasOption("solutions")) {
				this.solutionsFile = line.getOptionValue("solutions");
			} else {
				System.err.println("Missing required option: solutions");
				usageOptions(options,flags);
			}
		}
	}

	protected void setupSRW(CommandLine line, int flags, Options options) {
		double mu = SRW.DEFAULT_MU;
		double eta = SRW.DEFAULT_ETA;
		double delta = SRW.DEFAULT_DELTA;

		if (line.hasOption("srw")) {
			String[] values = line.getOptionValues("srw");
			if (values.length > 1) {
				mu = Double.parseDouble(values[1]);
			}
			if (values.length > 2) {
				eta = Double.parseDouble(values[2]);
			}
			if (values.length > 3) {
				delta = Double.parseDouble(values[3]);
			}
			if (values[0].equals("l2p")) {
				if (this.trove) {
					this.srw = new edu.cmu.ml.praprolog.trove.learn.L2PosNegLossTrainedSRW(SRW.DEFAULT_MAX_T,mu,eta,weightingScheme,delta);
				} else {
					this.srw = new edu.cmu.ml.praprolog.learn.L2PosNegLossTrainedSRW<String>(SRW.DEFAULT_MAX_T,mu,eta,weightingScheme,delta);
				}
			} else if (values[0].equals("l2plocal")) {
				if (this.trove) {
					this.srw = new edu.cmu.ml.praprolog.trove.learn.LocalL2PosNegLossTrainedSRW(SRW.DEFAULT_MAX_T,mu,eta,weightingScheme,delta);
				} else {
					this.srw = new edu.cmu.ml.praprolog.learn.LocalL2PosNegLossTrainedSRW<String>(SRW.DEFAULT_MAX_T,mu,eta,weightingScheme,delta);
				}
			} else {
				System.err.println("No srw definition for '"+values+"'");
				usageOptions(options,flags);
			}
		} else {
			if (this.trove) {
				this.srw = new edu.cmu.ml.praprolog.trove.learn.L2PosNegLossTrainedSRW(SRW.DEFAULT_MAX_T,mu,eta,weightingScheme,delta);
			} else {
				this.srw = new edu.cmu.ml.praprolog.learn.L2PosNegLossTrainedSRW<String>(SRW.DEFAULT_MAX_T,mu,eta,weightingScheme,delta);
			}
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(super.toString()).append(":\n");
		sb.append("Cooker: ").append(cooker.getClass().getCanonicalName()).append("\n");
		sb.append("Walker: ").append(srw.getClass().getCanonicalName()).append("\n");
		sb.append("Trainer: ").append(trainer.getClass().getCanonicalName()).append("\n");
		sb.append("Tester: ");
		if (tester != null) 
			sb.append(tester.getClass().getCanonicalName()).append("\n");
		else 
			sb.append("none\n");
		if (queryAnswerer != null) {
			sb.append("QueryAnswerer: ").append(queryAnswerer.getClass().getCanonicalName());
			sb.append(" (").append(this.normalize ? "normalized" : "unnormalized").append(")\n");
		}
		sb.append("Weighting Scheme: ").append(weightingScheme.getClass().getCanonicalName()).append("\n");
		sb.append("Pretest? ").append(this.pretest ? "yes" : "no").append("\n");
		sb.append("Strict? ").append(this.strict ? "yes" : "no").append("\n");
		return sb.toString();
	}
}
