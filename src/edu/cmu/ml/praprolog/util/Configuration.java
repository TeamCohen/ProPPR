package edu.cmu.ml.praprolog.util;

import java.io.File;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import edu.cmu.ml.praprolog.prove.Component;
import edu.cmu.ml.praprolog.prove.DprProver;
import edu.cmu.ml.praprolog.prove.PprProver;
import edu.cmu.ml.praprolog.prove.Prover;
import edu.cmu.ml.praprolog.prove.TracingDfsProver;

public class Configuration {
	public static final int USE_PROGRAMFILES=1;
	public static final int USE_DATA=2;
	public static final int USE_OUTPUT=4;
	public static final int USE_PROVER=8;
	public static final int USE_THREADS=0x10;
	public static final int USE_TRAIN=0x20;
	public static final int USE_TEST=0x40;
	public static final int USE_TRAINTEST=0x260;
	public static final int USE_LEARNINGSET=0x80;
	public static final int USE_QUERIES=0x100;
	public static final int USE_PARAMS = 0x200;
	public static final int USE_SRW = 0x400;
	/** programFiles, prover, threads **/
	public static final int USE_DEFAULTS=0x19;
	
	public Prover prover=null;
	public String[] programFiles=null;
	public File dataFile=null;
	public File queryFile=null;
	public File testFile=null;
	public String outputFile=null;
	public int nthreads=-1;
	public double alpha = Component.ALPHA_DEFAULT;
	public int epochs=5;
	public boolean traceLosses=false;
	public String paramsFile=null;

	static boolean isOn(int flags, int flag) {
		return (flags & flag) == flag;
	}
	
	public Configuration(String[] args) { this(args, new PprProver()); }
	public Configuration(String[] args, int flags) { this(args,new PprProver(), flags); }
	public Configuration(String[] args, Prover dflt) { this(args, dflt, USE_DEFAULTS); }

	public Configuration(String[] args, Prover dflt, int flags) {
		if (isOn(flags,USE_DATA) && isOn(flags,USE_TRAINTEST)) {
			throw new IllegalArgumentException("Programmer error: Illegal to request --data and also --train/--test");
		}
		
		Options options = new Options();
		this.prover = dflt;
		addOptions(options, flags);

		try {
			CommandLineParser parser = new BasicParser();

		    // parse the command line arguments
		    CommandLine line = parser.parse( options, args );
		    retrieveSettings(line,flags,options);
			
		} catch( Exception exp ) {
			System.err.println("\n"+exp.getMessage()+"\n");
			usageOptions(options,flags);
		}
	}
	protected File getExistingFileOption(CommandLine line, String name) {
		File value = new File(line.getOptionValue(name));
		if (!value.exists()) throw new IllegalArgumentException("File '"+value.getName()+"' must exist");
		return value;
	}
	protected void retrieveSettings(CommandLine line, int flags, Options options) {
		if (isOn(flags,USE_PROGRAMFILES) && line.hasOption("programFiles"))  this.programFiles = line.getOptionValues("programFiles");
		if (isOn(flags,USE_DATA) && line.hasOption("data"))                  this.dataFile = getExistingFileOption(line,"data");
		if (isOn(flags,USE_QUERIES) && line.hasOption("queries"))            this.queryFile = getExistingFileOption(line,"queries");
		if ((isOn(flags,USE_OUTPUT) || isOn(flags,USE_TRAIN)) 
				&& line.hasOption("output"))                                 this.outputFile = line.getOptionValue("output");
		if (isOn(flags,USE_THREADS) && line.hasOption("threads"))            this.nthreads = Integer.parseInt(line.getOptionValue("threads"));
		if (isOn(flags,USE_LEARNINGSET) && line.hasOption("epochs"))         this.epochs = Integer.parseInt(line.getOptionValue("epochs"));
		if (isOn(flags,USE_LEARNINGSET) && line.hasOption("traceLosses"))    this.traceLosses = true;
		if (isOn(flags,USE_TEST) && line.hasOption("test"))                  this.testFile = getExistingFileOption(line,"test");
		if (isOn(flags,USE_TRAIN) && line.hasOption("train"))                this.dataFile = getExistingFileOption(line,"train");
		if (isOn(flags,USE_PARAMS) && line.hasOption("params"))              this.paramsFile = line.getOptionValue("params");
		if (isOn(flags,USE_PROVER) && line.hasOption("prover")) {
			String[] values = line.getOptionValues("prover");
			if(values[0].startsWith("ppr")) {
				if (values.length==1) {
					this.prover = new PprProver();
				} else {
					int depth = Integer.parseInt(values[1]);
					this.prover = new PprProver(depth);
				}
			} else if (values[0].startsWith("dpr")) {
				if (values.length==1)
					this.prover = new DprProver();
				else {
					double epsilon = Double.parseDouble(values[1]);
					this.alpha = DprProver.MINALPH_DEFAULT;
					if (values.length>2) {
						this.alpha = Double.parseDouble(values[2]);
					}
					this.prover = new DprProver(epsilon,this.alpha);
					this.alpha += epsilon;
				}
			} else if(values[0].startsWith("tr")) {
				int depth = TracingDfsProver.DEFAULT_MAXDEPTH;
				if (values.length!=1) {
					depth = Integer.parseInt(values[1]);
				}
				this.prover = new TracingDfsProver(depth);
			}else {
				System.err.println("No prover definition for '"+values[0]+"'");
			    usageOptions(options,flags);
			}
		}
	}
	protected void addOptions(Options options, int flags) {
		options.addOption(
				OptionBuilder
					.withLongOpt("programFiles")
					.isRequired(isOn(flags,USE_PROGRAMFILES))
					.withArgName("file:...:file")
					.hasArgs()
					.withValueSeparator(':')
					.withDescription("Description of the logic program. Formats:\n\t\tcrules:goal,, & ... & goal,, # feature,, # variable,,\n\t\tcfacts:f\\ta\\ta")
					.create());
		if (!isOn(flags,USE_TRAINTEST)) options.addOption(
				OptionBuilder
					.withLongOpt("data")
					.isRequired(isOn(flags,USE_DATA))
					.withArgName("file")
					.hasArg()
					.withDescription("Examples. Format: f a a\\t{+|-}f a a\\t...")
					.create());
		options.addOption(
				OptionBuilder
					.withLongOpt("output")
					.isRequired(isOn(flags,USE_OUTPUT | USE_TRAIN))
					.withArgName("file")
					.hasArg()
					.withDescription("Cooked training examples. Format: query\\tkeys,,\\tposList,,\\tnegList,,\\tgraph")
					.create());
		options.addOption(
				OptionBuilder
					.withLongOpt("queries")
				        .isRequired(isOn(flags,USE_QUERIES))
					.withArgName("file")
					.hasArg()
				        .withDescription("Queries.  Format f a a")
					.create());
		options.addOption(
				OptionBuilder
					.withLongOpt("prover")
					.withArgName("class[:arg:...:arg]")
					.hasArgs()
					.withValueSeparator(':')
					.withDescription("Default: "+this.prover.getClass().getSimpleName()+"\n"
							+"Available options:\n"
							+"ppr[:depth] (default depth=5)\n"
							+"dpr[:eps[:alph]] (default eps=1E-4, alph=0.1)\n"
							+"tr[:depth] (default depth=5)")
					.create());
		if (isOn(flags,USE_THREADS)) options.addOption(
				OptionBuilder
					.withLongOpt("threads")
					.withArgName("integer")
					.hasArg()
					.withDescription("Activate multithreading with x worker threads.")
					.create());
		if (isOn(flags,USE_TRAIN))
			options.addOption(
					OptionBuilder
						.withLongOpt("train")
						.isRequired()
						.withArgName("file")
						.hasArg()
						.withDescription("Training examples. Format: f a a\\t{+|-}f a a\\t...")
						.create());
		if (isOn(flags,USE_TEST))
			options.addOption(
					OptionBuilder
						.withLongOpt("test")
						.isRequired()
						.withArgName("file")
						.hasArg()
						.withDescription("Testing examples. Format: f a a\\t{+|-}f a a\\t...")
						.create());	
		if (isOn(flags,USE_PARAMS))
			options.addOption(
					OptionBuilder
						.withLongOpt("params")
						.withArgName("file")
						.hasArg()
						.withDescription("Save/load learned walker parameters.")
						.create());
		if (isOn(flags,USE_LEARNINGSET)) {
			options.addOption(
				OptionBuilder
					.withLongOpt("epochs")
					.withArgName("integer")
					.hasArg()
					.withDescription("For training model parameters (default = 5)")
					.create());
			options.addOption(
					OptionBuilder
						.withLongOpt("traceLosses")
						.withDescription("Print training loss at each epoch")
						.create());
		}
	}
	protected void constructUsageSyntax(StringBuilder syntax, int flags) {
		if (isOn(flags,USE_PROGRAMFILES)) syntax.append(" --programFiles file.crules:file.cfacts:file.graph");
		if (isOn(flags,USE_DATA)) syntax.append(" --data training.data");
		if (isOn(flags,USE_OUTPUT)) syntax.append(" --output training.cooked");
		if (isOn(flags,USE_PROVER)) syntax.append(" [--prover { ppr[:depth] | dpr[:eps[:alph]] | tr[:depth] }]");
		if (isOn(flags,USE_TRAIN)) syntax.append(" --train training.data");
		if (isOn(flags,USE_TEST)) syntax.append(" --test testing.data");
		if (isOn(flags,USE_PARAMS)) syntax.append("  [--params params.txt]");
		if (isOn(flags,USE_LEARNINGSET)) syntax.append(" [--epochs <int>] [--traceLosses]");
	}
	protected void usageOptions(Options options,int flags) {
		HelpFormatter formatter = new HelpFormatter();
		int width = 80;

		String swidth = System.getenv("COLUMNS");
		if (swidth != null) {
			try {
				width = Integer.parseInt(swidth);
			} catch (NumberFormatException e) {}
		}
		formatter.setWidth(width);
		formatter.setLeftPadding(0);
		formatter.setDescPadding(2);
		StringBuilder syntax = new StringBuilder();
		constructUsageSyntax(syntax, flags);
		formatter.printHelp(syntax.toString(), options );
		System.exit(0);
	}
	@Override
	public String toString() {
		return this.getClass().getCanonicalName();
	}
}
