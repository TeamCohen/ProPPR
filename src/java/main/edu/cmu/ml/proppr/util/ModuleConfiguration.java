package edu.cmu.ml.proppr.util;

import java.io.IOException;

import edu.cmu.ml.proppr.learn.*;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import edu.cmu.ml.proppr.AdaGradTrainer;
import edu.cmu.ml.proppr.CachingTrainer;
import edu.cmu.ml.proppr.Grounder;
import edu.cmu.ml.proppr.Trainer;
import edu.cmu.ml.proppr.learn.tools.ClippedExp;
import edu.cmu.ml.proppr.learn.tools.Exp;
import edu.cmu.ml.proppr.learn.tools.LReLU;
import edu.cmu.ml.proppr.learn.tools.Linear;
import edu.cmu.ml.proppr.learn.tools.ReLU;
import edu.cmu.ml.proppr.learn.tools.Sigmoid;
import edu.cmu.ml.proppr.learn.tools.SquashingFunction;
import edu.cmu.ml.proppr.learn.tools.StoppingCriterion;
import edu.cmu.ml.proppr.learn.tools.Tanh;
import edu.cmu.ml.proppr.prove.DfsProver;
import edu.cmu.ml.proppr.prove.DprProver;
import edu.cmu.ml.proppr.prove.IdDprProver;
import edu.cmu.ml.proppr.prove.IdPprProver;
import edu.cmu.ml.proppr.prove.PathDprProver;
import edu.cmu.ml.proppr.prove.PprProver;
import edu.cmu.ml.proppr.prove.PriorityQueueProver;
import edu.cmu.ml.proppr.prove.Prover;
import edu.cmu.ml.proppr.prove.TracingDfsProver;
import edu.cmu.ml.proppr.util.multithreading.Multithreading;

public class ModuleConfiguration extends Configuration {
	private static final String SEED_CONST_OPTION = "seed";
	private static final String SRW_MODULE_OPTION = "srw";
	private static final String TRAINER_MODULE_OPTION = "trainer";
	private static final String GROUNDER_MODULE_OPTION = "grounder";
	public  static final String SQUASHFUNCTION_MODULE_OPTION = "squashingFunction";
	private static final String OLD_SQUASHFUNCTION_MODULE_OPTION = "weightingScheme";
	private static final String PROVER_MODULE_OPTION = "prover";

	private enum PROVERS { ippr, ppr, qpr, idpr, dpr, pdpr, dfs, tr };
	private enum SQUASHFUNCTIONS { linear, sigmoid, tanh, ReLU, LReLU, exp, clipExp };
	private enum TRAINERS { cached, caching, streaming, adagrad };
	private enum SRWS { ppr, dpr, adagrad }
	private enum REGULARIZERS { l1, l1laplacian, l1grouplasso, l2 };
	private enum REGULARIZERSCHEDULES { synch, global, lazy, local };
	private enum LOSSFUNCTIONS { posneg, normpos, l2square };
	
	public Grounder<?> grounder;
	public SRW srw;
	public Trainer trainer;
	public SquashingFunction squashingFunction;
	public Prover<?> prover;
	public ModuleConfiguration(String[] args, int inputFiles, int outputFiles, int constants, int modules) {
		super(args,  inputFiles,  outputFiles,  constants,  modules);
	}

	@Override
	protected void addOptions(Options options, int[] allFlags) {
		super.addOptions(options, allFlags);
		int flags;

		//modules
		flags = modules(allFlags);
		if(isOn(flags, USE_SQUASHFUNCTION)) { 
			options.addOption(
					OptionBuilder
					.withLongOpt(SQUASHFUNCTION_MODULE_OPTION)
					.withArgName("w")
					.hasArg()
					.withDescription("Default: clipExp\n"
							+ "Available options:\n"
							+ "linear\n"
							+ "sigmoid\n"
							+ "tanh\n"
							+ "ReLU\n"
							+ "LReLU (leaky ReLU)\n"
							+ "exp\n"
							+ "clipExp (clipped to e*x @x=1)")
							.create());
		}
		if(isOn(flags, USE_PROVER))
			options.addOption(
					OptionBuilder
					.withLongOpt(PROVER_MODULE_OPTION)
					.withArgName("class[:arg:...:arg]")
					.hasArg()
					.withDescription("Default: dpr\n"
							+ "Available options:\n"
							+ "ippr\n"
							+ "ppr\n"
							+ "dpr\n"
							+ "idpr\n"
							+ "qpr\n"
							+ "pdpr\n"
							+ "dfs\n"
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
					.withArgName("class")
					.hasArgs()
					.withValueSeparator(':')
					.withDescription("Default: cached:shuff=true:pct=0.1:stableEpochs=3\n"
							+ "Available trainers:\n"
							+ "cached[:shuff={true|false}] (faster)\n"
							+ "streaming                   (large dataset)\n"
							+ "adagrad\n"
							+ "Available parameters:\n"
							+ "pct - stopping criterion max % improvement\n"
							+ "stableEpochs - stopping criterion")
							.create());
		if (isOn(flags, USE_SRW))
			options.addOption(
					OptionBuilder
					.withLongOpt(SRW_MODULE_OPTION)
					.withArgName("class")
					.hasArgs()
					.withValueSeparator(':')
					.withDescription("Default: ppr:reg=l2:sched=global:loss=posneg\n"
							 + "Syntax: srw:param=value:param=value...\n"
							 + "Available srws: ppr,dpr,adagrad\n"
							 + "Available [reg]ularizers: l1,l1laplacian,l1grouplasso,l2\n"
							 + "Available [sched]ules: global,local\n"
							 + "Available [loss] functions: posneg\n"
							 + "Other parameters:\n"
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
				case ippr:
					this.prover = new IdPprProver(apr);
					break;
				case ppr:
					this.prover = new PprProver(apr);
					break;
				case dpr:
					this.prover = new DprProver(apr);
					break;
				case idpr:
					this.prover = new IdDprProver(apr);
					break;
				case qpr:
					this.prover = new PriorityQueueProver(apr);
					break;
				case pdpr:
					this.prover = new PathDprProver(apr);
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

		if (anyOn(flags, USE_SQUASHFUNCTION | USE_PROVER | USE_SRW)) {
			if (!line.hasOption(SQUASHFUNCTION_MODULE_OPTION)) {
				// default:
				this.squashingFunction = SRW.DEFAULT_SQUASHING_FUNCTION();
			} else {
				switch(SQUASHFUNCTIONS.valueOf(line.getOptionValue(SQUASHFUNCTION_MODULE_OPTION))) {
				case linear: squashingFunction = new Linear(); break;
				case sigmoid: squashingFunction = new Sigmoid(); break;
				case tanh: squashingFunction = new Tanh(); break;
				case ReLU: squashingFunction = new ReLU(); break;
				case LReLU: squashingFunction = new LReLU(); break;
				case exp: squashingFunction = new Exp(); break;
				case clipExp: squashingFunction = new ClippedExp(); break;
				default: this.usageOptions(options, allFlags, "Unrecognized squashing function " + line.getOptionValue(SQUASHFUNCTION_MODULE_OPTION));
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
			if (isOn(flags,USE_TRAINER)) {
				// set default stopping criteria
				double percent = StoppingCriterion.DEFAULT_MAX_PCT_IMPROVEMENT;
				int stableEpochs = StoppingCriterion.DEFAULT_MIN_STABLE_EPOCHS;
				
				TRAINERS type = TRAINERS.cached;
				if (line.hasOption(TRAINER_MODULE_OPTION)) type = TRAINERS.valueOf(line.getOptionValues(TRAINER_MODULE_OPTION)[0]);
				switch(type) {
				case streaming: 
					this.trainer = new Trainer(this.srw, this.nthreads, this.throttle); 
					break;
				case caching: //fallthrough
				case cached:
					boolean shuff = CachingTrainer.DEFAULT_SHUFFLE;
					if (line.hasOption(TRAINER_MODULE_OPTION)) {
						for (String val : line.getOptionValues(TRAINER_MODULE_OPTION)) {
							if (val.startsWith("shuff")) shuff = Boolean.parseBoolean(val.substring(val.indexOf("=")+1));
						}
					}
					this.trainer = new CachingTrainer(this.srw, this.nthreads, this.throttle, shuff); 
					break;
				case adagrad:
					this.trainer = new AdaGradTrainer(this.srw, this.nthreads, this.throttle);
					if (this.squashingFunction instanceof ReLU)
						log.warn("AdaGrad performs quite poorly with --squashingFunction ReLU. For better results, switch to an exp variant.");
					stableEpochs = 2; // override default
					break;
				default: this.usageOptions(options, allFlags, "Unrecognized trainer "+line.getOptionValue(TRAINER_MODULE_OPTION));
				}
				
				// now get stopping criteria from command line
				if (line.hasOption(TRAINER_MODULE_OPTION)) {
					for (String val : line.getOptionValues(TRAINER_MODULE_OPTION)) {
						if (val.startsWith("pct")) percent = Double.parseDouble(val.substring(val.indexOf("=")+1));
						else if (val.startsWith("stableEpochs")) stableEpochs = Integer.parseInt(val.substring(val.indexOf("=")+1));
					}
				}
				this.trainer.setStoppingCriteria(stableEpochs, percent);
			}
		}

		if (isOn(flags, USE_SRW) && this.srw==null) this.setupSRW(line,flags,options);
	}

	@Override
	protected void constructUsageSyntax(StringBuilder syntax, int[] allFlags) {
		super.constructUsageSyntax(syntax, allFlags);
		int flags;

		//modules
		flags = modules(allFlags);
		if (isOn(flags, USE_PROVER)) syntax.append(" [--").append(PROVER_MODULE_OPTION).append(" ippr | ppr | dpr | pdpr | dfs | tr ]");
		if (isOn(flags, USE_SQUASHFUNCTION)) 
			syntax.append(" [--").append(SQUASHFUNCTION_MODULE_OPTION).append(" linear | sigmoid | tanh | ReLU | exp]");
		if (isOn(flags, USE_TRAINER)) syntax.append(" [--").append(TRAINER_MODULE_OPTION).append(" cached|streaming]");
	}

	protected void setupSRW(CommandLine line, int flags, Options options) {
		SRWOptions sp = new SRWOptions(apr, this.squashingFunction);

		if (line.hasOption(SRW_MODULE_OPTION)) {
			String[] values = line.getOptionValues(SRW_MODULE_OPTION);
			REGULARIZERS regularizerType = REGULARIZERS.l2;
			REGULARIZERSCHEDULES scheduleType = REGULARIZERSCHEDULES.synch;
			LOSSFUNCTIONS lossType = LOSSFUNCTIONS.posneg;
			boolean namedParameters = false;
			if (values.length > 1 && values[1].contains("=")) namedParameters = true;

			if (namedParameters) {
				for (int i=1; i<values.length; i++) {
					String[] parts = values[i].split("=");
					switch(parts[0]) {
					case "reg":
						regularizerType = REGULARIZERS.valueOf(parts[1]);
						break;
					case "sched":
						scheduleType = REGULARIZERSCHEDULES.valueOf(parts[1]);
						break;
					case "loss":
						lossType = LOSSFUNCTIONS.valueOf(parts[1]);
						break;
					default:
						sp.set(parts);
					}
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
			
			SRWS type = SRWS.valueOf(values[0]);
			switch(type) {
			case ppr:
				this.srw = new SRW(sp);
				break;
			case dpr:
				this.srw = new DprSRW(sp, DprSRW.DEFAULT_STAYPROB);
				break;
			case adagrad:
				this.srw = new AdaGradSRW(sp);
				break;
			default: usageOptions(options,-1,-1,-1,flags,"No srw definition for '"+values[0]+"'");
			}
			Regularize reg = null;
			switch(regularizerType) {
			case l1:
				reg = new RegularizeL1();
				break;
			case l1laplacian:
				reg = new RegularizeL1Laplacian();
				break;
			case l1grouplasso:
				reg = new RegularizeL1GroupLasso();
				break;
			case l2:
				reg = new RegularizeL2();
				break;
			}
			switch(scheduleType) {
			case global:
			case synch: // fallthrough
				this.srw.setRegularizer(new RegularizationSchedule(this.srw, reg));
				break;
			case local:
			case lazy: // fallthrough
				this.srw.setRegularizer(new LocalRegularizationSchedule(this.srw, reg));
				break;
			}
			switch(lossType) {
			case posneg:
				this.srw.setLossFunction(new PosNegLoss());
				break;
			case normpos:
				this.srw.setLossFunction(new NormalizedPosLoss());
				break;
			case l2square:
<<<<<<< HEAD
				this.srw.setLossFunction(new PairwiseL2SqLoss());
=======
				this.srw.setLossFunction(new L2SqLoss());
>>>>>>> a312e4a5be3efe1d3ac5240a237c43f3e437d726
				break;
			}
		} else {
			this.srw = new SRW(sp);
			this.srw.setRegularizer(new RegularizationSchedule(this.srw, new RegularizeL2()));
		}
	}

	@Override
	public String toString() {
		String superString = super.toString();
		if (superString==null) superString = "unknownConfigClass";
		StringBuilder sb = new StringBuilder(superString).append("\n");
		if (trainer != null)
			sb.append(String.format(FORMAT_STRING, "Trainer")).append(": ").append(trainer.getClass().getCanonicalName()).append("\n");
		if (prover != null)
			sb.append(String.format(FORMAT_STRING, "Prover")).append(": ").append(prover.getClass().getCanonicalName()).append("\n");
		if (srw != null)
			sb.append(String.format(FORMAT_STRING, "Walker")).append(": ").append(srw.getClass().getCanonicalName()).append("\n");
		if (squashingFunction != null)
			sb.append(String.format(FORMAT_STRING, "Squashing function")).append(": ").append(squashingFunction.getClass().getCanonicalName()).append("\n");
		sb.append(String.format(FORMAT_STRING, "APR Alpha")).append(": ").append(apr.alpha).append("\n");
		sb.append(String.format(FORMAT_STRING, "APR Epsilon")).append(": ").append(apr.epsilon).append("\n");
		sb.append(String.format(FORMAT_STRING, "APR Depth")).append(": ").append(apr.maxDepth).append("\n");
		return sb.toString();
	}
}
