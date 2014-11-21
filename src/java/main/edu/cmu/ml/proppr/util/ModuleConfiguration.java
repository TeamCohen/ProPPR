package edu.cmu.ml.proppr.util;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import edu.cmu.ml.proppr.Grounder;
import edu.cmu.ml.proppr.Trainer;
import edu.cmu.ml.proppr.learn.AprSRW;
import edu.cmu.ml.proppr.learn.SRW;
import edu.cmu.ml.proppr.learn.tools.ExpWeightingScheme;
import edu.cmu.ml.proppr.learn.tools.LinearWeightingScheme;
import edu.cmu.ml.proppr.learn.tools.ReLUWeightingScheme;
import edu.cmu.ml.proppr.learn.tools.SRWParameters;
import edu.cmu.ml.proppr.learn.tools.SigmoidWeightingScheme;
import edu.cmu.ml.proppr.learn.tools.TanhWeightingScheme;
import edu.cmu.ml.proppr.learn.tools.WeightingScheme;
import edu.cmu.ml.proppr.prove.DfsProver;
import edu.cmu.ml.proppr.prove.DprProver;
import edu.cmu.ml.proppr.prove.PprProver;
import edu.cmu.ml.proppr.prove.Prover;
import edu.cmu.ml.proppr.prove.TracingDfsProver;
import edu.cmu.ml.proppr.util.multithreading.Multithreading;

public class ModuleConfiguration extends Configuration {
	private static final String SEED_CONST_OPTION = "seed";
	private static final String SRW_MODULE_OPTION = "srw";
	private static final String TRAINER_MODULE_OPTION = "trainer";
	private static final String GROUNDER_MODULE_OPTION = "grounder";
	private static final String WEIGHTINGSCHEME_MODULE_OPTION = "weightingScheme";
	private static final String PROVER_MODULE_OPTION = "prover";

	private enum PROVERS { ppr, dpr, dfs, tr };
	private enum WEIGHTINGSCHEMES { linear, sigmoid, tanh, ReLU, exp };
	/** queries, notest, srw **/
	//	public static final int USE_QUERYANSWERER = USE_QUERIES | USE_SOLUTIONS | USE_NOTEST | USE_SRW;
	public Grounder grounder;
	public SRW srw;
	public Trainer trainer;
	//	public Tester tester;
	//	public QueryAnswerer queryAnswerer;
	//	public boolean pretest;//=false;
	//	public boolean strict;//=false;
	//	public boolean normalize;
	//	public String solutionsFile;
	public WeightingScheme weightingScheme;
	public Prover prover;
	public ModuleConfiguration(String[] args, int inputFiles, int outputFiles, int constants, int modules) {
		super(args,  inputFiles,  outputFiles,  constants,  modules);
	}

	@Override
	protected void addOptions(Options options, int[] allFlags) {
		super.addOptions(options, allFlags);
		//		options.addOption(
		//				OptionBuilder
		//				.withLongOpt("strict")
		//				.withDescription("Fail immediately if an unexpected state occurs (default off)")
		//				.create());
		//		options.addOption(
		//				OptionBuilder
		//				.withLongOpt("pretest")
		//				.withDescription("Run and report accuracy on test examples before training")
		//				.create());
		int flags;

		//modules
		flags = modules(allFlags);
		if(isOn(flags, USE_PROVER))
			options.addOption(
					OptionBuilder
					.withLongOpt(PROVER_MODULE_OPTION)
					.withArgName("class[:arg:...:arg]")
					.hasArg()
					.withDescription("Default: dpr\n"
							+ "Available options:\n"
							+ "ppr[:depth] (default depth=5)\n"
							+ "dpr[:eps[:alph[:strat]]] (default eps=1E-4, alph=0.1, strategy=throw(boost,adjust))\n"
							+ "df[:depth] (default depth=5)"
							+ "tr[:depth] (default depth=5)")
							.create());
		if (isOn(flags, USE_GROUNDER))
			options.addOption(
					OptionBuilder
					.withLongOpt(GROUNDER_MODULE_OPTION)
					.withArgName("class[:arg]")
					.hasArgs()
					.withValueSeparator(':')
					.withDescription("Default: g:3\n"
							+ "Available options:\n"
							+ "g[:threads[:throttle]] (default threads=3,throttle=-1)")
							.create());
		if (isOn(flags, USE_TRAINER))
			options.addOption(
					OptionBuilder
					.withLongOpt(TRAINER_MODULE_OPTION)
					.withArgName("class[:arg]")
					.hasArgs()
					.withValueSeparator(':')
					.withDescription("Default: t:3\n"
							+ "Available options:\n"
							+ "t[:threads[:throttle]] (default threads=3,throttle=-1)")
							.create());
		if (isOn(flags, USE_SRW))
			options.addOption(
					OptionBuilder
					.withLongOpt(SRW_MODULE_OPTION)
					.withArgName("class")
					.hasArgs()
					.withValueSeparator(':')
					.withDescription("Default: l2p (L2PosNegLossTrainedSRW)\n"
							+ "Default mu=.001\n"
							+ "Default eta=1.0\n"
							+ "Available options:\n"
							+ "l2p[:mu[:eta[:delta]]] (L2PosNegLossTrainedSRW)\n"
							+ "l2plocal[:mu[:eta[:delta]] (LocalL2PosNegLossTrainedSRW)\n"
							+ "apr[:mu[:eta[:delta[:eps[:alph]]]]] (AprSRW; default eps=1E-4, alph=0.1)")
							.create());
		//		options.addOption(
		//				OptionBuilder
		//				.withLongOpt("tester")
		//				.withArgName("class[:arg]")
		//				.hasArgs()
		//				.withValueSeparator(':')
		//				.withDescription("Default: mt\n"
		//						+ "Available options:\n"
		//						+ "t Tester\n"
		//						+ "mt[:threads] (default threads=3) MultithreadedTester\n"
		//						+ "rt RerankingTester")
		//						.create());
		if (isOn(flags, USE_SRW))
			options.addOption(
					OptionBuilder
					.withLongOpt(SEED_CONST_OPTION)
					.withArgName("s")
					.hasArg()
					.withDescription("Seed the SRW random number generator")
					.create());
		//		if (isOn(flags, USE_QUERYANSWERER)) {
		//			options.addOption(
		//					OptionBuilder
		//					.withLongOpt("queryAnswerer")
		//					.withArgName("class[:arg]")
		//					.hasArgs()
		//					.withValueSeparator(':')
		//					.withDescription("Runs QA after training. Default: none\n"
		//							+"Available options:\n"
		//							+"qa[:{norm|unnorm}] QueryAnswerer, normalized (default) or unnormalized\n"
		//							+"rqa[:{norm|unnorm}] RerankingQueryAnswerer")
		//							.create());
		//			options.addOption(
		//					OptionBuilder
		//					.withLongOpt("notest")
		//					.withDescription("Don't run Tester on test examples")
		//					.create());
		//			options.addOption(
		//					OptionBuilder
		//					.withLongOpt("solutions")
		//					.withArgName("file.txt")
		//					.hasArg()
		//					.withDescription("Save QA solutions here")
		//					.create());
		//		}
		//		if (isOn(flags, USE_MAXT)) {
		//			options.addOption(
		//					OptionBuilder
		//					.withLongOpt("maxT")
		//					.withArgName("<int>")
		//					.hasArg()
		//					.withDescription("Depth for SRW")
		//					.create());	
		//		}
	}

	private void seed(CommandLine line) {
		if (!line.hasOption(SEED_CONST_OPTION)) return;
		long seed = Long.parseLong(line.getOptionValue(SEED_CONST_OPTION));
		edu.cmu.ml.proppr.learn.SRW.seed(seed);
	}

	@Override
	protected void retrieveSettings(CommandLine line, int[] allFlags, Options options) throws IOException {
		super.retrieveSettings(line, allFlags, options);

		//		this.pretest=false;
		//		if (line.hasOption("pretest")) this.pretest = true;
		//		this.strict=false;
		//		if (line.hasOption("strict")) this.strict = true;

		//		if (isOn(flags,Configuration.USE_PROGRAMFILES)) {
		//			if (this.programFiles != null) 
		//				this.program = new LogicProgram(Component.loadComponents(programFiles, this.alpha, this));
		//			else if (!isOn(flags,Configuration.USE_DEFERREDPROGRAM)) missing(Configuration.USE_PROGRAMFILES, flags);
		//		}

		// TODO: There are likely other logic errors below for things that need a program if we've deferred it

		//		if (isOn(flags, USE_COMPLEX_FEATURES) && line.hasOption("complexFeatures")) {
		//			ComplexFeatureLibrary.init(this.program, this.complexFeatureConfigFile);
		//		}

		//		int threads = 3;
		//		if(line.hasOption("threads")) threads = this.nthreads;

		int flags;
		// modules
		flags = modules(allFlags);
		if (isOn(flags,USE_PROVER)) {
			if (!line.hasOption(PROVER_MODULE_OPTION)) {
				// default:
				this.prover = new DprProver();
			} else {
				String[] values = line.getOptionValue(PROVER_MODULE_OPTION).split(":");
				switch (PROVERS.valueOf(values[0])) {
				case ppr:
					if (values.length==1) {
						this.prover = new PprProver();
					} else {
						int depth = Integer.parseInt(values[1]);
						this.prover = new PprProver(depth);
					}
				case dpr:
					if (values.length==1)
						this.prover = new DprProver();
					else {
						double epsilon = Double.parseDouble(values[1]);
						double alpha = DprProver.MINALPH_DEFAULT;
						if (values.length>2) {
							alpha = Double.parseDouble(values[2]);
						}
						int strategy = DprProver.STRATEGY_DEFAULT;
						if (values.length>3) {
							if ("throw".equals(values[3])) strategy = DprProver.THROW_ALPHA_ERRORS;
							if ("boost".equals(values[3])) strategy = DprProver.BOOST_ALPHA;
							if ("adjust".equals(values[3])) strategy = DprProver.ADJUST_ALPHA;
						}
						this.prover = new DprProver(epsilon,alpha, strategy);
						alpha += epsilon;
					}
					break;
				case dfs:
					this.prover = new DfsProver();
					break;
				case tr:
					int depth = TracingDfsProver.DEFAULT_MAXDEPTH;
					if (values.length!=1) {
						depth = Integer.parseInt(values[1]);
					}
					this.prover = new TracingDfsProver(depth);
					break;
				default:
					usageOptions(options,allFlags,"No prover definition for '"+values[0]+"'");
				}
			}
		}

		if (anyOn(flags, USE_WEIGHTINGSCHEME | USE_PROVER | USE_SRW)) {
			if (!line.hasOption(WEIGHTINGSCHEME_MODULE_OPTION)) {
				// default:
				this.weightingScheme = SRW.DEFAULT_WEIGHTING_SCHEME();
			} else {
				switch(WEIGHTINGSCHEMES.valueOf(line.getOptionValue(WEIGHTINGSCHEME_MODULE_OPTION))) {
				case linear: weightingScheme = new LinearWeightingScheme(); break;
				case sigmoid: weightingScheme = new SigmoidWeightingScheme(); break;
				case tanh: weightingScheme = new TanhWeightingScheme(); break;
				case ReLU: weightingScheme = new ReLUWeightingScheme(); break;
				case exp: weightingScheme = new ExpWeightingScheme(); break;
				default: this.usageOptions(options, allFlags, "Unrecognized weighting scheme " + line.getOptionValue(WEIGHTINGSCHEME_MODULE_OPTION));
				}
			}
		}

		if (isOn(flags,Configuration.USE_GROUNDER)) {
			if (!line.hasOption(GROUNDER_MODULE_OPTION)) {
				this.grounder = new Grounder(nthreads,Multithreading.DEFAULT_THROTTLE,prover,program,plugins);
			} else {
				String[] values = line.getOptionValues(GROUNDER_MODULE_OPTION);
				int threads = nthreads;
				if (values.length>1) threads = Integer.parseInt(values[1]);
				int throttle = Multithreading.DEFAULT_THROTTLE;
				if (values.length>2) throttle = Integer.parseInt(values[2]);
				this.grounder = new Grounder(threads,throttle,prover,program,plugins);
			}
		}
		//		if (line.hasOption("cooker")) {
		//			String[] values = line.getOptionValues("cooker");
		//			if (values[0].equals("ec")) {
		//				this.grounder = new ExampleCooker(this.prover,this.program);
		//			} else {
		//				if (values.length > 1) threads = Integer.parseInt(values[1]);
		//				if (values[0].equals("mec")) {
		//					this.grounder = new MultithreadedExampleCooker(this.prover, this.program, threads);
		//				} else if (values[0].equals("mmc")) {
		//					this.grounder = new ModularMultiExampleCooker(this.prover, this.program, threads);
		//				}
		//			}
		//		} else this.grounder = new ModularMultiExampleCooker(this.prover, this.program, threads);

		//		this.trove=true;
		//		threads = 3;
		//		if(line.hasOption("threads")) threads = this.nthreads;
		//		if (line.hasOption("trainer")) {
		//			String[] values = line.getOptionValues("trainer");
		//			this.setupSRW(line, flags, options);
		//			seed(line);
		//			if (values.length > 1) {
		//				threads = Integer.parseInt(values[1]);
		//			}
		////			if (values[0].equals("t")) {
		////				this.trainer = new edu.cmu.ml.proppr.v1.Trainer<String>(
		////						(edu.cmu.ml.proppr.learn.SRW<PosNegRWExample<String>>) this.srw);
		////			} else if (values[0].equals("mt")) {
		////				this.trainer = new edu.cmu.ml.proppr.v1.MultithreadedTrainer<String>(
		////						(edu.cmu.ml.proppr.learn.SRW<PosNegRWExample<String>>) this.srw, threads);
		////			} else if (values[0].equals("mrr")) {
		////				this.trainer = new edu.cmu.ml.proppr.v1.MultithreadedRRTrainer<String>(
		////						(edu.cmu.ml.proppr.learn.SRW<PosNegRWExample<String>>) this.srw, threads);
		////			} else if (values[0].equals("trove.t")) {
		////				this.trainer = new Trainer( 
		////						(edu.cmu.ml.proppr.trove.learn.SRW<edu.cmu.ml.proppr.trove.learn.tools.PosNegRWExample>) this.srw);
		////			} else if (values[0].equals("trove.mt")) {
		////				this.trainer = new MultithreadedTrainer( 
		////						(edu.cmu.ml.proppr.trove.learn.SRW<edu.cmu.ml.proppr.trove.learn.tools.PosNegRWExample>) this.srw, threads);				
		////			} else if (values[0].equals("trove.mrr")) {
		////				this.trainer = new MultithreadedRRTrainer( 
		////						(edu.cmu.ml.proppr.trove.learn.SRW<edu.cmu.ml.proppr.trove.learn.tools.PosNegRWExample>) this.srw, threads);		
		////			} else if (values[0].equals("u")) {
		////				int throttle=Multithreading.DEFAULT_THROTTLE;
		////				if (values.length > 2) 
		////					throttle = Integer.parseInt(values[2]);
		////				this.trainer = new Trainer2((edu.cmu.ml.proppr.learn.SRW<PosNegRWExample<String>>) this.srw, threads, throttle);
		////			}
		//		} else {
		if (isOn(flags,USE_TRAIN)) {
			this.setupSRW(line, flags, options);
			seed(line);
			this.trainer = new Trainer(this.srw, this.nthreads, Multithreading.DEFAULT_THROTTLE);
		}
		//		}

		//		threads = 3;
		//		if(line.hasOption("threads")) threads = this.nthreads;
		//		if (isOn(flags,USE_NOTEST)) { // if NOTEST is available...
		//			if (line.hasOption("notest")) { // ..and the user has engaged it...
		//				this.tester = null; // don't use a Tester
		//			} else { // ...and we need to test,
		//				if (isOn(flags,USE_TEST) && !line.hasOption("test")) { // ...but the user hasn't specified a file...
		//					// give up
		//					usageOptions(options, flags,"Missing required option: one of\n\t--test <file>\n\t--notest");
		//				}
		//			}
		//		}
		//		if (!isOn(flags,USE_NOTEST) || !line.hasOption("notest")) {
		//			if (line.hasOption("tester")) {
		//				String[] values = line.getOptionValues("tester");
		//				if (values[0].equals("t")) {
		//					this.tester = new Tester(this.prover, this.program);
		//				} else {
		//					if (values.length > 1) threads = Integer.parseInt(values[1]);
		//					if (values[0].equals("mt")) {
		//						this.tester = new MultithreadedTester(this.prover, this.program, threads);
		//					} else if (values[0].endsWith("rt")) {
		//						this.trove = values[0].startsWith("trove.");
		//						if (this.srw == null) this.setupSRW(line,flags,options);
		//						if (this.trove) {
		//							this.tester = new edu.cmu.ml.proppr.trove.RerankingTester(
		//									this.prover, 
		//									this.program, 
		//									(edu.cmu.ml.proppr.trove.learn.SRW<edu.cmu.ml.proppr.trove.learn.tools.PosNegRWExample>) this.srw);
		//						} else {
		//							this.tester = new edu.cmu.ml.proppr.v1.RerankingTester(
		//									this.prover, 
		//									this.program, 
		//									(edu.cmu.ml.proppr.learn.SRW<PosNegRWExample<String>>) this.srw);
		//						}
		//					} else {
		//						this.usageOptions(options, flags,"No tester called '"+values[0]+"'");
		//					}
		//				}
		//			} else this.tester = new Tester(this.prover, this.program);
		//		}

		if (isOn(flags, USE_SRW) && this.srw==null) this.setupSRW(line,flags,options);

		//		this.normalize = true;
		//		if (line.hasOption("queryAnswerer")) {
		//			String[] values = line.getOptionValues("queryAnswerer");
		//			if (values[0].equals("qa"))
		//				this.queryAnswerer = new QueryAnswerer();
		//			else if (values[0].equals("rqa"))
		//				this.queryAnswerer = new RerankingQueryAnswerer((SRW<PosNegRWExample<String>>) this.srw);
		//			else {
		//				usageOptions(options, flags,"No queryAnswerer option '"+values[0]+"'");
		//			}
		//			if (values.length > 1) {
		//				this.normalize = values[1].equals("norm");
		//			}
		//
		//			if (line.hasOption("solutions")) {
		//				this.solutionsFile = line.getOptionValue("solutions");
		//			} else {
		//				usageOptions(options,flags,"Missing required option: solutions");
		//			}
		//		}
	}

	@Override
	protected void constructUsageSyntax(StringBuilder syntax, int[] allFlags) {
		super.constructUsageSyntax(syntax, allFlags);
		int flags;

		//modules
		flags = modules(allFlags);
		if (isOn(flags, USE_PROVER)) syntax.append(" [--").append(PROVER_MODULE_OPTION).append(" ppr[:depth] | dpr[:eps[:alph[:strat]]] | tr[:depth] ]");
		if (isOn(flags, USE_WEIGHTINGSCHEME)) 
			syntax.append(" [--").append(WEIGHTINGSCHEME_MODULE_OPTION).append(" linear | sigmoid | tanh | ReLU | exp]");
	}

	protected void setupSRW(CommandLine line, int flags, Options options) {
		SRWParameters sp = new SRWParameters();

		if (line.hasOption("maxT")) {
			sp.maxT = Integer.parseInt(line.getOptionValue("maxT"));
		}
		if (line.hasOption(SRW_MODULE_OPTION)) {
			String[] values = line.getOptionValues(SRW_MODULE_OPTION);

			boolean namedParameters = false;
			if (values.length > 1 && values[1].contains("=")) namedParameters = true;

			if (namedParameters) {
				for (int i=1; i<values.length; i++) {
					String[] parts = values[i].split("=");
					sp.set(parts);
				}
			} else {
				if (values.length > 1) {
					sp.mu = Double.parseDouble(values[1]);
				}
				if (values.length > 2) {
					sp.eta = Double.parseDouble(values[2]);
				}
				if (values.length > 3) {
					sp.delta = Double.parseDouble(values[3]);
				}
				if (values.length > 4) {
					sp.affinityFile = this.getExistingFile(values[4]);
				}                       
				if (values.length > 5) {
					sp.zeta = Double.parseDouble(values[5]);
				}
			}

			if (values[0].equals("l2p")) {
				this.srw = new edu.cmu.ml.proppr.learn.L2PosNegLossTrainedSRW(sp);
			} else if (values[0].equals("l1p")) {
				this.srw = new edu.cmu.ml.proppr.learn.L1PosNegLossTrainedSRW(sp);
			} else if (values[0].equals("l1plocal")) {
				this.srw = new edu.cmu.ml.proppr.learn.LocalL1PosNegLossTrainedSRW(sp);
			} else if (values[0].equals("l1plaplacianlocal")) {
				this.srw = new edu.cmu.ml.proppr.learn.LocalL1LaplacianPosNegLossTrainedSRW(sp);
			} else if (values[0].equals("l1plocalgrouplasso")) {
				this.srw = new edu.cmu.ml.proppr.learn.LocalL1GroupLassoPosNegLossTrainedSRW(sp);
			} else if (values[0].equals("l2plocal")) {
				this.srw = new edu.cmu.ml.proppr.learn.LocalL2PosNegLossTrainedSRW(sp);
			} else if (values[0].equals("apr")) {
				double epsilon = AprSRW.DEFAULT_EPSILON;
				if (values.length > 4) epsilon = Double.parseDouble(values[4]);
				this.srw = new edu.cmu.ml.proppr.learn.AprSRW(sp, epsilon, AprSRW.DEFAULT_STAYPROB);
			} else {
				usageOptions(options,-1,-1,-1,flags,"No srw definition for '"+values[0]+"'");
			}
		} else {
			this.srw = new edu.cmu.ml.proppr.learn.L2PosNegLossTrainedSRW(sp);
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(super.toString()).append(":\n");
		if (prover != null)   sb.append("  Prover: ").append(prover.getClass().getCanonicalName()).append("\n");
		if (grounder != null) sb.append("Grounder: ").append(grounder.getClass().getCanonicalName()).append("\n");
		if (srw != null)      sb.append("  Walker: ").append(srw.getClass().getCanonicalName()).append("\n");
		if (trainer != null)  sb.append(" Trainer: ").append(trainer.getClass().getCanonicalName()).append("\n");
		//sb.append("  Tester: ");
		//		if (tester != null) 
		//			sb.append(tester.getClass().getCanonicalName()).append("\n");
		//		else 
		//			sb.append("none\n");
		//		if (queryAnswerer != null) {
		//			sb.append("QueryAnswerer: ").append(queryAnswerer.getClass().getCanonicalName());
		//			sb.append(" (").append(this.normalize ? "normalized" : "unnormalized").append(")\n");
		//		}
		if (weightingScheme != null) sb.append("Weighting Scheme: ").append(weightingScheme.getClass().getCanonicalName()).append("\n");
		//		sb.append("Pretest? ").append(this.pretest ? "yes" : "no").append("\n");
		//		sb.append("Strict? ").append(this.strict ? "yes" : "no").append("\n");
		return sb.toString();
	}
}
