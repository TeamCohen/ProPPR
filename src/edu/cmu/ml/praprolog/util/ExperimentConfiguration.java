package edu.cmu.ml.praprolog.util;

import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import edu.cmu.ml.praprolog.ExampleCooker;
import edu.cmu.ml.praprolog.ModularMultiExampleCooker;
import edu.cmu.ml.praprolog.MultithreadedExampleCooker;
import edu.cmu.ml.praprolog.MultithreadedTester;
import edu.cmu.ml.praprolog.learn.LinearWeightingScheme;
import edu.cmu.ml.praprolog.learn.SRW;
import edu.cmu.ml.praprolog.learn.SigmoidWeightingScheme;
import edu.cmu.ml.praprolog.learn.TanhWeightingScheme;
import edu.cmu.ml.praprolog.learn.WeightingScheme;
import edu.cmu.ml.praprolog.prove.Component;
import edu.cmu.ml.praprolog.prove.LogicProgram;
import edu.cmu.ml.praprolog.trove.MultithreadedRRTrainer;
import edu.cmu.ml.praprolog.trove.MultithreadedTrainer;
import edu.cmu.ml.praprolog.Tester;
import edu.cmu.ml.praprolog.trove.Trainer;
import edu.cmu.ml.praprolog.trove.learn.L2PosNegLossTrainedSRW;

public class ExperimentConfiguration extends Configuration {
    public ExampleCooker cooker;
    public Object srw;
    public Object trainer;
    public Tester tester;
    public boolean trove;//=true;
    public boolean pretest;//=false;
    public boolean strict;//=false;
    public LogicProgram program;
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
                                         + "sigmoid")
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
			this.program = new LogicProgram(Component.loadComponents(programFiles, this.alpha));
		}
		
		int threads = 3;
		if(line.hasOption("threads")) threads = this.nthreads;
		if (line.hasOption("cooker")) {
			String[] values = line.getOptionValues("cooker");
			if (values[0].equals("ec")) {
				this.cooker = new ExampleCooker(this.prover,this.programFiles,this.alpha);
			} else {
				if (values.length > 1) threads = Integer.parseInt(values[1]);
				if (values[0].equals("mec")) {
					this.cooker = new MultithreadedExampleCooker(this.prover, this.programFiles, this.alpha, threads);
				} else if (values[0].equals("mmc")) {
					this.cooker = new ModularMultiExampleCooker(this.prover, this.programFiles, this.alpha, threads);
				}
			}
		} else this.cooker = new ModularMultiExampleCooker(this.prover, this.programFiles, this.alpha, threads);
		
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
		if (line.hasOption("tester")) {
			String[] values = line.getOptionValues("tester");
			if (values[0].equals("t")) {
				this.tester = new Tester(this.prover, this.cooker.getMasterProgram());
			} else {
				if (values.length > 1) threads = Integer.parseInt(values[1]);
				if (values[0].equals("mt")) {
					this.tester = new MultithreadedTester(this.prover, this.cooker.getMasterProgram(),threads);
				} else if (values[0].equals("rt")) {
					if (this.srw == null) this.setupSRW(line,flags,options);
					if (this.trove) {
						this.tester = new edu.cmu.ml.praprolog.trove.RerankingTester(
								this.prover, 
								this.cooker.getMasterProgram(), 
								(edu.cmu.ml.praprolog.trove.learn.L2PosNegLossTrainedSRW) this.srw);
					} else {
						this.tester = new edu.cmu.ml.praprolog.RerankingTester(
								this.prover, 
								this.cooker.getMasterProgram(), 
								(edu.cmu.ml.praprolog.learn.L2PosNegLossTrainedSRW<String>) this.srw);
					}
				} 
			}
		} else this.tester = new Tester(this.prover, this.cooker.getMasterProgram());
		
		if (isOn(flags, USE_SRW) && this.srw==null) this.setupSRW(line,flags,options);
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
		sb.append("Trainer: ").append(trainer.getClass().getCanonicalName()).append("\n");
		sb.append("Tester: ").append(tester.getClass().getCanonicalName()).append("\n");
		sb.append("Cooker: ").append(cooker.getClass().getCanonicalName()).append("\n");
		sb.append("Walker: ").append(srw.getClass().getCanonicalName()).append("\n");
		sb.append("Weighting Scheme: ").append(weightingScheme.getClass().getCanonicalName()).append("\n");
		sb.append("Pretest? ").append(this.pretest ? "yes" : "no").append("\n");
		sb.append("Strict? ").append(this.strict ? "yes" : "no").append("\n");
		return sb.toString();
	}
}
