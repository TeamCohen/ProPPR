package edu.cmu.ml.praprolog;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.cmu.ml.praprolog.graph.GraphWriter;
import edu.cmu.ml.praprolog.learn.PosNegRWExample;
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
	public ExampleCooker(Prover p, String[] programFiles, double alpha) {
		super.init(p,new LogicProgram(Component.loadComponents(programFiles,alpha)));
	}
	
	public void cookExamples(String dataFile, String outputFile) {
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(outputFile));
			cookExamples(dataFile,writer); 
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void cookExamples(String dataFile, Writer writer) throws IOException {
		int k=0, empty=0;
		for (RawPosNegExample rawX : new RawPosNegExampleStreamer(dataFile).load()) {
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
		Map<LogicProgramState,Double> ans = this.prover.proveState(program, x.getQueryState(), writer);
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
		return new PosNegRWExample<String>(writer.getGraph(), queryVector, posIds, negIds);
	}

	
	public static void main(String ... args) {
		Configuration c = new Configuration(args, Configuration.USE_DEFAULTS | Configuration.USE_DATA | Configuration.USE_OUTPUT);
		
		ExampleCooker cooker = null;
		if (c.nthreads < 0) cooker = new ExampleCooker(c.prover,c.programFiles,c.alpha);
		else cooker = new ModularMultiExampleCooker(c.prover, c.programFiles, c.alpha, c.nthreads); 
				//MultithreadedExampleCooker(c.prover,c.programFiles,c.nthreads);
		long start = System.currentTimeMillis();
		cooker.cookExamples(c.dataFile, c.outputFile);
		System.out.println(System.currentTimeMillis()-start);
		System.out.println("Done.");
		
	}
}
