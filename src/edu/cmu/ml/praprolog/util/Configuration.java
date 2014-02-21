package edu.cmu.ml.praprolog.util;

import edu.cmu.ml.praprolog.prove.*;
import org.apache.commons.cli.*;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class Configuration {
    public static final int
            USE_PROGRAMFILES = 1,
            USE_DATA = 2,
            USE_OUTPUT = 4,
            USE_PROVER = 8,
            USE_THREADS = 0x10,
            USE_DEFAULTS = 0x19,
            USE_TRAIN = 0x20,
            USE_TEST = 0x40,
            USE_LEARNINGSET = 0x80,
            USE_QUERIES = 0x100,
            USE_PARAMS = 0x200,
            USE_TRAINTEST = 0x260,
            USE_SRW = 0x400,
            USE_COMPLEX_FEATURES = 0x800;
    public static final String PROPFILE = "config.properties";
    private static final boolean DEFAULT_COMBINE = true;
    public Prover prover = null;
    public String[] programFiles = null;
    public String
            dataFile = null,
            queryFile = null,
            testFile = null,
            outputFile = null,
            complexFeatureConfigFile = null,
            paramsFile = null;
    public int nthreads = -1;
    public double alpha = Component.ALPHA_DEFAULT;
    public int epochs = 5;
    public boolean traceLosses = false;

    public Configuration(String[] args) { this(args, new DprProver()); }

    public Configuration(String[] args, int flags) { this(args, new DprProver(), flags, DEFAULT_COMBINE); }

    public Configuration(String[] args, Prover dflt) { this(args, dflt, USE_DEFAULTS, DEFAULT_COMBINE); }

    public Configuration(String[] args, Prover dflt, int flags) { this(args, dflt, flags, DEFAULT_COMBINE); }

    public Configuration(String[] args, Prover dflt, int flags, boolean combine) {
        if (isOn(flags, USE_DATA) && isOn(flags, USE_TRAINTEST)) {
            throw new IllegalArgumentException("Programmer error: Illegal to request --data and also --train/--test");
        }

        Options options = new Options();
        this.prover = dflt;
        addOptions(options, flags);

        try {
            PermissiveParser parser = new PermissiveParser(true);

            // if the user specified a properties file, add those values at the end
            // (so that command line args override them)
            if (combine) args = combinedArgs(args);

            // parse the command line arguments
            CommandLine line = parser.parse(options, args);
            if (parser.hasUnrecognizedOptions()) {
                System.err.println("WARNING: unrecognized options detected:");
                for (String opt : parser.getUnrecognizedOptions()) { System.err.println("\t" + opt); }
            }
            retrieveSettings(line, flags, options);

        } catch (Exception exp) {
            System.err.println("\n" + exp.getMessage() + "\n");
            usageOptions(options, flags);

			/*
             * For silently passing through unrecognized options, we may want to use:
			 *
	public class ExtendedGnuParser extends GnuParser {

    private boolean ignoreUnrecognizedOption;

    public ExtendedGnuParser(final boolean ignoreUnrecognizedOption) {
        this.ignoreUnrecognizedOption = ignoreUnrecognizedOption;
    }

    @Override
    protected void processOption(final String arg, final ListIterator iter) throws     ParseException {
        boolean hasOption = getOptions().hasOption(arg);

        if (hasOption || !ignoreUnrecognizedOption) {
            super.processOption(arg, iter);
        }
    }

}
			 */
        }
    }

    static boolean isOn(int flags, int flag) {
        return (flags & flag) == flag;
    }

    protected void retrieveSettings(CommandLine line, int flags, Options options) {
        if (isOn(flags, USE_PROGRAMFILES) && line.hasOption("programFiles"))
            this.programFiles = line.getOptionValues("programFiles");
        if (isOn(flags, USE_DATA) && line.hasOption("data")) this.dataFile = line.getOptionValue("data");
        if (isOn(flags, USE_QUERIES) && line.hasOption("queries")) this.queryFile = line.getOptionValue("queries");
        if ((isOn(flags, USE_OUTPUT) || isOn(flags, USE_TRAIN))
            && line.hasOption("output")) this.outputFile = line.getOptionValue("output");
        if (isOn(flags, USE_THREADS) && line.hasOption("threads"))
            this.nthreads = Integer.parseInt(line.getOptionValue("threads"));
        if (isOn(flags, USE_LEARNINGSET) && line.hasOption("epochs"))
            this.epochs = Integer.parseInt(line.getOptionValue("epochs"));
        if (isOn(flags, USE_LEARNINGSET) && line.hasOption("traceLosses")) this.traceLosses = true;
        if (isOn(flags, USE_TEST) && line.hasOption("test")) this.testFile = line.getOptionValue("test");
        if (isOn(flags, USE_TRAIN) && line.hasOption("train")) this.dataFile = line.getOptionValue("train");
        if (isOn(flags, USE_PARAMS) && line.hasOption("params")) this.paramsFile = line.getOptionValue("params");
        if (isOn(flags, USE_PROVER) && line.hasOption("prover")) {
            String[] values = line.getOptionValue("prover").split(":");
            if (values[0].startsWith("ppr")) {
                if (values.length == 1) {
                    this.prover = new PprProver();
                } else {
                    int depth = Integer.parseInt(values[1]);
                    this.prover = new PprProver(depth);
                }
            } else if (values[0].startsWith("dpr")) {
                if (values.length == 1)
                    this.prover = new DprProver();
                else {
                    double epsilon = Double.parseDouble(values[1]);
                    this.alpha = DprProver.MINALPH_DEFAULT;
                    if (values.length > 2) {
                        this.alpha = Double.parseDouble(values[2]);
                    }
                    this.prover = new DprProver(epsilon, this.alpha);
                    this.alpha += epsilon;
                }
            } else if (values[0].startsWith("tr")) {
                int depth = TracingDfsProver.DEFAULT_MAXDEPTH;
                if (values.length != 1) {
                    depth = Integer.parseInt(values[1]);
                }
                this.prover = new TracingDfsProver(depth);
            } else {
                System.err.println("No prover definition for '" + values[0] + "'");
                usageOptions(options, flags);
            }
        }
    }

    protected void addOptions(Options options, int flags) {
        options.addOption(
                OptionBuilder
                        .withLongOpt("programFiles")
                        .isRequired(isOn(flags, USE_PROGRAMFILES))
                        .withArgName("file:...:file")
                        .hasArgs()
                        .withValueSeparator(':')
                        .withDescription("Description of the logic program. Formats:\n\t\tcrules:goal,, & ... & goal,, # feature,, # variable,,\n\t\tcfacts:f\\ta\\ta")
                        .create());
        if (!isOn(flags, USE_TRAINTEST)) options.addOption(
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
        options.addOption(
                OptionBuilder
                        .withLongOpt("queries")
                        .isRequired(isOn(flags, USE_QUERIES))
                        .withArgName("file")
                        .hasArg()
                        .withDescription("Queries.  Format f a a")
                        .create());
        options.addOption(
                OptionBuilder
                        .withLongOpt("prover")
                        .withArgName("class[:arg:...:arg]")
                        .hasArg()
                        .withDescription("Default: " + this.prover.getClass().getSimpleName() + "\n"
                                         + "Available options:\n"
                                         + "ppr[:depth] (default depth=5)\n"
                                         + "dpr[:eps[:alph]] (default eps=1E-4, alph=0.1)\n"
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
        if (isOn(flags, USE_TEST))
            options.addOption(
                    OptionBuilder
                            .withLongOpt("test")
                            .isRequired()
                            .withArgName("file")
                            .hasArg()
                            .withDescription("Testing examples. Format: f a a\\t{+|-}f a a\\t...")
                            .create());
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
    }

    protected void constructUsageSyntax(StringBuilder syntax, int flags) {
        if (isOn(flags, USE_PROGRAMFILES)) syntax.append(" --programFiles file.crules:file.cfacts:file.graph");
        if (isOn(flags, USE_DATA)) syntax.append(" --data training.data");
        if (isOn(flags, USE_OUTPUT)) syntax.append(" --output training.cooked");
        if (isOn(flags, USE_PROVER)) syntax.append(" [--prover { ppr[:depth] | dpr[:eps[:alph]] | tr[:depth] }]");
        if (isOn(flags, USE_TRAIN)) syntax.append(" --train training.data");
        if (isOn(flags, USE_TEST)) syntax.append(" --test testing.data");
        if (isOn(flags, USE_PARAMS)) syntax.append("  [--params params.txt]");
        if (isOn(flags, USE_LEARNINGSET)) syntax.append(" [--epochs <int>] [--traceLosses]");
    }

    protected void usageOptions(Options options, int flags) {
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
        formatter.printHelp(syntax.toString(), options);
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
            StringBuilder sb = new StringBuilder();
            for (String name : props.stringPropertyNames()) {
                sb.append(" --").append(name);
                if (props.getProperty(name) != null) {
                    sb.append(" ").append(props.getProperty(name));
                }
            }
            return sb.substring(1).split("\\s");
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException(e);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
