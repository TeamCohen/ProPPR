package edu.cmu.ml.praprolog;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.log4j.Logger;

import edu.cmu.ml.praprolog.graph.GraphWriter;
import edu.cmu.ml.praprolog.learn.tools.PosNegRWExample;
import edu.cmu.ml.praprolog.prove.Component;
import edu.cmu.ml.praprolog.prove.Goal;
import edu.cmu.ml.praprolog.prove.LogicProgram;
import edu.cmu.ml.praprolog.prove.LogicProgramState;
import edu.cmu.ml.praprolog.prove.Prover;
import edu.cmu.ml.praprolog.prove.RawPosNegExample;
import edu.cmu.ml.praprolog.prove.RawPosNegExampleStreamer;
import edu.cmu.ml.praprolog.prove.ThawedPosNegExample;
import edu.cmu.ml.praprolog.prove.TracingDfsProver;
import edu.cmu.ml.praprolog.util.Configuration;
import edu.cmu.ml.praprolog.util.CustomConfiguration;
import edu.cmu.ml.praprolog.util.Dictionary;

/**
 * Exports a graph-based example for each raw example in a data file.
 * 
 * The conversion process is as follows:
 *     (read)-> raw example ->(thaw)-> thawed example ->(cook)-> cooked example
 * @author wcohen,krivard
 *
 */
public class ExampleCooker extends ExampleThawing {
	private static final Logger log = Logger.getLogger(ExampleCooker.class);
	public static final String COOKED_SUFFIX = ".cooked";
	protected File graphKeyFile=null;
	protected Writer graphKeyWriter=null;
	public ExampleCooker(Prover p, LogicProgram program) {
		super.init(p,program);
	}
	
	public void cookExamples(File dataFile, String outputFile) {
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(outputFile));
			if (this.graphKeyFile != null) this.graphKeyWriter = new BufferedWriter(new FileWriter(this.graphKeyFile));
			cookExamples(dataFile,writer); 
			writer.close();
			if (this.graphKeyFile != null) this.graphKeyWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	

    /** Single-threaded baseline method to ground examples.
     */

	public void cookExamples(File dataFile, Writer writer) throws IOException {
		int k=0, empty=0;
		for (RawPosNegExample rawX : new RawPosNegExampleStreamer(dataFile).stream()) {
			k++;
//			log.debug("raw example: "+rawX.getQuery()+" "+rawX.getPosList()+" "+rawX.getNegList());
			try {	
				if (log.isDebugEnabled()) {
					log.debug("Free Memory Created "+k+" "+Runtime.getRuntime().freeMemory()+" / "+Runtime.getRuntime().totalMemory()+" "+System.currentTimeMillis());
					log.debug("Created "+k+" "+System.currentTimeMillis()+" "+Thread.currentThread().getName());
					log.debug("Calling "+k+" "+System.currentTimeMillis()+" "+Thread.currentThread().getName());
				}
				PosNegRWExample<String> x = cookExample(rawX, this.masterProgram);
				if (log.isDebugEnabled()) { 
					log.debug("Finished "+k+" "+System.currentTimeMillis()+" "+Thread.currentThread().getName());
					log.debug("Asking "+k+" "+System.currentTimeMillis()+" "+Thread.currentThread().getName());
					log.debug("Free Memory Got "+k+" "+Runtime.getRuntime().freeMemory()+" / "+Runtime.getRuntime().totalMemory()+" "+System.currentTimeMillis());
					log.debug("Got "+k+" "+System.currentTimeMillis()+" "+Thread.currentThread().getName());
				}
				if (x.getGraph().getNumEdges() > 0) writer.write(serializeCookedExample(rawX, x));
				else { log.warn("Empty graph for example "+k); empty++; }
				if (log.isDebugEnabled()) {
					log.debug("Free Memory Wrote "+k+" "+Runtime.getRuntime().freeMemory()+" / "+Runtime.getRuntime().totalMemory()+" "+System.currentTimeMillis());
					log.debug("Wrote "+k+" "+System.currentTimeMillis()+" "+Thread.currentThread().getName());
				}
			} catch(RuntimeException e) {
				log.error("from example line "+k,e);
			}
		}
		if (empty>0) log.info("Skipped "+empty+" of "+k+" examples due to empty graphs");
	}
	
	long lastPrint = System.currentTimeMillis();
	int nwritten=0;

	protected String serializeCookedExample(RawPosNegExample rawX, PosNegRWExample<String> x) {
		
		if (log.isInfoEnabled()) {
			nwritten++;
			long now = System.currentTimeMillis();
			if (now-lastPrint > 5000) {
				log.info("Cooked "+nwritten+" examples");
				lastPrint = now;
			}
		}
		
		if (x.length() == 0) {
			log.warn("No positive or negative solutions for query "+nwritten+":"+rawX.getQuery().toSaveString()+"; skipping");
			return "";
		}
		
		StringBuilder line = new StringBuilder();
		line.append(rawX.getQuery().toSaveString())
			.append("\t");
		Dictionary.buildString(x.getQueryVec().keySet(), line, ",");
		line.append("\t");
		Dictionary.buildString(x.getPosList(), line, ",");
		line.append("\t");
		Dictionary.buildString(x.getNegList(), line, ",");
		line.append("\t")
			.append(x.getGraph().toString())
			.append("\n");
		return line.toString();
	}
	
	protected String serializeGraphKey(RawPosNegExample rawX, GraphWriter gw) {
		StringBuilder key = new StringBuilder();
		String s = rawX.getQuery().toSaveString();
		ArrayList<Object> states = gw.getNodes();
		for (int i=1; i<states.size(); i++) {
			key.append(s)
				.append("\t")
				.append(i)
				.append("\t")
				.append((LogicProgramState) states.get(i))
				.append("\n");
		}
		return key.toString();
	}
	
	protected void saveGraphKey(RawPosNegExample rawX, GraphWriter writer) {
		try {
			this.graphKeyWriter.write(serializeGraphKey(rawX,writer));
		} catch (IOException e) {
			throw new IllegalStateException("Couldn't write to graph key file "+this.graphKeyFile.getName(),e);
		}
	}
	
	protected Prover getProver() {
		return this.prover;
	}
	

	/**
	 * Run the prover to convert a raw example to a random-walk example
	 * @param rawX
	 * @return
	 */
	public PosNegRWExample<String> cookExample(RawPosNegExample rawX, LogicProgram program) {
		ThawedPosNegExample x = thawExample(rawX,program);
		if (log.isTraceEnabled())
			log.trace("thawed example: "
					+x.getQueryState()
					+Dictionary.buildString(x.getPosSet(), new StringBuilder(), " -", false).toString()
					+Dictionary.buildString(x.getPosSet(), new StringBuilder(), " +", false).toString());
		GraphWriter writer = new GraphWriter();
		Map<LogicProgramState,Double> ans = this.getProver().proveState(program, x.getQueryState(), writer);
		if (log.isTraceEnabled()) {
			new TracingDfsProver().proveState(new LogicProgram(program), x.getQueryState());
		}
		List<String> posIds = new ArrayList<String>();
		List<String> negIds = new ArrayList<String>();
		for (Map.Entry<LogicProgramState,Double> soln : ans.entrySet()) {
			if (soln.getKey().isSolution()) {
				Goal groundGoal = soln.getKey().getGroundGoal();
				// FIXME: slow?
				if (Arrays.binarySearch(x.getPosSet(), groundGoal) >= 0) posIds.add(writer.getId(soln.getKey()));
				if (Arrays.binarySearch(x.getNegSet(), groundGoal) >= 0) negIds.add(writer.getId(soln.getKey()));
			}
		}
		Map<String,Double> queryVector = new HashMap<String,Double>();
		queryVector.put(writer.getId(x.getQueryState()), 1.0);
		if (this.graphKeyFile!= null) { saveGraphKey(rawX, writer); }
		updateStatistics(rawX,rawX.getPosList().length,rawX.getNegList().length,posIds.size(),negIds.size());
		return new PosNegRWExample<String>(writer.getGraph(), queryVector, posIds, negIds);
	}
	// statistics
        static int totalPos=0, totalNeg=0, coveredPos=0, coveredNeg=0;
        static RawPosNegExample worstX = null;
        static double smallestFractionCovered = 1.0;

        protected synchronized static void updateStatistics(RawPosNegExample rawX,int npos,int nneg,int covpos,int covneg) {
	    // keep track of some statistics - synchronized for multithreading
	    totalPos += npos;
	    totalNeg += nneg;
	    coveredPos += covpos;
	    coveredNeg += covneg;
	    double fractionCovered = covpos/(double)npos;
	    if (fractionCovered < smallestFractionCovered) {
		worstX = rawX;
		smallestFractionCovered = fractionCovered;
	    }
	}
	
        protected void reportStatistics(int empty) {
	    if (empty>0) log.info("Skipped "+empty+" examples due to empty graphs");
	    log.info("totalPos: " + totalPos + " totalNeg: "+totalNeg+" coveredPos: "+coveredPos+" coveredNeg: "+coveredNeg);
	    if (totalPos>0) log.info("For positive examples " + coveredPos + "/" + totalPos + " proveable [" + ((100.0*coveredPos)/totalPos) + "%]");
	    if (totalNeg>0) log.info("For negative examples " + coveredNeg + "/" + totalNeg + " proveable [" + ((100.0*coveredNeg)/totalNeg) + "%]");
	    if (worstX!=null) log.info("Example with fewest ["+100.0*smallestFractionCovered+"%] pos examples covered: "+worstX.getQuery());
	}

	public static class ExampleCookerConfiguration extends CustomConfiguration {
		private File keyFile;
		public ExampleCookerConfiguration(String[] args, int flags) {
			super(args, flags);
		}

		@Override
		protected void addCustomOptions(Options options, int flags) {
			options.addOption(OptionBuilder
					.withLongOpt("graphKey")
					.withArgName("keyFile")
					.hasArg()
					.withDescription("Save a key to the cooked graphs providing the LogicProgramState definitions of the numbered nodes")
					.create());
		}

		@Override
		protected void retrieveCustomSettings(CommandLine line, int flags,
				Options options) {
			if (line.hasOption("graphKey")) this.keyFile = new File(line.getOptionValue("graphKey"));
		}

		@Override
		public Object getCustomSetting(String name) {
			return keyFile;
		}

		
	}
	
	public static void main(String ... args) {
		int flags = Configuration.USE_DEFAULTS | Configuration.USE_DATA | Configuration.USE_OUTPUT;
		ExampleCookerConfiguration c = new ExampleCookerConfiguration(args, flags);
		if (c.programFiles == null) Configuration.missing(Configuration.USE_PROGRAMFILES,flags);
				
		ExampleCooker cooker = null;
		if (c.nthreads < 0) cooker = new ExampleCooker(c.prover,new LogicProgram(Component.loadComponents(c.programFiles,c.alpha)));
		else cooker = new ModularMultiExampleCooker(c.prover, new LogicProgram(Component.loadComponents(c.programFiles,c.alpha)), c.nthreads); 
				//MultithreadedExampleCooker(c.prover,c.programFiles,c.nthreads);
		long start = System.currentTimeMillis();
		if (c.getCustomSetting("graphKey") != null) cooker.useGraphKeyFile((File) c.getCustomSetting("graphKey"));
		cooker.cookExamples(c.dataFile, c.outputFile);
		System.out.println("Time "+(System.currentTimeMillis()-start) + " msec");
		System.out.println("Done.");
		
	}

	public void useGraphKeyFile(File keyFile) {
		log.info("Using graph key file "+keyFile.getName());
		this.graphKeyFile = keyFile;
	}
}
