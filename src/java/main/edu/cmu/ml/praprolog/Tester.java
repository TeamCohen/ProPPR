package edu.cmu.ml.praprolog;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import edu.cmu.ml.praprolog.learn.tools.WeightingScheme;
import edu.cmu.ml.praprolog.prove.Goal;
import edu.cmu.ml.praprolog.prove.InnerProductWeighter;
import edu.cmu.ml.praprolog.prove.LogicProgram;
import edu.cmu.ml.praprolog.prove.LogicProgramState;
import edu.cmu.ml.praprolog.prove.Prover;
import edu.cmu.ml.praprolog.prove.RawPosNegExample;
import edu.cmu.ml.praprolog.prove.RawPosNegExampleStreamer;
import edu.cmu.ml.praprolog.prove.ThawedPosNegExample;
import edu.cmu.ml.praprolog.prove.TracingDfsProver;
import edu.cmu.ml.praprolog.util.Configuration;
import edu.cmu.ml.praprolog.util.Dictionary;
import edu.cmu.ml.praprolog.util.ExperimentConfiguration;
import edu.cmu.ml.praprolog.util.ParamVector;
import edu.cmu.ml.praprolog.util.ParamsFile;
import edu.cmu.ml.praprolog.util.SimpleParamVector;

public class Tester extends ExampleThawing {
	private static final Logger log = Logger.getLogger(Tester.class);
	public Tester(Prover p, LogicProgram lp) {
		super.init(p,lp);
	}
	
	public class TestResults {
		public double pairTotal;
		public double pairErrors;
		public double errorRate;
		public double map;
		public TestResults(double pt, double pe, double er, double m) {
			this.pairTotal = pt;
			this.pairErrors = pe;
			this.errorRate = er;
			this.map = m;
		}
	}
	public TestResults testExamples(File testFile) { return testExamples(testFile,false); }
	public TestResults testExamples(File dataFile, boolean strict) {
		int k=0;
		double pairTotal=0,pairErrors=0,apTotal=0,numAP=0;
//		List<RawPosNegExample> examples = new RawPosNegExampleStreamer(dataFile).load();
		for (RawPosNegExample rawX : new RawPosNegExampleStreamer(dataFile).stream()) {
			k++;
//			log.debug("raw example: "+rawX.getQuery()+" "+rawX.getPosList()+" "+rawX.getNegList());
			try {	
//				if (log.isDebugEnabled()) {
//					log.debug("Free Memory Created "+k+" "+Runtime.getRuntime().freeMemory()+" / "+Runtime.getRuntime().totalMemory()+" "+System.currentTimeMillis());
//					log.debug("Created "+k+" "+System.currentTimeMillis()+" "+Thread.currentThread().getName());
//					log.debug("Calling "+k+" "+System.currentTimeMillis()+" "+Thread.currentThread().getName());
//				}
				
				ExampleSolutionScore x = testExample(rawX, this.masterProgram);
				pairTotal+=x.numPairs;
				pairErrors+=x.numErrors;
				apTotal+=x.averagePrecision;
				numAP++;
//				if (log.isDebugEnabled()) { 
//					log.debug("Finished "+k+" "+System.currentTimeMillis()+" "+Thread.currentThread().getName());
//					log.debug("Asking "+k+" "+System.currentTimeMillis()+" "+Thread.currentThread().getName());
//					log.debug("Free Memory Got "+k+" "+Runtime.getRuntime().freeMemory()+" / "+Runtime.getRuntime().totalMemory()+" "+System.currentTimeMillis());
//					log.debug("Got "+k+" "+System.currentTimeMillis()+" "+Thread.currentThread().getName());
//				}
//				writer.write(serializeCookedExample(rawX, x));
//				if (log.isDebugEnabled()) {
//					log.debug("Free Memory Wrote "+k+" "+Runtime.getRuntime().freeMemory()+" / "+Runtime.getRuntime().totalMemory()+" "+System.currentTimeMillis());
//					log.debug("Wrote "+k+" "+System.currentTimeMillis()+" "+Thread.currentThread().getName());
//				}
			} catch(RuntimeException e) {
				if (!strict) log.error("from example line "+k,e);
				else throw(new RuntimeException("from example line "+k,e));
			}
		}
		log.info("pairTotal "+pairTotal+" pairErrors "+pairErrors+" errorRate "+ (pairErrors/pairTotal) +" map "+ (apTotal/numAP) );
		return new TestResults(pairTotal,pairErrors,pairErrors/pairTotal,apTotal/numAP);
	}
	
	public ExampleSolutionScore testExample(RawPosNegExample rawX, LogicProgram program) {
		ThawedPosNegExample x = thawExample(rawX,program);
		if (log.isTraceEnabled())
			log.trace("thawed example: "
					+x.getQueryState()
					+Dictionary.buildString(x.getNegSet(), new StringBuilder(), " -", false).toString()
					+Dictionary.buildString(x.getPosSet(), new StringBuilder(), " +", false).toString());
		else if (log.isDebugEnabled()) log.debug("Query: "+x.getQueryState());
		Map<LogicProgramState,Double> ans = getSolutions(x,program);
		if (log.isTraceEnabled()) {
			new TracingDfsProver().proveState(new LogicProgram(program), x.getQueryState());
		}

		Map<Goal,Double> solnScore = new HashMap<Goal,Double>();
		double last=0.0;int diffcount=-1,solcount=0;
		for (Map.Entry<LogicProgramState,Double> soln : ans.entrySet()) {
			if (soln.getKey().isSolution()) {
				Goal groundGoal = soln.getKey().getGroundGoal();
				solnScore.put(groundGoal, soln.getValue());
				if (soln.getValue() != last) diffcount++;
				last = soln.getValue();
				if (log.isDebugEnabled()) log.debug("Solution: ("+soln.getValue()+") "+soln.getKey());
			}
		}
		if (solnScore.size() > 1 && diffcount==0) log.warn("All answers ranked equally for query "+rawX.getQuery());
		ExampleSolutionScore s = new ExampleSolutionScore();
		for (Goal p : x.getPosSet()) {
			for (Goal n : x.getNegSet()) {
				s.numPairs += 1;
                if (Dictionary.safeGet(solnScore,p,0.0) <= Dictionary.safeGet(solnScore, n, 0.0)) s.numErrors += 1;
			}
		}
		s.averagePrecision = averagePrecision(solnScore,x.getPosSet());
		return s;
	}
	
	public Map<LogicProgramState,Double> getSolutions(ThawedPosNegExample x,LogicProgram program) {
		return this.prover.proveState(program, x.getQueryState(), null);
	}
	
	private int maxTraced=10;
	private static final Comparator<Map.Entry<Goal,Double>> SCORE_ORDER = new Comparator<Map.Entry<Goal,Double>>() {
		@Override public int compare(Entry<Goal, Double> arg0,
				Entry<Goal, Double> arg1) {
			return arg1.getValue().compareTo(arg0.getValue());
		}};
	protected double averagePrecision(Map<Goal, Double> solnScore, Goal[] posSet) {
		double rank = 0;
		double numFP = 0;
		double totPrec = 0.0;
		int numPosSeen = 0;
		List<Map.Entry<Goal,Double>> scores = new ArrayList<Map.Entry<Goal,Double>>();
		scores.addAll(solnScore.entrySet());
		Collections.sort(scores,SCORE_ORDER);
		for (Map.Entry<Goal,Double> score : scores) {
			rank++;
			boolean correct = Arrays.binarySearch(posSet, score.getKey())>=0;
			if (correct) {
				totPrec += ((rank - numFP)) / rank; 
				numPosSeen++;
			} else {
				numFP++;
			}
			if (log.isTraceEnabled() && (rank < maxTraced || correct))
				log.trace(String.format("%d %g %s (%s)",Math.round(rank),score.getValue(),score.getKey(),(correct ? "+" : "-")));
		}
		log.debug("ap "+(totPrec / posSet.length) + (numPosSeen>0 ? " uncorrected "+(totPrec/numPosSeen) : ""));
		return totPrec / posSet.length;
	}
	
	public class ExampleSolutionScore {
		public int numPairs=0;
		public int numErrors=0;
		public double averagePrecision=0;
	}
	
	public static void main(String[] args) {
		int flags = Configuration.USE_DEFAULTS | Configuration.USE_TEST | Configuration.USE_PARAMS;
		log.info(String.format("flags: 0x%x",flags));
		ExperimentConfiguration c = new ExperimentConfiguration(args,flags);
		
//		Tester tester = new Tester(c.prover, new LogicProgram(Component.loadComponents(c.programFiles,c.alpha)));
		if (c.paramsFile != null) {
			ParamsFile file = new ParamsFile(c.paramsFile);
			c.tester.setParams(new SimpleParamVector(Dictionary.load(file)), c.weightingScheme);
			file.check(c);
		}

		log.info("Testing on "+c.testFile+"...");
		long start = System.currentTimeMillis();
		TestResults results = c.tester.testExamples(c.testFile);
		System.out.println("result= running time "+(System.currentTimeMillis() - start));
		System.out.println("result= pairs "+ results.pairTotal+" errors "+results.pairErrors+" errorRate "+results.errorRate+" map "+results.map);
	
	}
	public void setParams(ParamVector paramVec, WeightingScheme wScheme) {
		this.masterProgram.setFeatureDictWeighter(InnerProductWeighter.fromParamVec(paramVec, wScheme));
	}
}
