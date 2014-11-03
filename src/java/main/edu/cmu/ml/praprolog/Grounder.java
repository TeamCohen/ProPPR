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

import edu.cmu.ml.praprolog.examples.PosNegRWExample;
import edu.cmu.ml.praprolog.examples.InferenceExampleStreamer;
import edu.cmu.ml.praprolog.examples.InferenceExample;
import edu.cmu.ml.praprolog.prove.wam.Goal;
import edu.cmu.ml.praprolog.prove.wam.LogicProgramException;
import edu.cmu.ml.praprolog.prove.wam.ProofGraph;
import edu.cmu.ml.praprolog.prove.wam.Query;
import edu.cmu.ml.praprolog.prove.wam.State;
import edu.cmu.ml.praprolog.prove.wam.WamProgram;
import edu.cmu.ml.praprolog.prove.wam.plugins.WamPlugin;
import edu.cmu.ml.praprolog.prove.Prover;
import edu.cmu.ml.praprolog.util.Configuration;
import edu.cmu.ml.praprolog.util.CustomConfiguration;
import edu.cmu.ml.praprolog.util.Dictionary;

/**
 * Exports a graph-based example for each raw example in a data file.
 * 
 * The conversion process is as follows:
 *     (read)-> raw example ->(thaw)-> thawed example ->(ground)-> grounded example
 * @author wcohen,krivard
 *
 */
public class Grounder {
	private static final Logger log = Logger.getLogger(Grounder.class);
	public static final String GROUNDED_SUFFIX = ".grounded";
	protected File graphKeyFile=null;
	protected Writer graphKeyWriter=null;
	protected GroundingStatistics statistics=null;

	protected Prover prover;
	protected WamProgram masterProgram;
	protected WamPlugin[] masterPlugins;
	
	public Grounder(Prover p, WamProgram program, WamPlugin ... plugins) {
		this.prover = p;
		this.masterProgram = program;
		this.masterPlugins = plugins;
	}
	
	public class GroundingStatistics {
	    public GroundingStatistics() {
	    	log.info("Resetting grounding statistics...");
	    }
		// statistics
		int totalPos=0, totalNeg=0, coveredPos=0, coveredNeg=0;
		InferenceExample worstX = null;
		double smallestFractionCovered = 1.0;
		int nwritten=0;
		
		protected synchronized void updateStatistics(InferenceExample ex,int npos,int nneg,int covpos,int covneg) {
			// keep track of some statistics - synchronized for multithreading
			totalPos += npos;
			totalNeg += nneg;
			coveredPos += covpos;
			coveredNeg += covneg;
			double fractionCovered = covpos/(double)npos;
			if (fractionCovered < smallestFractionCovered) {
				worstX = ex;
				smallestFractionCovered = fractionCovered;
			}
		}
	}

	public void groundExamples(File dataFile, String outputFile) {
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(outputFile));
			if (this.graphKeyFile != null) this.graphKeyWriter = new BufferedWriter(new FileWriter(this.graphKeyFile));
			groundExamples(dataFile,writer); 
			writer.close();
			if (this.graphKeyFile != null) this.graphKeyWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	/** Single-threaded baseline method to ground examples.
	 */

	public void groundExamples(File dataFile, Writer writer) throws IOException {
		this.statistics = new GroundingStatistics();
		int k=0, empty=0;
		for (InferenceExample inf : new InferenceExampleStreamer(dataFile).stream()) {
			k++;
			try {
				ProofGraph pg = new ProofGraph(inf,this.masterProgram,this.masterPlugins);
				PosNegRWExample<State> x = groundExample(pg);
				if (x.getGraph().edgeSize() > 0) writer.write(serializeGroundedExample(pg, x));
				else { log.warn("Empty graph for example "+k); empty++; }
			} catch(RuntimeException e) {
				log.error("from example line "+k,e);
			} catch (LogicProgramException e) {
				log.error("logic program exception on example line "+k,e);
			}
		}
		if (empty>0) log.info("Skipped "+empty+" of "+k+" examples due to empty graphs");
	}

	long lastPrint = System.currentTimeMillis();

	protected String serializeGroundedExample(ProofGraph pg, PosNegRWExample<State> x) {
		if (log.isInfoEnabled()) {
			statistics.nwritten++;
			long now = System.currentTimeMillis();
			if (now-lastPrint > 5000) {
				log.info("Grounded "+statistics.nwritten+" examples");
				lastPrint = now;
			}
		}

		if (x.length() == 0) {
			log.warn("No positive or negative solutions for query "+statistics.nwritten+":"+pg.getExample().getQuery().toString()+"; skipping");
			return "";
		}

		return pg.serialize(x);
	}

//	protected String serializeGraphKey(InferenceExample ex, ProofGraph pg) {
//		StringBuilder key = new StringBuilder();
//		String s = ex.getQuery().toString();
//		ArrayList<Object> states = pg.
//		for (int i=1; i<states.size(); i++) {
//			key.append(s)
//			.append("\t")
//			.append(i)
//			.append("\t")
//			.append((State) states.get(i))
//			.append("\n");
//		}
//		return key.toString();
//	}

//	protected void saveGraphKey(InferenceExample rawX, GraphWriter writer) {
//		try {
//			this.graphKeyWriter.write(serializeGraphKey(rawX,writer));
//		} catch (IOException e) {
//			throw new IllegalStateException("Couldn't write to graph key file "+this.graphKeyFile.getName(),e);
//		}
//	}

	protected Prover getProver() {
		return this.prover;
	}


	/**
	 * Run the prover to convert a raw example to a random-walk example
	 * @param rawX
	 * @return
	 * @throws LogicProgramException 
	 */
	public PosNegRWExample<State> groundExample(ProofGraph pg) throws LogicProgramException {
		if (log.isTraceEnabled())
			log.trace("thawed example: "+pg.getExample().toString());
		Map<State,Double> ans = this.getProver().prove(pg);
		PosNegRWExample<State> ground = pg.makeRWExample(ans);
		InferenceExample ex = pg.getExample();
		statistics.updateStatistics(ex,
				ex.getPosSet().length,ex.getNegSet().length,
				ground.getPosList().size(),ground.getNegList().size());
//		if (this.graphKeyFile!= null) { saveGraphKey(rawX, writer); }
		return ground;
	}

	protected void reportStatistics(int empty) {
		if (empty>0) log.info("Skipped "+empty+" examples due to empty graphs");
		log.info("totalPos: " + statistics.totalPos 
				+ " totalNeg: "+statistics.totalNeg
				+" coveredPos: "+statistics.coveredPos
				+" coveredNeg: "+statistics.coveredNeg);
		if (statistics.totalPos>0) 
			log.info("For positive examples " + statistics.coveredPos 
				+ "/" + statistics.totalPos 
				+ " proveable [" + ((100.0*statistics.coveredPos)/statistics.totalPos) + "%]");
		if (statistics.totalNeg>0) 
			log.info("For negative examples " + statistics.coveredNeg 
					+ "/" + statistics.totalNeg 
					+ " proveable [" + ((100.0*statistics.coveredNeg)/statistics.totalNeg) + "%]");
		if (statistics.worstX!=null) 
			log.info("Example with fewest ["+100.0*statistics.smallestFractionCovered+"%] pos examples covered: "
					+ statistics.worstX.getQuery());
	}

	public static class ExampleGrounderConfiguration extends CustomConfiguration {
		private File keyFile;
		public ExampleGrounderConfiguration(String[] args, int flags) {
			super(args, flags);
		}

		@Override
		protected void addCustomOptions(Options options, int flags) {
			options.addOption(OptionBuilder
					.withLongOpt("graphKey")
					.withArgName("keyFile")
					.hasArg()
					.withDescription("Save a key to the grounded graphs providing the LogicProgramState definitions of the numbered nodes")
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
		ExampleGrounderConfiguration c = new ExampleGrounderConfiguration(args, flags);
		if (c.programFiles == null) Configuration.missing(Configuration.USE_PROGRAMFILES,flags);

		Grounder grounder = null;
		if (c.nthreads < 0) grounder = new Grounder(c.prover,c.program,c.plugins);
//		else grounder = new ModularMultiExampleGrounder(c.prover, new LogicProgram(Component.loadComponents(c.programFiles,c.alpha,c)), c.nthreads); 
		//MultithreadedExampleGrounder(c.prover,c.programFiles,c.nthreads);
		long start = System.currentTimeMillis();
		if (c.getCustomSetting("graphKey") != null) grounder.useGraphKeyFile((File) c.getCustomSetting("graphKey"));
		grounder.groundExamples(c.dataFile, c.outputFile);
		System.out.println("Time "+(System.currentTimeMillis()-start) + " msec");
		System.out.println("Done.");

	}

	public void useGraphKeyFile(File keyFile) {
		log.info("Using graph key file "+keyFile.getName());
		this.graphKeyFile = keyFile;
	}
}
