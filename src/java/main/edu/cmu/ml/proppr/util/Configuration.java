package edu.cmu.ml.proppr.util;

import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PermissiveParser;

import edu.cmu.ml.proppr.learn.tools.ExpWeightingScheme;
import edu.cmu.ml.proppr.learn.tools.LinearWeightingScheme;
import edu.cmu.ml.proppr.learn.tools.ReLUWeightingScheme;
import edu.cmu.ml.proppr.learn.tools.SigmoidWeightingScheme;
import edu.cmu.ml.proppr.learn.tools.TanhWeightingScheme;
import edu.cmu.ml.proppr.learn.tools.WeightingScheme;
import edu.cmu.ml.proppr.prove.DprProver;
import edu.cmu.ml.proppr.prove.Prover;
import edu.cmu.ml.proppr.prove.TracingDfsProver;
import edu.cmu.ml.proppr.prove.wam.AWamProgram;
import edu.cmu.ml.proppr.prove.wam.WamProgram;
import edu.cmu.ml.proppr.prove.wam.plugins.FactsPlugin;
import edu.cmu.ml.proppr.prove.wam.plugins.LightweightGraphPlugin;
import edu.cmu.ml.proppr.prove.wam.plugins.WamPlugin;


import java.io.*;

/**
 * Configuration engine for input files, output files and (for whatever reason) constants/hyperparameters.
 * 
 * For modules (prover, grounder, trainer, tester, etc) see ModuleConfiguration subclass. 
 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
 *
 */
public class Configuration {
	/* set files */
	/** file. */
	public static final int USE_QUERIES = 0x1;
	public static final int USE_GROUNDED = 0x2;
	public static final int USE_ANSWERS = 0x4;
	public static final int USE_TRAIN = 0x8;
	public static final int USE_TEST = 0x10;
	public static final int USE_PARAMS = 0x20;
	private static final String QUERIES_FILE_OPTION = "queries";
	private static final String GROUNDED_FILE_OPTION = "grounded";
	private static final String ANSWERS_FILE_OPTION = "solutions";
	private static final String TRAIN_FILE_OPTION = "train";
	private static final String TEST_FILE_OPTION = "test";
	private static final String PARAMS_FILE_OPTION = "params";
	private static final String EXAMPLES_FORMAT = "f(A1,A2)\\t{+|-}f(a1,a2)\\t...";

	/* set constants */
	/** constant. programFiles, ternaryIndex */
	public static final int USE_WAM = 0x1;
	/** constant. */
	public static final int USE_THREADS = 0x2;
	public static final int USE_EPOCHS = 0x4;
	public static final int USE_TRACELOSSES = 0x8;
	public static final int USE_FORCE = 0x10;
	private static final String PROGRAMFILES_CONST_OPTION = "programFiles";
	private static final String TERNARYINDEX_CONST_OPTION = "ternaryIndex";
	private static final String THREADS_CONST_OPTION = "threads";
	private static final String EPOCHS_CONST_OPTION = "epochs";
	private static final String TRACELOSSES_CONST_OPTION = "traceLosses";
	private static final String FORCE_CONST_OPTION = "force";

	/* set class for module */
	/** module. */
	public static final int USE_WEIGHTINGSCHEME = 0x1;
	public static final int USE_GROUNDER = 0x2;
	public static final int USE_SRW = 0x4;
	public static final int USE_TRAINER = 0x8;
	public static final int USE_PROVER = 0x10;

	public static final String PROPFILE = "config.properties";
	private static final boolean DEFAULT_COMBINE = true;

	public File queryFile = null;
	public File testFile = null;
	//	public File complexFeatureConfigFile = null;
	public File groundedFile = null;
	public File paramsFile = null;
	public File solutionsFile = null;

	public AWamProgram program = null;
	public WamPlugin[] plugins = null;
	public String[] programFiles = null;
	public int nthreads = -1;
	//    public double alpha = Component.ALPHA_DEFAULT;
	public int epochs = 5;
	public boolean traceLosses = false;
	public boolean force = false;
	public boolean ternaryIndex = false;

	static boolean isOn(int flags, int flag) {
		return (flags & flag) == flag;
	}	
	static boolean anyOn(int flags, int flag) {
		return (flags & flag) > 0;
	}

	protected int inputFiles(int[] flags) { return flags[0]; }
	protected int outputFiles(int[] flags) { return flags[1]; }
	protected int constants(int[] flags) { return flags[2]; }
	protected int modules(int[] flags) { return flags[3]; }

	private Configuration() {}
	public Configuration(String[] args, int inputFiles, int outputFiles, int constants, int modules) {
		boolean combine = DEFAULT_COMBINE;
		int[] flags = {inputFiles, outputFiles, constants, modules};
		
		Options options = new Options();
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
	protected File getExistingFile(String filename) {
		File value = new File(filename);
		if (!value.exists()) throw new IllegalArgumentException("File '"+value.getName()+"' must exist");
		return value;
	}
	protected void retrieveSettings(CommandLine line, int[] allFlags, Options options) throws IOException {
		int flags;

		// input files: must exist already
		flags = inputFiles(allFlags);
		if (isOn(flags,USE_QUERIES) && line.hasOption(QUERIES_FILE_OPTION))   this.queryFile = getExistingFile(line.getOptionValue(QUERIES_FILE_OPTION));
		if (isOn(flags,USE_GROUNDED) && line.hasOption(GROUNDED_FILE_OPTION)) this.groundedFile = getExistingFile(line.getOptionValue(GROUNDED_FILE_OPTION));
		if (isOn(flags,USE_ANSWERS) && line.hasOption(ANSWERS_FILE_OPTION))   this.solutionsFile = getExistingFile(line.getOptionValue(ANSWERS_FILE_OPTION));
		if (isOn(flags,USE_TEST) && line.hasOption(TEST_FILE_OPTION))         this.testFile = getExistingFile(line.getOptionValue(TEST_FILE_OPTION));
		if (isOn(flags,USE_TRAIN) && line.hasOption(TRAIN_FILE_OPTION))       this.queryFile = getExistingFile(line.getOptionValue(TRAIN_FILE_OPTION));
		if (isOn(flags,USE_PARAMS) && line.hasOption(PARAMS_FILE_OPTION))     this.paramsFile = getExistingFile(line.getOptionValue(PARAMS_FILE_OPTION));

		// output & intermediate files: may not exist yet
		flags = outputFiles(allFlags);
		if (isOn(flags,USE_QUERIES) && line.hasOption(QUERIES_FILE_OPTION))   this.queryFile = new File(line.getOptionValue(QUERIES_FILE_OPTION));
		if (isOn(flags,USE_GROUNDED) && line.hasOption(GROUNDED_FILE_OPTION)) this.groundedFile = new File(line.getOptionValue(GROUNDED_FILE_OPTION));
		if (isOn(flags,USE_ANSWERS) && line.hasOption(ANSWERS_FILE_OPTION))   this.solutionsFile = new File(line.getOptionValue(ANSWERS_FILE_OPTION));
		if (isOn(flags,USE_TEST) && line.hasOption(TEST_FILE_OPTION))         this.testFile = new File(line.getOptionValue(TEST_FILE_OPTION));
		if (isOn(flags,USE_TRAIN) && line.hasOption(TRAIN_FILE_OPTION))       this.queryFile = new File(line.getOptionValue(TRAIN_FILE_OPTION));
		if (isOn(flags,USE_PARAMS) && line.hasOption(PARAMS_FILE_OPTION))     this.paramsFile = new File(line.getOptionValue(PARAMS_FILE_OPTION));

		// constants
		flags = constants(allFlags);
		if (isOn(flags,USE_WAM) && line.hasOption(PROGRAMFILES_CONST_OPTION))  this.programFiles = line.getOptionValues(PROGRAMFILES_CONST_OPTION);
		if (isOn(flags,USE_WAM) && line.hasOption(TERNARYINDEX_CONST_OPTION))  this.ternaryIndex = Boolean.parseBoolean(line.getOptionValue(TERNARYINDEX_CONST_OPTION));
		if (isOn(flags,USE_THREADS) && line.hasOption(THREADS_CONST_OPTION))    this.nthreads = Integer.parseInt(line.getOptionValue(THREADS_CONST_OPTION));
		if (isOn(flags,USE_EPOCHS) && line.hasOption(EPOCHS_CONST_OPTION))      this.epochs = Integer.parseInt(line.getOptionValue(EPOCHS_CONST_OPTION));
		if (isOn(flags,USE_TRACELOSSES) && line.hasOption(TRACELOSSES_CONST_OPTION)) this.traceLosses = true;
		if (isOn(flags,USE_FORCE) && line.hasOption(FORCE_CONST_OPTION))              this.force = true;


		if (this.programFiles != null) this.loadProgramFiles();
	}

	/**
	 * Clears program and plugin list, then loads them from --programFiles option.
	 * @throws IOException
	 */
	protected void loadProgramFiles() throws IOException {
		this.program = null;
		this.plugins = new WamPlugin[programFiles.length-1];
		int i=0;
		for (String s : programFiles) {
			if (s.endsWith(".wam")) {
				if (this.program != null) throw new IllegalArgumentException("Multiple WAM programs not supported");
				this.program = WamProgram.load(this.getExistingFile(s));
			} else if (s.endsWith(".graph")) {
				this.plugins[i++] = LightweightGraphPlugin.load(this.getExistingFile(s));
			} else if (s.endsWith("facts")) {
				this.plugins[i++] = FactsPlugin.load(this.getExistingFile(s), this.ternaryIndex);
			} else {
				throw new IllegalArgumentException("Plugin type for "+s+" unsupported/unknown");
			}
		}
	}

	/**
	 * For all option flags as specified in this file, addOptions creates
	 * and adds Option objects to the Options object.
	 */
	protected void addOptions(Options options, int[] allFlags) {
		int flags;

		// input files
		flags = inputFiles(allFlags);
		if(isOn(flags, USE_QUERIES))
			options.addOption(
					OptionBuilder
					.withLongOpt(QUERIES_FILE_OPTION)
					.withArgName("file")
					.hasArg()
					.withDescription("Queries. Format (discards after tab): "+EXAMPLES_FORMAT)
					.create());		
		if (isOn(flags, USE_GROUNDED))
			options.addOption(
					OptionBuilder
					.withLongOpt(GROUNDED_FILE_OPTION)
					.isRequired()
					.withArgName("file")
					.hasArg()
					.withDescription("Grounded examples. Format: query\\tkeys,,\\tposList,,\\tnegList,,\\tgraph")
					.create());
		if (isOn(flags, USE_TRAIN))
			options.addOption(
					OptionBuilder
					.withLongOpt(TRAIN_FILE_OPTION)
					.isRequired()
					.withArgName("file")
					.hasArg()
					.withDescription("Training examples. Format: "+EXAMPLES_FORMAT)
					.create());
		if (isOn(flags, USE_TEST))
			options.addOption(
					OptionBuilder
					.withLongOpt(TEST_FILE_OPTION)
					.isRequired()
					.withArgName("file")
					.hasArg()
					.withDescription("Testing examples. Format: "+EXAMPLES_FORMAT)
					.create());
		if (isOn(flags, USE_PARAMS))
			options.addOption(
					OptionBuilder
					.withLongOpt(PARAMS_FILE_OPTION)
					.withArgName("file")
					.hasArg()
					.withDescription("Learned walker parameters. Format: feature\\t0.000000")
					.create());

		// output files
		flags = outputFiles(allFlags);
		if(isOn(flags, USE_QUERIES))
			options.addOption(
					OptionBuilder
					.withLongOpt(QUERIES_FILE_OPTION)
					.withArgName("file")
					.hasArg()
					.withDescription("Output queries")
					.create());		
		if (isOn(flags, USE_GROUNDED))
			options.addOption(
					OptionBuilder
					.withLongOpt(GROUNDED_FILE_OPTION)
					.isRequired()
					.withArgName("file")
					.hasArg()
					.withDescription("Output grounded examples.")
					.create());
		if (isOn(flags, USE_TRAIN))
			options.addOption(
					OptionBuilder
					.withLongOpt(TRAIN_FILE_OPTION)
					.isRequired()
					.withArgName("file")
					.hasArg()
					.withDescription("Output training examples.")
					.create());
		if (isOn(flags, USE_TEST))
			options.addOption(
					OptionBuilder
					.withLongOpt(TEST_FILE_OPTION)
					.isRequired()
					.withArgName("file")
					.hasArg()
					.withDescription("Ouput testing examples.")
					.create());
		if (isOn(flags, USE_PARAMS))
			options.addOption(
					OptionBuilder
					.withLongOpt(PARAMS_FILE_OPTION)
					.withArgName("file")
					.hasArg()
					.withDescription("Output learned walker parameters.")
					.create());

		// constants
		flags = constants(allFlags);
		if (isOn(flags, USE_WAM)) {
			options.addOption(
					OptionBuilder
					.withLongOpt(PROGRAMFILES_CONST_OPTION)
					.withArgName("file:...:file")
					.hasArgs()
					.withValueSeparator(':')
					.withDescription("Description of the logic program. Permitted extensions: .wam, .cfacts, .graph, .sparse")
					.create());
			options.addOption(
					OptionBuilder
					.withLongOpt(TERNARYINDEX_CONST_OPTION)
					.withArgName("true|false")
					.hasArg()
					.withDescription("Turn on A1A2 index for facts of arity >= 3.")
					.create());
		}		
		if (isOn(flags, USE_THREADS)) 
			options.addOption(
				OptionBuilder
				.withLongOpt(THREADS_CONST_OPTION)
				.withArgName("integer")
				.hasArg()
				.withDescription("Use x worker threads. (Pls ensure x < #cores)")
				.create());
		if (isOn(flags, USE_EPOCHS))
			options.addOption(
				OptionBuilder
				.withLongOpt(EPOCHS_CONST_OPTION)
				.withArgName("integer")
				.hasArg()
				.withDescription("Use x training epochs (default = 5)")
				.create());
		if (isOn(flags, USE_TRACELOSSES))
			options.addOption(
				OptionBuilder
				.withLongOpt(TRACELOSSES_CONST_OPTION)
				.withDescription("Print training loss at each epoch")
				.create());
		if (isOn(flags, USE_FORCE))
			options.addOption(
				OptionBuilder
				.withLongOpt("force")
				.withDescription("Ignore errors and run anyway")
				.create());
		


//		if (isOn(flags, USE_COMPLEX_FEATURES)) {
//			options.addOption(
//					OptionBuilder
//					.withLongOpt("complexFeatures")
//					.withArgName("file")
//					.hasArg()
//					.withDescription("Properties file for complex features")
//					.create());
//		}
	}

	protected void constructUsageSyntax(StringBuilder syntax, int[] allFlags) {
		int flags;
		
		//input files
		flags = inputFiles(allFlags);
		if (isOn(flags, USE_QUERIES)) syntax.append(" --").append(QUERIES_FILE_OPTION).append(" inputFile");
		if (isOn(flags, USE_GROUNDED)) syntax.append(" --").append(GROUNDED_FILE_OPTION).append(" inputFile.grounded");
		if (isOn(flags, USE_ANSWERS)) syntax.append(" --").append(ANSWERS_FILE_OPTION).append(" inputFile");
		if (isOn(flags, USE_TRAIN)) syntax.append(" --").append(TRAIN_FILE_OPTION).append(" inputFile");
		if (isOn(flags, USE_TEST)) syntax.append(" --").append(TEST_FILE_OPTION).append(" inputFile");
		if (isOn(flags, USE_PARAMS)) syntax.append(" --").append(PARAMS_FILE_OPTION).append(" inputFile");
		
		//output files
		flags = outputFiles(allFlags);
		if (isOn(flags, USE_QUERIES)) syntax.append(" --").append(QUERIES_FILE_OPTION).append(" outputFile");
		if (isOn(flags, USE_GROUNDED)) syntax.append(" --").append(GROUNDED_FILE_OPTION).append(" outputFile.grounded");
		if (isOn(flags, USE_ANSWERS)) syntax.append(" --").append(ANSWERS_FILE_OPTION).append(" outputFile");
		if (isOn(flags, USE_TRAIN)) syntax.append(" --").append(TRAIN_FILE_OPTION).append(" outputFile");
		if (isOn(flags, USE_TEST)) syntax.append(" --").append(TEST_FILE_OPTION).append(" outputFile");
		if (isOn(flags, USE_PARAMS)) syntax.append(" --").append(PARAMS_FILE_OPTION).append(" outputFile");
		
		//constants
		flags = constants(allFlags);
		if (isOn(flags, USE_WAM)) syntax.append(" --").append(PROGRAMFILES_CONST_OPTION).append(" file.crules:file.cfacts:file.graph");
		if (isOn(flags, USE_WAM)) syntax.append(" [--").append(TERNARYINDEX_CONST_OPTION).append(" true|false]");
		if (isOn(flags, USE_THREADS)) syntax.append(" [--").append(THREADS_CONST_OPTION).append(" integer]");
		if (isOn(flags, USE_EPOCHS)) syntax.append(" [--").append(EPOCHS_CONST_OPTION).append(" integer]");
		if (isOn(flags, USE_TRACELOSSES)) syntax.append(" [--").append(TRACELOSSES_CONST_OPTION).append("]");
		if (isOn(flags, USE_FORCE)) syntax.append(" [--").append(FORCE_CONST_OPTION).append("]");
		
	}
	
	/**
	 * Calls System.exit(0)
	 */
	protected void usageOptions(Options options, int inputFile, int outputFile, int constants, int modules, String msg) {
		usageOptions(options,new int[] {inputFile, outputFile, constants, modules},msg);
	}
	

	/**
	 * Calls System.exit(0)
	 */
	protected void usageOptions(Options options, int[] flags) {
		usageOptions(options,flags,null);
	}

	/**
	 * Calls System.exit(0)
	 */
	protected void usageOptions(Options options, int[] flags, String msg) {
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
	public static void missing(int options, int[] flags) {
		StringBuilder sb = new StringBuilder("Missing required option:\n");
		switch(options) {
		case USE_WAM:sb.append("\tprogramFiles"); break;
		default: throw new UnsupportedOperationException("Bad programmer! Add handling to Configuration.missing for flag "+options);
		}
		Configuration c = new Configuration();
		Options o = new Options();
		c.addOptions(o, flags);
		c.usageOptions(o, flags, sb.toString());
	}
}
