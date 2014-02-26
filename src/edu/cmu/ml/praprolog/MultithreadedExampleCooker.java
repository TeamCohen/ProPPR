package edu.cmu.ml.praprolog;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;

import edu.cmu.ml.praprolog.learn.PosNegRWExample;
import edu.cmu.ml.praprolog.prove.LogicProgram;
import edu.cmu.ml.praprolog.prove.Prover;
import edu.cmu.ml.praprolog.prove.RawPosNegExample;
import edu.cmu.ml.praprolog.prove.RawPosNegExampleStreamer;

public class MultithreadedExampleCooker extends ExampleCooker {
	private static final Logger log = Logger.getLogger(MultithreadedExampleCooker.class);
	protected int nthreads;
	public MultithreadedExampleCooker(Prover p, LogicProgram program, int nt) {
		super(p, program);
		this.nthreads = nt;
	}

	@Override
	protected Prover getProver() {
		// for threadsafe backtraces
		return this.prover.copy();
	}
	
	@Override
	public void cookExamples(String dataFile, Writer writer) throws IOException {
		ExecutorService executor = Executors.newFixedThreadPool(this.nthreads);
		
		// using a deque for ease of garbage collection of already-written futures
		ArrayDeque<Future<ExampleCookingResult>> futures = new ArrayDeque<Future<ExampleCookingResult>>();
		
		// only works if entire raw file fits in memory
//		List<RawPosNegExample> rawExamples = new RawPosNegExampleStreamer(dataFile).load(); 
		long start = System.currentTimeMillis();
		for (RawPosNegExample rawX : new RawPosNegExampleStreamer(dataFile).stream()) {
			futures.add(executor.submit(new CookerThread(rawX,this,nextId(),log)));
		}
		executor.shutdown();
		int id=0, empty=0;
		while(futures.size() > 0) {
			Future<ExampleCookingResult> f = futures.pop();
			ExampleCookingResult result;
			try {
				if (log.isDebugEnabled()) log.debug("Asking "+id+++" "+System.currentTimeMillis()+" "+Thread.currentThread().getName());
				result = f.get(); // blocking call
				if (log.isDebugEnabled()) log.debug("Got "+result.id+" "+System.currentTimeMillis()+" "+Thread.currentThread().getName());
				if (result.cookedExample.getGraph().getNumEdges() > 0) 
					writer.write(this.serializeCookedExample(result.rawX, result.cookedExample));
				else { log.warn("Empty graph for example "+id); empty++; }
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
		if (log.isDebugEnabled()) log.debug("Cooking-only "+(System.currentTimeMillis() - start));

		if (empty>0) log.info("Skipped "+empty+" of "+id+" examples due to empty graphs");
	}
	
	protected static int nextId=0;
	protected static int nextId() {
		return nextId++;
	}
	public class ExampleCookingResult {
		public RawPosNegExample rawX;
		public PosNegRWExample<String> cookedExample;
		public int id;

		public ExampleCookingResult(int id, RawPosNegExample rawX,
				PosNegRWExample<String> cookExample) {
			this.rawX = rawX;
			this.cookedExample = cookExample;
			this.id = id;
		}
		
	}
	public class CookerThread implements Callable<ExampleCookingResult> {
		RawPosNegExample rawX;
		ExampleCooker cooker;
		int id;
		Logger log;
		public CookerThread(RawPosNegExample rawX,ExampleCooker cooker, int id,Logger log) {
			this.rawX = rawX;
			this.cooker = cooker;
			this.id = id;
			this.log = log;
			if (log.isDebugEnabled()) log.debug("Created "+this.id+" "+System.currentTimeMillis()+" "+Thread.currentThread().getName());
		}
		public ExampleCookingResult call() {
			if (log.isDebugEnabled()) log.debug("Calling "+this.id+" "+System.currentTimeMillis()+" "+Thread.currentThread().getName());
			try {
				ExampleCookingResult result = new ExampleCookingResult(this.id, this.rawX, cooker.cookExample(this.rawX, cooker.masterProgram));
				if (log.isDebugEnabled()) log.debug("Finished "+this.id+" "+System.currentTimeMillis()+" "+Thread.currentThread().getName());
				return result;
			} catch(RuntimeException e) {
				throw new RuntimeException("on example "+this.id,e);
			}
		}
	}
}
