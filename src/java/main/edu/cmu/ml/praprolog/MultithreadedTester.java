package edu.cmu.ml.praprolog;

import java.io.File;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;

import edu.cmu.ml.praprolog.Tester.ExampleSolutionScore;
import edu.cmu.ml.praprolog.Tester.TestResults;
import edu.cmu.ml.praprolog.prove.v1.LogicProgram;
import edu.cmu.ml.praprolog.prove.v1.LogicProgramState;
import edu.cmu.ml.praprolog.prove.v1.Prover;
import edu.cmu.ml.praprolog.prove.v1.RawPosNegExample;
import edu.cmu.ml.praprolog.prove.v1.RawPosNegExampleStreamer;
import edu.cmu.ml.praprolog.prove.v1.ThawedPosNegExample;

public class MultithreadedTester extends Tester {
	private static final Logger log = Logger.getLogger(MultithreadedTester.class);
	protected int nthreads;

	public MultithreadedTester(Prover p, LogicProgram lp, int threads) {
		super(p, lp);
		this.nthreads = threads;
	}
	
	@Override
	public TestResults testExamples(File dataFile, boolean strict) {
		ExecutorService executor = Executors.newFixedThreadPool(this.nthreads);
		ArrayDeque<Future<ExampleSolutionScore>> futures = new ArrayDeque<Future<ExampleSolutionScore>>();
		
		int k=0;
		double pairTotal=0,pairErrors=0,apTotal=0,numAP=0;
		List<RawPosNegExample> examples = new RawPosNegExampleStreamer(dataFile).load();
		for (RawPosNegExample rawX : examples) {
			k++;
			futures.add(executor.submit(new TesterThread(rawX,this.masterProgram,k)));
		}
		executor.shutdown();
		int id=0;
		long start = System.currentTimeMillis();
		while(futures.size() > 0) {
			Future<ExampleSolutionScore> f = futures.pop();
			try {
				id++;
				if((System.currentTimeMillis() - start)>2000 ) log.info(id+" examples tested...");
				ExampleSolutionScore x = f.get();
				pairTotal+=x.numPairs;
				pairErrors+=x.numErrors;
				apTotal+=x.averagePrecision;
				numAP++;
			} catch(InterruptedException e) {
				if (!strict) log.error("from example "+id,e);
				else throw(new RuntimeException("from example "+id,e));
			} catch(ExecutionException e) {
				if (!strict) log.error("from example "+id,e);
				else throw(new RuntimeException("from example "+id,e));
			}
		}
		log.info("pairTotal "+pairTotal+" pairErrors "+pairErrors+" errorRate "+ (pairErrors/pairTotal) +" map "+ (apTotal/numAP) );
		return new TestResults(pairTotal,pairErrors,pairErrors/pairTotal,apTotal/numAP);
	}
	
	Map<Thread,Prover> provers = new TreeMap<Thread,Prover>();
	protected Prover getProver() {
		Thread t = Thread.currentThread();
		if (!provers.containsKey(t)) 
			provers.put(t, this.prover.copy());
		return provers.get(t);
	}
	
	@Override	
	public Map<LogicProgramState,Double> getSolutions(ThawedPosNegExample x,LogicProgram program) {
		return getProver().proveState(program, x.getQueryState(), null);
	}
	
	public class TesterThread implements Callable<ExampleSolutionScore> {
		RawPosNegExample rawX;
		LogicProgram testerProgram;
		int id;
		public TesterThread(RawPosNegExample rx, LogicProgram lp, int id) {
			this.rawX = rx;
			this.testerProgram = lp;
			this.id = id;
		}
		@Override
		public ExampleSolutionScore call() throws Exception {
			try {
				return testExample(rawX, this.testerProgram);
			} catch(RuntimeException e) {
				throw new RuntimeException("on example "+this.id,e);
			}
		}
		
	}

}
