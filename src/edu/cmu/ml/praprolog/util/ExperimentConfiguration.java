package edu.cmu.ml.praprolog.util;

import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import edu.cmu.ml.praprolog.ExampleCooker;
import edu.cmu.ml.praprolog.ModularMultiExampleCooker;
import edu.cmu.ml.praprolog.MultithreadedExampleCooker;
import edu.cmu.ml.praprolog.MultithreadedTester;
import edu.cmu.ml.praprolog.learn.SRW;
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
	
	public ExperimentConfiguration(String[] args, int flags) {
		super(args, flags);
	}
	
	@Override 
	protected void addOptions(Options options, int flags) {
		super.addOptions(options,flags);
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
						+"Available options:\n"
						+"ec\n"
						+"mec[:threads] (default threads=3)\n"
						+"mmc[:threads] (default threads=3)")
					.create());
		options.addOption(
				OptionBuilder
					.withLongOpt("trainer")
					.withArgName("class[:arg]")
					.hasArgs()
					.withValueSeparator(':')
					.withDescription("Default: trove.mrr:3\n"
							+"Available options:\n"
							+"t\n"
							+"mt[:threads] (default threads=3)\n"
							+"mrr[:threads] (default threads=3)\n"
							+"trove.t\n"
							+"trove.mt[:threads] (default threads=3)\n"
							+"trove.mrr[:threads] (default threads=3)")
					.create());
		options.addOption(
				OptionBuilder
					.withLongOpt("tester")
					.withArgName("class[:arg]")
					.hasArgs()
					.withValueSeparator(':')
					.withDescription("Default: t\n"
						+"Available options:\n"
						+"t\n"
						+"mt[:threads] (default threads=3)")
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
				this.srw = new edu.cmu.ml.praprolog.trove.learn.L2PosNegLossTrainedSRW();
				troveSeed(line);
			} else {
				this.trove = false;
				this.srw = new edu.cmu.ml.praprolog.learn.L2PosNegLossTrainedSRW<String>();
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
			this.srw = new edu.cmu.ml.praprolog.trove.learn.L2PosNegLossTrainedSRW();
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
				}
			}
		} else this.tester = new Tester(this.prover, this.cooker.getMasterProgram());
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(super.toString()).append(":\n");
		sb.append("Trainer: ").append(trainer.getClass().getCanonicalName()).append("\n");
		sb.append("Tester: ").append(tester.getClass().getCanonicalName()).append("\n");
		sb.append("Cooker: ").append(cooker.getClass().getCanonicalName()).append("\n");
		sb.append("Walker: ").append(srw.getClass().getCanonicalName()).append("\n");
		sb.append("Pretest? ").append(this.pretest ? "yes" : "no").append("\n");
		sb.append("Strict? ").append(this.strict ? "yes" : "no").append("\n");
		return sb.toString();
	}
}
