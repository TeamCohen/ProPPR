package edu.cmu.ml.proppr.v1;

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

import java.util.concurrent.*;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.graph.v1.GraphWriter;
import edu.cmu.ml.proppr.prove.v1.Component;
import edu.cmu.ml.proppr.prove.v1.Goal;
import edu.cmu.ml.proppr.prove.v1.LogicProgram;
import edu.cmu.ml.proppr.prove.v1.LogicProgramState;
import edu.cmu.ml.proppr.prove.v1.Prover;
import edu.cmu.ml.proppr.prove.v1.RawPosNegExample;
import edu.cmu.ml.proppr.prove.v1.RawPosNegExampleStreamer;
import edu.cmu.ml.proppr.prove.v1.ThawedPosNegExample;
import edu.cmu.ml.proppr.prove.v1.TracingDfsProver;
import edu.cmu.ml.proppr.util.Configuration;
import edu.cmu.ml.proppr.util.CustomConfiguration;
import edu.cmu.ml.proppr.util.Dictionary;

/**
 * Exports a graph-based example for each raw example in a data file.
 * 
 * The conversion process is as follows:
 *     (read)-> raw example ->(thaw)-> thawed example ->(cook)-> cooked example
 * @author wcohen,krivard
 *
 */
public class ExampleCookerWC extends ExampleThawing {

    private static final Logger log = Logger.getLogger(ExampleCookerWC.class);

    public ExampleCookerWC(Prover p, LogicProgram program) {
	super.init(p,program);
    }
    protected Prover getProver() { return this.prover; }


    /** Parallel processing utility class - transform inputs (of
       arbitrary type Tin) to strings, and write the results to an
       output file.  Will use one output thread and k input threads.
     */
    abstract public class ParallelStreamTranformerAndWriter<Tin> {

	private Iterable<Tin> inputIt;
	private Writer outputWriter;
	private int numThreads;

	/** This is overridden to do the actual transformation work. **/
	abstract public String transform(Tin in);

	public ParallelStreamTranformerAndWriter(Iterable<Tin> inputIt,Writer outputWriter,int numThreads) {
	    this.inputIt = inputIt;
	    this.outputWriter = outputWriter;
	    this.numThreads = numThreads;
	}

	/** This used to find out how many processes are still working. **/
	private class SynchronizedCounter {
	    private int c = 0;
	    public synchronized void inc() { c++; }
	    public synchronized void dec() { c--; }
	    public synchronized int get() { return c; }
	}

	/** Process everything in the input stream **/

	public void processStream() 
	{

	    log.info("Processing stream with "+numThreads+" worker thread(s)");

	    /* track number of threads actively processing data */
	    final SynchronizedCounter threadsWorking = new SynchronizedCounter();

	    /* transformed inputs are placed on the output queue, and then
	       gobbled up buy the outputConsumer thread */
	    final BlockingQueue<String> outputQueue = new ArrayBlockingQueue(numThreads);
	    ExecutorService outputConsumer = Executors.newFixedThreadPool(1); 
	    outputConsumer.execute(new Runnable() {
		    public void run() {
			try {
			    while (true) {
				String transformed = outputQueue.take();
				threadsWorking.inc();
				//log.info("removed from output queue: "+transformed.substring(0,20));
				outputWriter.write(transformed);
				threadsWorking.dec();
			    }
			} catch (InterruptedException ex) {
			    //log.info("outputConsumer interrupted, probably on shutDown");
			} catch (IOException ex) {
			    ex.printStackTrace();
			}
		    }
		});

	    /* things waiting to be transformed are sent to this thread pool, 
	       and then gobbled up by the transformerPool threads. */
	    final BlockingQueue<Tin> taskQueue = new ArrayBlockingQueue(numThreads);
	    ExecutorService transformerPool = Executors.newFixedThreadPool(numThreads);
	    for (int i=0; i<numThreads; i++) {
		transformerPool.execute(new Runnable() {
			public void run() {
			    try {
				while (true) {
				    Tin x = taskQueue.take();
				    threadsWorking.inc();
				    //log.info("removed from input queue: "+x.toString());
				    String result = transform(x);
				    if (result != null) {
					//log.info("added to output queue: "+result.substring(0,20));
					outputQueue.put(result);
				    }
				    threadsWorking.dec();
				}
			    } catch (InterruptedException ex) {
				//log.info("transformerPool worker interrupted, probably on shutDown");
			    }
			}
		    });
	    }

	    /* Since we're using a blocking queue, this will block
	     * when there are numThreads things waiting to be
	     * processed.
	     */

	    try {
		for (Tin x : inputIt) {
		    //log.info("added to input queue: "+x.toString());
		    taskQueue.put(x);
		}
	    } catch (InterruptedException ex) {
		log.warn("Unexpected InterruptedException adding to input queue");
		ex.printStackTrace();
	    }
	    
	    /* There might still be O(numThreads) tasks waiting for
	     * transformation or output.
	     */

	    log.info("waiting for queues to clear");
	    while ((taskQueue.peek()!=null) || (outputQueue.peek()!=null) || threadsWorking.get()>0) {
		try {
		    int nt = taskQueue.size();
		    int no = outputQueue.size();
		    int nw = threadsWorking.get();
		    log.info(nt + " tasks "+no+" outputs waiting "+nw+" threads working ");
		    Thread.sleep(50 * nw);
		} catch (InterruptedException ex) {
		    log.warn("Unexpected InterruptedException waiting for queues to clear");
		    ex.printStackTrace();
		}
	    }
	    log.info("work is done, shutting down worker threads");
	    transformerPool.shutdownNow();
	    outputConsumer.shutdownNow();
	    log.info("done");
	}
    }


    /** Single-threaded baseline method to ground examples.
     */

    public void cookExamplesSingle(File dataFile, Writer outputWriter) throws IOException {
	int k=0;
	for (RawPosNegExample rawX : new RawPosNegExampleStreamer(dataFile).stream()) {
	    k++;
	    try {	
		PosNegRWExample<String> x = cookExample(rawX, this.masterProgram);
		if (x.getGraph().getNumEdges() > 0) {
		    outputWriter.write(serializeCookedExample(rawX, x));
		}
	    } catch(RuntimeException e) {
		log.error("from example line "+k,e);
	    }
	}
    }
	
    /** Multi-threaded baseline method to ground examples.
     */

    public class ParallelCooker extends ParallelStreamTranformerAndWriter<RawPosNegExample> {
	LogicProgram program;
	public ParallelCooker(Iterable<RawPosNegExample> ins,Writer outw,int numThreads, LogicProgram program) {
	    super(ins,outw,numThreads);
	    this.program = program;
	}
	public String transform(RawPosNegExample rawX) {
	    PosNegRWExample<String> x = cookExample(rawX,program);
	    if (x.getGraph().getNumEdges() > 0) {
		return serializeCookedExample(rawX, x);
	    } else {
		return null;
	    }
	}
    }

    public void cookExamples(File dataFile, Writer outputWriter, int numThreads) {
	Iterable<RawPosNegExample> inStream = new RawPosNegExampleStreamer(dataFile).stream();
	ParallelCooker pcooker = new ParallelCooker(inStream,outputWriter,numThreads,this.masterProgram);
	pcooker.processStream();
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
	GraphWriter graphWriter = new GraphWriter();
	Map<LogicProgramState,Double> ans = this.getProver().proveState(program, x.getQueryState(), graphWriter);
	if (log.isTraceEnabled()) {
	    new TracingDfsProver().proveState(new LogicProgram(program), x.getQueryState());
	}
	List<String> posIds = new ArrayList<String>();
	List<String> negIds = new ArrayList<String>();
	for (Map.Entry<LogicProgramState,Double> soln : ans.entrySet()) {
	    if (soln.getKey().isSolution()) {
		Goal groundGoal = soln.getKey().getGroundGoal();
		// FIXME: slow?
		if (Arrays.binarySearch(x.getPosSet(), groundGoal) >= 0) posIds.add(graphWriter.getId(soln.getKey()));
		if (Arrays.binarySearch(x.getNegSet(), groundGoal) >= 0) negIds.add(graphWriter.getId(soln.getKey()));
	    }
	}
	Map<String,Double> queryVector = new HashMap<String,Double>();
	queryVector.put(graphWriter.getId(x.getQueryState()), 1.0);
	boolean isEmpty = graphWriter.getGraph().getNumEdges()==0;
	updateStatistics(rawX,rawX.getPosList().length,rawX.getNegList().length,posIds.size(),negIds.size(),isEmpty);
	return new PosNegRWExample<String>(graphWriter.getGraph(), queryVector, posIds, negIds);
    }

    protected String serializeCookedExample(RawPosNegExample rawX, PosNegRWExample<String> x) {
		
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
	

    // statistics - should probably be synchronized
    int numExamples=0,totalPos=0, totalNeg=0, coveredPos=0, coveredNeg=0, numEmpty=0;
    RawPosNegExample worstX = null;
    double smallestFractionCovered = 1.0;

    protected synchronized void updateStatistics(RawPosNegExample rawX,int npos,int nneg,int covpos,int covneg,boolean isEmpty) {
	// keep track of some statistics - synchronized for multithreading
	numExamples ++;
	if (isEmpty) numEmpty++;
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
	
    protected void reportStatistics() {
	log.info("Cooked "+numExamples+" examples");
	if (numEmpty>0) log.info("Skipped "+numEmpty+"/"+numExamples+" examples due to empty graphs");
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
	@Override protected void addCustomOptions(Options options, int flags) {
	    options.addOption(OptionBuilder
			      .withLongOpt("graphKey")
			      .withArgName("keyFile")
			      .hasArg()
			      .withDescription("Save a key to the cooked graphs providing the LogicProgramState definitions of the numbered nodes")
			      .create());
	}
	@Override protected void retrieveCustomSettings(CommandLine line, int flags, Options options) {
	    if (line.hasOption("graphKey")) this.keyFile = new File(line.getOptionValue("graphKey"));
	}
	@Override public Object getCustomSetting(String name) {
	    return keyFile;
	}
    }
	
    public static void main(String ... args) throws IOException {
	int flags = Configuration.USE_DEFAULTS | Configuration.USE_DATA | Configuration.USE_OUTPUT;
	ExampleCookerConfiguration c = new ExampleCookerConfiguration(args, flags);
	if (c.programFiles == null) Configuration.missing(Configuration.USE_PROGRAMFILES,flags);
				
	ExampleCookerWC cooker = new ExampleCookerWC(c.prover, new LogicProgram(Component.loadComponents(c.programFiles,c.alpha,c)));
	long start = System.currentTimeMillis();
	Writer outputWriter = new BufferedWriter(new FileWriter(c.outputFile));
	cooker.cookExamples(c.dataFile, outputWriter, c.nthreads >= 1 ? c.nthreads : 1);
	cooker.reportStatistics();
	outputWriter.close();
	System.out.println("Time "+(System.currentTimeMillis()-start) + " msec");
	System.out.println("Done.");
    }
}
