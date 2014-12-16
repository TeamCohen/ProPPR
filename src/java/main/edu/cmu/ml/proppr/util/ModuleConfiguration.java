package edu.cmu.ml.proppr.util;

import java.io.IOException;

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
	public Grounder grounder;
	public SRW srw;
	public Trainer trainer;
	public WeightingScheme weightingScheme;
	public Prover prover;
	public ModuleConfiguration(String[] args, int inputFiles, int outputFiles, int constants, int modules) {
		super(args,  inputFiles,  outputFiles,  constants,  modules);
	}

	@Override
	protected void addOptions(Options options, int[] allFlags) {
		super.addOptions(options, allFlags);
		int flags;

		//modules
		flags = modules(allFlags);
		if(isOn(flags, USE_WEIGHTINGSCHEME))
			options.addOption(
					OptionBuilder
					.withLongOpt(WEIGHTINGSCHEME_MODULE_OPTION)
					.withArgName("w")
					.hasArg()
					.withDescription("Default: ReLU\n"
							+ "Available options:\n"
							+ "linear\n"
							+ "sigmoid\n"
							+ "tanh\n"
							+ "ReLU\n"
							+ "exp")
							.create());
		if(isOn(flags, USE_PROVER))
			options.addOption(
					OptionBuilder
					.withLongOpt(PROVER_MODULE_OPTION)
					.withArgName("class[:arg:...:arg]")
					.hasArg()
					.withDescription("Default: dpr\n"
							+ "Available options:\n"
							+ "ppr\n"
							+ "dpr[:strat] (default strategy=boost(exception,adjust))\n"
							+ "df"
							+ "tr")
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
							 + "Syntax: srw:param=value:param=value...\n"
							 + "Available srw options:\n"
							 + "l1p, l1plocal, l1laplacianplocal, l1pgrouplassoplocal\n"
							 + "l2p, l2plocal\n"
							 + "apr\n"
							 + "Available parameters:\n"
							 + "mu,eta,delta,zeta,affinityFile\n"
							+ "Default mu=.001\n"
							+ "Default eta=1.0")
							.create());
		if (isOn(flags, USE_SRW))
			options.addOption(
					OptionBuilder
					.withLongOpt(SEED_CONST_OPTION)
					.withArgName("s")
					.hasArg()
					.withDescription("Seed the SRW random number generator")
					.create());
	}

	private void seed(CommandLine line) {
		if (!line.hasOption(SEED_CONST_OPTION)) return;
		long seed = Long.parseLong(line.getOptionValue(SEED_CONST_OPTION));
		edu.cmu.ml.proppr.learn.SRW.seed(seed);
	}

	@Override
	protected void retrieveSettings(CommandLine line, int[] allFlags, Options options) throws IOException {
		super.retrieveSettings(line, allFlags, options);

		int flags;
		// modules
		flags = modules(allFlags);
		if (isOn(flags,USE_PROVER)) {
			if (!line.hasOption(PROVER_MODULE_OPTION)) {
				// default:
				this.prover = new DprProver(apr);
			} else {
				String[] values = line.getOptionValue(PROVER_MODULE_OPTION).split(":");
				switch (PROVERS.valueOf(values[0])) {
				case ppr:
					this.prover = new PprProver(apr);
				case dpr:
					if (values.length==1)
						this.prover = new DprProver(apr);
					else {
						APROptions.ALPHA_STRATEGY strategy = APROptions.ALPHA_STRATEGY_DEFAULT;
						if ("throw".equals(values[1])) strategy = APROptions.ALPHA_STRATEGY.exception;
						else strategy = APROptions.ALPHA_STRATEGY.valueOf(values[1]);
						apr.alphaErrorStrategy = strategy;
						this.prover = new DprProver(apr);
					}
					break;
				case dfs:
					this.prover = new DfsProver(apr);
					break;
				case tr:
					this.prover = new TracingDfsProver(apr);
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
				this.grounder = new Grounder(nthreads,Multithreading.DEFAULT_THROTTLE,apr,prover,program,plugins);
			} else {
				String[] values = line.getOptionValues(GROUNDER_MODULE_OPTION);
				int threads = nthreads;
				if (values.length>1) threads = Integer.parseInt(values[1]);
				int throttle = Multithreading.DEFAULT_THROTTLE;
				if (values.length>2) throttle = Integer.parseInt(values[2]);
				this.grounder = new Grounder(threads,throttle,apr,prover,program,plugins);
			}
		}
		if (isOn(flags,USE_TRAIN)) {
			this.setupSRW(line, flags, options);
			seed(line);
			this.trainer = new Trainer(this.srw, this.nthreads, Multithreading.DEFAULT_THROTTLE);
		}

		if (isOn(flags, USE_SRW) && this.srw==null) this.setupSRW(line,flags,options);
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
		SRWOptions sp = new SRWOptions(apr);

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
				this.srw = new edu.cmu.ml.proppr.learn.AprSRW(sp, AprSRW.DEFAULT_STAYPROB);
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
		if (prover != null)   sb.append("   Prover: ").append(prover.getClass().getCanonicalName()).append("\n");
//		if (grounder != null) sb.append(" Grounder: ").append(grounder.getClass().getCanonicalName()).append("\n");
		if (srw != null)      sb.append("   Walker: ").append(srw.getClass().getCanonicalName()).append("\n");
//		if (trainer != null)  sb.append("  Trainer: ").append(trainer.getClass().getCanonicalName()).append("\n");
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
		sb.append("    Alpha: ").append(apr.alpha).append("\n");
		sb.append("  Epsilon: ").append(apr.epsilon).append("\n");
		sb.append("Max depth: ").append(apr.maxDepth).append("\n");
		sb.append(" Strategy: ").append(apr.alphaErrorStrategy.name()).append("\n");
		return sb.toString();
	}
}
