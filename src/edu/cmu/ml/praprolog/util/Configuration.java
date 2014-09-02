package edu.cmu.ml.praprolog.util;

import java.util.Properties;
import java.util.Set;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PermissiveParser;

import edu.cmu.ml.praprolog.learn.tools.LinearWeightingScheme;
import edu.cmu.ml.praprolog.learn.tools.ReLUWeightingScheme;
import edu.cmu.ml.praprolog.learn.tools.SigmoidWeightingScheme;
import edu.cmu.ml.praprolog.learn.tools.TanhWeightingScheme;
import edu.cmu.ml.praprolog.learn.tools.ExpWeightingScheme;
import edu.cmu.ml.praprolog.learn.tools.WeightingScheme;
import edu.cmu.ml.praprolog.prove.*;
import org.apache.commons.cli.*;

import java.io.*;
import java.util.Properties;

public class Configuration {
    public static final int USE_PROGRAMFILES = 1;
    public static final int USE_DATA = 2;
    public static final int USE_OUTPUT = 4;
    public static final int USE_PROVER = 8;
    public static final int USE_THREADS = 0x10;
    public static final int USE_TRAIN = 0x20;
    public static final int USE_TEST = 0x40;
    /** epochs, traceLosses */
    public static final int USE_LEARNINGSET = 0x80;
    /** */
    public static final int USE_QUERIES = 0x100;
    public static final int USE_PARAMS = 0x200;
    public static final int USE_SRW = 0x400;
    public static final int USE_COMPLEX_FEATURES = 0x800;
    public static final int USE_NOTEST = 0x1000;
    public static final int USE_SOLUTIONS = 0x2000;
    public static final int USE_DEFERREDPROGRAM = 0x4000;
    // combo flags:
    /** programFiles, prover, threads **/
    public static final int USE_DEFAULTS = 0x19;
    /** train, test, params **/
    public static final int USE_TRAINTEST = 0x260;
    public static final String PROPFILE = "config.properties";
    private static final boolean DEFAULT_COMBINE = true;
    public Prover prover = null;
    public String[] programFiles = null;
    public File dataFile = null;
    public File queryFile = null;
    public File testFile = null;
    public File complexFeatureConfigFile = null;
    public String outputFile = null;
    public int nthreads = -1;
    public double alpha = Component.ALPHA_DEFAULT;
    public int epochs = 5;
    public boolean traceLosses = false;
    public String paramsFile = null;
    public WeightingScheme weightingScheme = null;
    public boolean force = false;
	public Boolean ternaryIndex = null;

	static boolean isOn(int flags, int flag) {
		return (flags & flag) == flag;
	}	
	static boolean anyOn(int flags, int flag) {
		return (flags & flag) > 0;
	}
	
	private Configuration() {}
	public Configuration(String[] args) { this(args, new DprProver()); }
	public Configuration(String[] args, int flags) { this(args, new DprProver(), flags, DEFAULT_COMBINE); }
	public Configuration(String[] args, Prover dflt) { this(args, dflt, USE_DEFAULTS, DEFAULT_COMBINE); }
	public Configuration(String[] args, Prover dflt, int flags) { this(args,dflt,flags,DEFAULT_COMBINE); }
	public Configuration(String[] args, Prover dflt, int flags, boolean combine) {
		if (isOn(flags,USE_DATA) && isOn(flags,USE_TRAINTEST)) {
			throw new IllegalArgumentException("Programmer error: Illegal to request --data and also --train/--test");
		}
		
		Options options = new Options();
		this.prover = dflt;
		addOptions(options, flags);

		try {
			PermissiveParser parser = new PermissiveParser(true);
			
			// if the user specified a properties file, add those values at the end
			// (so that command line args override them)
			if(combine) args = combinedArgs(args);
			
		    // parse the command line arguments
		    CommandLine line = parser.parse( options, args );
		    if (parser.hasUnrecognizedOptions()) {
		    	System.err.println("WARNING: unrecognized options detected:");
		    	for (String opt : parser.getUnrecognizedOptions()) { System.err.println("\t"+opt); }
		    }
		    retrieveSettings(line,flags,options);
			
		} catch( Exception exp ) {
			StringWriter sw = new StringWriter();
			exp.printStackTrace(new PrintWriter(sw));
			usageOptions(options,flags,exp.getMessage()+"\n"+sw.toString());
		}
	}
	protected File getExistingFileOption(CommandLine line, String name) {
		File value = new File(line.getOptionValue(name));
		if (!value.exists()) throw new IllegalArgumentException("File '"+value.getName()+"' must exist");
		return value;
	}
	protected void retrieveSettings(CommandLine line, int flags, Options options) {
		if (isOn(flags,USE_PROGRAMFILES) && line.hasOption("programFiles"))  this.programFiles = line.getOptionValues("programFiles");
		if (isOn(flags,USE_PROGRAMFILES) && line.hasOption("ternaryIndex"))  this.ternaryIndex = Boolean.parseBoolean(line.getOptionValue("ternaryIndex"));
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
			String[] values = line.getOptionValue("prover").split(":");
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
					int strategy = DprProver.STRATEGY_DEFAULT;
					if (values.length>3) {
						if ("throw".equals(values[3])) strategy = DprProver.THROW_ALPHA_ERRORS;
						if ("boost".equals(values[3])) strategy = DprProver.BOOST_ALPHA;
						if ("adjust".equals(values[3])) strategy = DprProver.ADJUST_ALPHA;
					}
					this.prover = new DprProver(epsilon,this.alpha, strategy);
					this.alpha += epsilon;
				}
			} else if(values[0].startsWith("tr")) {
				int depth = TracingDfsProver.DEFAULT_MAXDEPTH;
				if (values.length!=1) {
					depth = Integer.parseInt(values[1]);
				}
				this.prover = new TracingDfsProver(depth);
			}else {
			    usageOptions(options,flags,"No prover definition for '"+values[0]+"'");
			}
		}

        if (anyOn(flags, USE_PROGRAMFILES | USE_PROVER)) {
            if (this.weightingScheme == null) this.weightingScheme = new TanhWeightingScheme();
            if (line.hasOption("weightingScheme")) {
                String value = line.getOptionValue("weightingScheme");
                if (value.equals("linear")) weightingScheme = new LinearWeightingScheme();
                else if (value.equals("sigmoid")) weightingScheme = new SigmoidWeightingScheme();
                else if (value.equals("tanh")) weightingScheme = new TanhWeightingScheme();
                else if (value.equals("ReLU")) weightingScheme = new ReLUWeightingScheme();
                else if (value.equals("exp")) weightingScheme = new ExpWeightingScheme();
                else {
                    this.usageOptions(options, flags, "Unrecognized weighting scheme " + value);
                }
            }
        }
        
        if (line.hasOption("force")) this.force = true;
    }

    /**
     * For all option flags as specified in this file, addOptions creates
     * and adds Option objects to the Options object.
     */
    protected void addOptions(Options options, int flags) {
    	if (isOn(flags, USE_PROGRAMFILES)) {
	        options.addOption(
	                OptionBuilder
	                        .withLongOpt("programFiles")
	                        .withArgName("file:...:file")
	                        .hasArgs()
	                        .withValueSeparator(':')
	                        .withDescription("Description of the logic program. Formats:\n\t\tcrules:goal,, & ... & goal,, # feature,, # variable,,\n\t\tcfacts:f\\ta\\ta")
	                        .create());
	        options.addOption(
	        		OptionBuilder
	        				.withLongOpt("ternaryIndex")
	        				.withArgName("true|false")
	        				.hasArg()
	        				.withDescription("Turn A1A2 index on/off in GoalComponent (default off/false)")
	        				.create());
    	}
        if (!isOn(flags, USE_TRAINTEST))
            options.addOption(
                    OptionBuilder
                            .withLongOpt("data")
                            .isRequired(isOn(flags, USE_DATA))
                            .withArgName("file")
                            .hasArg()
                            .withDescription("Examples. Format: f a a\\t{+|-}f a a\\t...")
                            .create());
        options.addOption(
                OptionBuilder
                        .withLongOpt("output")
                        .isRequired(isOn(flags, USE_OUTPUT | USE_TRAIN))
                        .withArgName("file")
                        .hasArg()
                        .withDescription("Cooked training examples. Format: query\\tkeys,,\\tposList,,\\tnegList,,\\tgraph")
                        .create());
        if(isOn(flags, USE_QUERIES))
	        options.addOption(
	                OptionBuilder
	                        .withLongOpt("queries")
	                        .withArgName("file")
	                        .hasArg()
	                        .withDescription("Queries. Format f a a")
	                        .create());
        if(isOn(flags, USE_PROVER))
	        options.addOption(
		                OptionBuilder
		                        .withLongOpt("prover")
		                        .withArgName("class[:arg:...:arg]")
		                        .hasArg()
		                        .withDescription("Default: " + this.prover.getClass().getSimpleName() + "\n"
		                                         + "Available options:\n"
		                                         + "ppr[:depth] (default depth=5)\n"
		                                         + "dpr[:eps[:alph[:strat]]] (default eps=1E-4, alph=0.1, strategy=throw(boost,adjust))\n"
		                                         + "tr[:depth] (default depth=5)")
		                        .create());
        if (isOn(flags, USE_THREADS)) options.addOption(
                OptionBuilder
                        .withLongOpt("threads")
                        .withArgName("integer")
                        .hasArg()
                        .withDescription("Activate multithreading with x worker threads.")
                        .create());
        if (isOn(flags, USE_TRAIN))
            options.addOption(
                    OptionBuilder
                            .withLongOpt("train")
                            .isRequired()
                            .withArgName("file")
                            .hasArg()
                            .withDescription("Training examples. Format: f a a\\t{+|-}f a a\\t...")
                            .create());
        if (isOn(flags, USE_TEST)) {
            options.addOption(
            		OptionBuilder
			            .withLongOpt("test")
			            .isRequired(!isOn(flags,USE_NOTEST))
			            .withArgName("file")
			            .hasArg()
			            .withDescription("Testing examples. Format: f a a\\t{+|-}f a a\\t...")
			            .create());
        }
        if (isOn(flags, USE_PARAMS))
            options.addOption(
                    OptionBuilder
                            .withLongOpt("params")
                            .withArgName("file")
                            .hasArg()
                            .withDescription("Save/load learned walker parameters.")
                            .create());
        if (isOn(flags, USE_LEARNINGSET)) {
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
        if (isOn(flags, USE_COMPLEX_FEATURES)) {
            options.addOption(
                    OptionBuilder
                            .withLongOpt("complexFeatures")
                            .withArgName("file")
                            .hasArg()
                            .withDescription("Properties file for complex features")
                            .create());
        }
        options.addOption(
        	OptionBuilder
        		.withLongOpt("force")
        		.withDescription("Ignore errors and run anyway")
        		.create());
    }

    protected void constructUsageSyntax(StringBuilder syntax, int flags) {
        if (isOn(flags, USE_PROGRAMFILES)) syntax.append(" --programFiles file.crules:file.cfacts:file.graph");
        if (isOn(flags, USE_DATA)) syntax.append(" --data training.data");
        if (isOn(flags, USE_OUTPUT)) syntax.append(" --output training.cooked");
        if (isOn(flags, USE_PROVER)) syntax.append(" [--prover { ppr[:depth] | dpr[:eps[:alph[:strat]]] | tr[:depth] }]");
        if (isOn(flags, USE_TRAIN)) syntax.append(" --train training.data");
        if (isOn(flags, USE_TEST)) syntax.append(" --test testing.data");
        if (isOn(flags, USE_PARAMS)) syntax.append("  [--params params.txt]");
        if (isOn(flags, USE_LEARNINGSET)) syntax.append(" [--epochs <int>] [--traceLosses]");
        if (isOn(flags, USE_COMPLEX_FEATURES)) syntax.append("[-- complexFeatConfig complex_features.conf");
    }

    /**
     * Calls System.exit(0)
     */
    protected void usageOptions(Options options, int flags) {
    	usageOptions(options,flags,null);
    }

    /**
     * Calls System.exit(0)
     */
    protected void usageOptions(Options options, int flags, String msg) {
        HelpFormatter formatter = new HelpFormatter();
        int width = 80;

        String swidth = System.getenv("COLUMNS");
        if (swidth != null) {
            try {
                width = Integer.parseInt(swidth);
            } catch (NumberFormatException e) {}
        }
//        formatter.setWidth(width);
//        formatter.setLeftPadding(0);
//        formatter.setDescPadding(2);
        StringBuilder syntax = new StringBuilder();
        constructUsageSyntax(syntax, flags);
        String printMsg = "";
        if (msg != null) printMsg = ("\nBAD USAGE:" + msg +"\n\n");
//        formatter.printHelp(syntax.toString(), options);
        PrintWriter pw = new PrintWriter(System.err);
        formatter.printHelp(pw, width, syntax.toString(), printMsg, options, 0, 2, printMsg);
        pw.flush();
        pw.close();
        System.exit(0);
    }

    @Override
    public String toString() {
        return this.getClass().getCanonicalName();
    }

    protected String[] combinedArgs(String[] origArgs) {
        // if the user specified a properties file, add those values at the end
        // (so that command line args override them)
        if (System.getProperty(PROPFILE) != null) {
            String[] propArgs = fakeCommandLine(System.getProperty(PROPFILE));
            String[] args = new String[origArgs.length + propArgs.length];
            int i = 0;
            for (int j = 0; j < origArgs.length; j++) args[i++] = origArgs[j];
            for (int j = 0; j < propArgs.length; j++) args[i++] = propArgs[j];
            return args;
        }
        return origArgs;
    }

    protected String[] fakeCommandLine(String propsFile) {
        Properties props = new Properties();
        try {
            props.load(new BufferedReader(new FileReader(propsFile)));
            return fakeCommandLine(props);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException(e);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
    protected String[] fakeCommandLine(Properties props) {
        StringBuilder sb = new StringBuilder();
        for (String name : props.stringPropertyNames()) {
            sb.append(" --").append(name);
            if (props.getProperty(name) != null) {
                sb.append(" ").append(props.getProperty(name));
            }
        }
        return sb.substring(1).split("\\s");
    }
	public static void missing(int options, int flags) {
		StringBuilder sb = new StringBuilder("Missing required option:\n");
		switch(options) {
			case USE_PROGRAMFILES:sb.append("\tprogramFiles"); break;
			default: throw new UnsupportedOperationException("Bad programmer! Add handling to Configuration.missing for flag "+options);
		}
		Configuration c = new Configuration();
		Options o = new Options();
		c.addOptions(o, flags);
		c.usageOptions(o, flags, sb.toString());
	}
}
