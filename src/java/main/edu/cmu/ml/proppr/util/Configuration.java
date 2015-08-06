package edu.cmu.ml.proppr.util;

import java.util.Arrays;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PermissiveParser;
import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.prove.wam.WamProgram;
import edu.cmu.ml.proppr.prove.wam.WamBaseProgram;
import edu.cmu.ml.proppr.prove.wam.plugins.FactsPlugin;
import edu.cmu.ml.proppr.prove.wam.plugins.GraphlikePlugin;
import edu.cmu.ml.proppr.prove.wam.plugins.LightweightGraphPlugin;
import edu.cmu.ml.proppr.prove.wam.plugins.SparseGraphPlugin;
import edu.cmu.ml.proppr.prove.wam.plugins.SplitFactsPlugin;
import edu.cmu.ml.proppr.prove.wam.plugins.WamPlugin;
import edu.cmu.ml.proppr.util.multithreading.Multithreading;

import java.io.*;

/**
 * Configuration engine for input files, output files and (for whatever reason) constants/hyperparameters.
 * 
 * For modules (prover, grounder, trainer, tester, etc) see ModuleConfiguration subclass. 
 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
 *
 */
public class Configuration {
	private static final Logger log = Logger.getLogger(Configuration.class);
	public static final int FORMAT_WIDTH=18;
	public static final String FORMAT_STRING="%"+FORMAT_WIDTH+"s";
	/* set files */
	/** file. */
	public static final int USE_QUERIES = 0x1;
	public static final int USE_GROUNDED = 0x2;
	public static final int USE_ANSWERS = 0x4;
	public static final int USE_TRAIN = 0x8;
	public static final int USE_TEST = 0x10;
	public static final int USE_PARAMS = 0x20;
	public static final int USE_GRADIENT = 0x40;
	public static final int USE_INIT_PARAMS = 0x80;
	public static final String QUERIES_FILE_OPTION = "queries";
	public static final String GROUNDED_FILE_OPTION = "grounded";
	public static final String SOLUTIONS_FILE_OPTION = "solutions";
	public static final String TRAIN_FILE_OPTION = "train";
	public static final String TEST_FILE_OPTION = "test";
	public static final String PARAMS_FILE_OPTION = "params";
	public static final String INIT_PARAMS_FILE_OPTION = "initParams";
	public static final String GRADIENT_FILE_OPTION = "gradient";
	public static final String EXAMPLES_FORMAT = "f(A1,A2)\\t{+|-}f(a1,a2)\\t...";

	/* set constants */
	/** constant. programFiles, ternaryIndex */
	public static final int USE_WAM = 0x1;
	/** constant. */
	public static final int USE_THREADS = 0x2;
	public static final int USE_EPOCHS = 0x4;
	public static final int USE_TRACELOSSES = 0x8;
	public static final int USE_FORCE = 0x10;
	public static final int USE_ORDER = 0x20;
	public static final int USE_DUPCHECK = 0x40;
	public static final int USE_THROTTLE = 0x80;
	private static final String PROGRAMFILES_CONST_OPTION = "programFiles";
	private static final String TERNARYINDEX_CONST_OPTION = "ternaryIndex";
	private static final String APR_CONST_OPTION = "apr";
	private static final String THREADS_CONST_OPTION = "threads";
	private static final String EPOCHS_CONST_OPTION = "epochs";
	private static final String TRACELOSSES_CONST_OPTION = "traceLosses";
	private static final String FORCE_CONST_OPTION = "force";
	private static final String ORDER_CONST_OPTION = "order";
	private static final String DUPCHECK_CONST_OPTION = "duplicateCheck";
	private static final String THROTTLE_CONST_OPTION = "throttle";
	

	/* set class for module */
	/** module. */
	public static final int USE_SQUASHFUNCTION = 0x1;
	public static final int USE_GROUNDER = 0x2;
	public static final int USE_SRW = 0x4;
	public static final int USE_TRAINER = 0x8;
	public static final int USE_PROVER = 0x10;

	public static final String PROPFILE = "config.properties";
	private static final boolean DEFAULT_COMBINE = true;
	
	private static final int USE_APR = USE_WAM | USE_PROVER | USE_SRW;

	public File queryFile = null;
	public File testFile = null;
	public File groundedFile = null;
	public File paramsFile = null;
	public File initParamsFile = null;
	public File solutionsFile = null;
	public File gradientFile = null;

	public WamProgram program = null;
	public WamPlugin[] plugins = null;
	public String[] programFiles = null;
	public int nthreads = -1;
    public APROptions apr = new APROptions();
	public int epochs = 5;
	public boolean traceLosses = true;
	public boolean force = false;
	public boolean ternaryIndex = false;
	public boolean maintainOrder = true;
	public int duplicates = (int) 1e6;
	public int throttle = Multithreading.DEFAULT_THROTTLE;

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
		options.addOption(OptionBuilder.withLongOpt("profile")
				.withDescription("Holds all computation & loading until the return key is pressed.")
				.create());
		addOptions(options, flags);

		CommandLine line = null;
		try {
			PermissiveParser parser = new PermissiveParser(true);

			// if the user specified a properties file, add those values at the end
			// (so that command line args override them)
			if(combine) args = combinedArgs(args);

			// parse the command line arguments
			line = parser.parse( options, args );
			if (parser.hasUnrecognizedOptions()) {
				System.err.println("WARNING: unrecognized options detected:");
				for (String opt : parser.getUnrecognizedOptions()) { System.err.println("\t"+opt); }
			}
			if (line.hasOption("profile")) {
				System.out.println("Holding for profiler setup; press any key to proceed.");
				System.in.read();
			}
			retrieveSettings(line,flags,options);

		} catch( Exception exp ) {
			if (args[0].equals("--help")) usageOptions(options, flags);
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
		
		if (line.hasOption("help")) usageOptions(options, allFlags);

		// input files: must exist already
		flags = inputFiles(allFlags);
		if (isOn(flags,USE_QUERIES) && line.hasOption(QUERIES_FILE_OPTION))         this.queryFile = getExistingFile(line.getOptionValue(QUERIES_FILE_OPTION));
		if (isOn(flags,USE_GROUNDED) && line.hasOption(GROUNDED_FILE_OPTION))       this.groundedFile = getExistingFile(line.getOptionValue(GROUNDED_FILE_OPTION));
		if (isOn(flags,USE_ANSWERS) && line.hasOption(SOLUTIONS_FILE_OPTION))       this.solutionsFile = getExistingFile(line.getOptionValue(SOLUTIONS_FILE_OPTION));
		if (isOn(flags,USE_TEST) && line.hasOption(TEST_FILE_OPTION))               this.testFile = getExistingFile(line.getOptionValue(TEST_FILE_OPTION));
		if (isOn(flags,USE_TRAIN) && line.hasOption(TRAIN_FILE_OPTION))             this.queryFile = getExistingFile(line.getOptionValue(TRAIN_FILE_OPTION));
		if (isOn(flags,USE_PARAMS) && line.hasOption(PARAMS_FILE_OPTION))           this.paramsFile = getExistingFile(line.getOptionValue(PARAMS_FILE_OPTION));
		if (isOn(flags,USE_INIT_PARAMS) && line.hasOption(INIT_PARAMS_FILE_OPTION)) this.initParamsFile = getExistingFile(line.getOptionValue(INIT_PARAMS_FILE_OPTION));
		if (isOn(flags,USE_GRADIENT) && line.hasOption(GRADIENT_FILE_OPTION))       this.gradientFile = getExistingFile(line.getOptionValue(GRADIENT_FILE_OPTION));
		
		// output & intermediate files: may not exist yet
		flags = outputFiles(allFlags);
		if (isOn(flags,USE_QUERIES) && line.hasOption(QUERIES_FILE_OPTION))         this.queryFile = new File(line.getOptionValue(QUERIES_FILE_OPTION));
		if (isOn(flags,USE_GROUNDED) && line.hasOption(GROUNDED_FILE_OPTION))       this.groundedFile = new File(line.getOptionValue(GROUNDED_FILE_OPTION));
		if (isOn(flags,USE_ANSWERS) && line.hasOption(SOLUTIONS_FILE_OPTION))       this.solutionsFile = new File(line.getOptionValue(SOLUTIONS_FILE_OPTION));
		if (isOn(flags,USE_TEST) && line.hasOption(TEST_FILE_OPTION))               this.testFile = new File(line.getOptionValue(TEST_FILE_OPTION));
		if (isOn(flags,USE_TRAIN) && line.hasOption(TRAIN_FILE_OPTION))             this.queryFile = new File(line.getOptionValue(TRAIN_FILE_OPTION));
		if (isOn(flags,USE_PARAMS) && line.hasOption(PARAMS_FILE_OPTION))           this.paramsFile = new File(line.getOptionValue(PARAMS_FILE_OPTION));
		if (isOn(flags,USE_GRADIENT) && line.hasOption(GRADIENT_FILE_OPTION))       this.gradientFile = new File(line.getOptionValue(GRADIENT_FILE_OPTION));

		// constants
		flags = constants(allFlags);
		if (isOn(flags,USE_WAM)) {
			if (line.hasOption(PROGRAMFILES_CONST_OPTION)) this.programFiles = line.getOptionValues(PROGRAMFILES_CONST_OPTION);
			if (line.hasOption(TERNARYINDEX_CONST_OPTION)) this.ternaryIndex = Boolean.parseBoolean(line.getOptionValue(TERNARYINDEX_CONST_OPTION));
		}
		if (anyOn(flags,USE_APR))
			if (line.hasOption(APR_CONST_OPTION))          this.apr = new APROptions(line.getOptionValues(APR_CONST_OPTION));
		if (isOn(flags,USE_THREADS) && line.hasOption(THREADS_CONST_OPTION))   this.nthreads = Integer.parseInt(line.getOptionValue(THREADS_CONST_OPTION));
		if (isOn(flags,USE_EPOCHS) && line.hasOption(EPOCHS_CONST_OPTION))     this.epochs = Integer.parseInt(line.getOptionValue(EPOCHS_CONST_OPTION));
		if (isOn(flags,USE_TRACELOSSES) && line.hasOption(TRACELOSSES_CONST_OPTION)) this.traceLosses = true;
		if (isOn(flags,USE_FORCE) && line.hasOption(FORCE_CONST_OPTION))             this.force = true;
		if (isOn(flags,USE_ORDER) && line.hasOption(ORDER_CONST_OPTION)) {
			String order = line.getOptionValue(ORDER_CONST_OPTION);
			if (order.equals("same") || order.equals("maintain")) this.maintainOrder = true;
			else this.maintainOrder = false;
		}
		if (anyOn(flags,USE_DUPCHECK|USE_WAM) && line.hasOption(DUPCHECK_CONST_OPTION)) this.duplicates = (int) Double.parseDouble(line.getOptionValue(DUPCHECK_CONST_OPTION));
		if (isOn(flags,USE_THROTTLE) && line.hasOption(THROTTLE_CONST_OPTION)) this.throttle = Integer.parseInt(line.getOptionValue(THROTTLE_CONST_OPTION));
		
		if (this.programFiles != null) this.loadProgramFiles(line,allFlags,options);
	}

	/**
	 * Clears program and plugin list, then loads them from --programFiles option.
	 * @param flags 
	 * @param options 
	 * @throws IOException
	 */
	protected void loadProgramFiles(CommandLine line, int[] flags, Options options) throws IOException {
		this.program = null;
		int nplugins = programFiles.length;
		for (String s : programFiles) if (s.endsWith(".wam")) nplugins--;
		this.plugins = new WamPlugin[nplugins];
		int i=0;
		int wam,graph,facts;
		wam = graph = facts = 0;
		int iFacts = -1;
		for (String s : programFiles) {
			if (s.endsWith(".wam")) {
				if (this.program != null) usageOptions(options,flags,PROGRAMFILES_CONST_OPTION+": Multiple WAM programs not supported");
				this.program = WamBaseProgram.load(this.getExistingFile(s));
				wam++;
			} else if (i>=this.plugins.length) {
				usageOptions(options,flags,PROGRAMFILES_CONST_OPTION+": Parser got very confused about how many plugins you specified. Send Katie a bug report!");
			} else if (s.endsWith(GraphlikePlugin.FILE_EXTENSION)) {
				this.plugins[i++] = LightweightGraphPlugin.load(this.apr, this.getExistingFile(s), this.duplicates);
				graph++;
			} else if (s.endsWith(FactsPlugin.FILE_EXTENSION)) {
				FactsPlugin p = FactsPlugin.load(this.apr, this.getExistingFile(s), this.ternaryIndex, this.duplicates);
				if (iFacts<0) {
					iFacts = i;
					this.plugins[i++] = p;
				} else {
					SplitFactsPlugin sf;
					if (this.plugins[iFacts] instanceof FactsPlugin) {
						sf = new SplitFactsPlugin(this.apr);
						sf.add((FactsPlugin) this.plugins[iFacts]);
						this.plugins[iFacts] = sf;
					} else sf = (SplitFactsPlugin) this.plugins[iFacts];
					sf.add(p);
				}
				facts++;
			} else if (s.endsWith(SparseGraphPlugin.FILE_EXTENSION)) {
				this.plugins[i++] = SparseGraphPlugin.load(this.apr, this.getExistingFile(s));
			} else {
				usageOptions(options,flags,PROGRAMFILES_CONST_OPTION+": Plugin type for "+s+" unsupported/unknown");
			}
		}
		if (facts>1) { // trim array
			this.plugins = Arrays.copyOfRange(this.plugins,0,i);
		}
		if (graph>1) {
			log.warn("Consolidated graph files not yet supported! If the same functor exists in two files, facts in the later file will be hidden from the prover!");
		}
	}

	/**
	 * For all option flags as specified in this file, addOptions creates
	 * and adds Option objects to the Options object.
	 */
	protected void addOptions(Options options, int[] allFlags) {
		int flags;

		options.addOption(
				OptionBuilder
				.withLongOpt("help")
				.withDescription("Print usage syntax.")
				.create());
		
		// input files
		flags = inputFiles(allFlags);
		if(isOn(flags, USE_QUERIES))
			options.addOption(
					OptionBuilder
					.withLongOpt(QUERIES_FILE_OPTION)
					.isRequired()
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
		if (isOn(flags, USE_INIT_PARAMS))
			options.addOption(
					OptionBuilder
					.withLongOpt(INIT_PARAMS_FILE_OPTION)
					.withArgName("file")
					.hasArg()
					.withDescription("Learned walker parameters. Same format as --params, but used to warm-start a learner.")
					.create());

		// output files
		flags = outputFiles(allFlags);
		if(isOn(flags, USE_ANSWERS))
			options.addOption(
					OptionBuilder
					.withLongOpt(SOLUTIONS_FILE_OPTION)
					.isRequired()
					.withArgName("file")
					.hasArg()
					.withDescription("Output answers")
					.create());	
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
		if(isOn(flags, USE_GRADIENT))
			options.addOption(
					OptionBuilder
					.withLongOpt(GRADIENT_FILE_OPTION)
					.isRequired()
					.withArgName("file")
					.hasArg()
					.withDescription("Output gradient.")
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
					.withDescription("Output testing examples.")
					.create());
		if (isOn(flags, USE_PARAMS))
			options.addOption(
					OptionBuilder
					.withLongOpt(PARAMS_FILE_OPTION)
					.isRequired()
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
		if (anyOn(flags, USE_APR))
			options.addOption(
					OptionBuilder
					.withLongOpt(APR_CONST_OPTION)
					.withArgName("options")
					.hasArgs()
					.withValueSeparator(':')
					.withDescription("Pagerank options. Default: eps=1e-4:alph=0.1:depth=5\n"
							+ "Syntax: param=value:param=value...\n"
							+ "Available parameters:\n"
							+ "eps, alph, depth")
					.create());
		if (isOn(flags, USE_ORDER))
			options.addOption(
					OptionBuilder
					.withLongOpt(ORDER_CONST_OPTION)
					.withArgName("o")
					.hasArg()
					.withDescription("Set ordering of outputs wrt inputs. Valid options:\n"
							+"same, maintain (keep input ordering)\n"
							+"anything else (reorder outputs to save time/memory)")
					.create()
					);
		if (anyOn(flags, USE_DUPCHECK|USE_WAM))
			options.addOption(
					OptionBuilder
					.withLongOpt(DUPCHECK_CONST_OPTION)
					.withArgName("size")
					.hasArg()
					.withDescription("Default: "+duplicates+"\nCheck for duplicates, expecting <size> values. Increasing <size> is cheap.\n"
							+"To turn off duplicate checking, set to -1.")
					.create());
		if (isOn(flags, USE_THROTTLE)) 
			options.addOption(
				OptionBuilder
				.withLongOpt(THROTTLE_CONST_OPTION)
				.withArgName("integer")
				.hasArg()
				.withDescription("Default: -1\nPause buffering of new jobs if unfinished queue grows beyond x. -1 to disable.")
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
		if (isOn(flags, USE_ANSWERS)) syntax.append(" --").append(SOLUTIONS_FILE_OPTION).append(" inputFile");
		if (isOn(flags, USE_TRAIN)) syntax.append(" --").append(TRAIN_FILE_OPTION).append(" inputFile");
		if (isOn(flags, USE_TEST)) syntax.append(" --").append(TEST_FILE_OPTION).append(" inputFile");
		if (isOn(flags, USE_PARAMS)) syntax.append(" --").append(PARAMS_FILE_OPTION).append(" params.wts");
		if (isOn(flags, USE_INIT_PARAMS)) syntax.append(" --").append(INIT_PARAMS_FILE_OPTION).append(" initParams.wts");

		//output files
		flags = outputFiles(allFlags);
		if (isOn(flags, USE_QUERIES)) syntax.append(" --").append(QUERIES_FILE_OPTION).append(" outputFile");
		if (isOn(flags, USE_GROUNDED)) syntax.append(" --").append(GROUNDED_FILE_OPTION).append(" outputFile.grounded");
		if (isOn(flags, USE_ANSWERS)) syntax.append(" --").append(SOLUTIONS_FILE_OPTION).append(" outputFile");
		if (isOn(flags, USE_TRAIN)) syntax.append(" --").append(TRAIN_FILE_OPTION).append(" outputFile");
		if (isOn(flags, USE_TEST)) syntax.append(" --").append(TEST_FILE_OPTION).append(" outputFile");
		if (isOn(flags, USE_PARAMS)) syntax.append(" --").append(PARAMS_FILE_OPTION).append(" params.wts");
		if (isOn(flags, USE_GRADIENT)) syntax.append(" --").append(GRADIENT_FILE_OPTION).append(" gradient.dwts");
		
		//constants
		flags = constants(allFlags);
		if (isOn(flags, USE_WAM)) syntax.append(" --").append(PROGRAMFILES_CONST_OPTION).append(" file.wam:file.cfacts:file.graph");
		if (isOn(flags, USE_WAM)) syntax.append(" [--").append(TERNARYINDEX_CONST_OPTION).append(" true|false]");
		if (isOn(flags, USE_THREADS)) syntax.append(" [--").append(THREADS_CONST_OPTION).append(" integer]");
		if (isOn(flags, USE_EPOCHS)) syntax.append(" [--").append(EPOCHS_CONST_OPTION).append(" integer]");
		if (isOn(flags, USE_TRACELOSSES)) syntax.append(" [--").append(TRACELOSSES_CONST_OPTION).append("]");
		if (isOn(flags, USE_FORCE)) syntax.append(" [--").append(FORCE_CONST_OPTION).append("]");
		if (isOn(flags, USE_ORDER)) syntax.append(" [--").append(ORDER_CONST_OPTION).append(" same|reorder]");
		if (anyOn(flags, USE_DUPCHECK|USE_WAM)) syntax.append(" [--").append(DUPCHECK_CONST_OPTION).append(" -1|integer]");
		if (isOn(flags, USE_THROTTLE)) syntax.append(" [--").append(THROTTLE_CONST_OPTION).append(" integer]");
	}
	
	/**
	 * Calls System.exit()
	 */
	protected void usageOptions(Options options, int inputFile, int outputFile, int constants, int modules, String msg) {
		usageOptions(options,new int[] {inputFile, outputFile, constants, modules},msg);
	}
	

	/**
	 * Calls System.exit()
	 */
	protected void usageOptions(Options options, int[] flags) {
		usageOptions(options,flags,null);
	}

	/**
	 * Calls System.exit()
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
		if (msg != null) printMsg = ("\nBAD USAGE:\n" + msg +"\n");
		//        formatter.printHelp(syntax.toString(), options);
		PrintWriter pw = new PrintWriter(System.err);
		formatter.printHelp(pw, width, syntax.toString(), "", options, 0, 2, printMsg);
		pw.write("\n");
		pw.flush();
		pw.close();
		int stat = msg!=null ? 1 : 0;
		System.exit(stat);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("\n").append( this.getClass().getCanonicalName() );
		displayFile(sb, QUERIES_FILE_OPTION, queryFile);
		displayFile(sb, TEST_FILE_OPTION, testFile);
		displayFile(sb, GROUNDED_FILE_OPTION, groundedFile);
		displayFile(sb, INIT_PARAMS_FILE_OPTION, initParamsFile);
		displayFile(sb, PARAMS_FILE_OPTION, paramsFile);
		displayFile(sb, SOLUTIONS_FILE_OPTION, solutionsFile);
		displayFile(sb, GRADIENT_FILE_OPTION, gradientFile);
		if (!maintainOrder) display(sb, "Output order","reordered");
		if (this.programFiles != null) {
			display(sb, "Duplicate checking", duplicates>0? ("up to "+duplicates) : "off");
		}
		display(sb, THREADS_CONST_OPTION,nthreads);
		return sb.toString();
	}
	private void displayFile(StringBuilder sb, String name, File f) {
		if (f != null) sb.append("\n")
		.append(String.format("%"+(FORMAT_WIDTH-5)+"s file: ", name))
		.append(f.getPath());
	}
	private void display(StringBuilder sb, String name, Object value) {
		sb.append("\n")
		.append(String.format("%"+(FORMAT_WIDTH)+"s: %s",name,value.toString()));
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
