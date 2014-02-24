package edu.cmu.ml.praprolog;

import static org.junit.Assert.*;

import java.io.File;
import java.util.HashMap;

import junit.framework.TestResult;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import edu.cmu.ml.praprolog.Tester.ExampleSolutionScore;
import edu.cmu.ml.praprolog.Tester.TestResults;
import edu.cmu.ml.praprolog.prove.Component;
import edu.cmu.ml.praprolog.prove.DprProver;
import edu.cmu.ml.praprolog.prove.Goal;
import edu.cmu.ml.praprolog.prove.GoalComponentTest;
import edu.cmu.ml.praprolog.prove.LogicProgram;
import edu.cmu.ml.praprolog.prove.PprProver;
import edu.cmu.ml.praprolog.prove.RawPosNegExample;
import edu.cmu.ml.praprolog.prove.RawPosNegExampleStreamer;
import edu.cmu.ml.praprolog.prove.RuleComponentTest;
import edu.cmu.ml.praprolog.prove.ThawedPosNegExample;

public class TesterTest {

	@Before
	public void setup() {
		BasicConfigurator.configure(); Logger.getRootLogger().setLevel(Level.WARN);
	}
	
	@Test
	public void testTestExample() {
		LogicProgram milk = new LogicProgram(RuleComponentTest.makeClassifyIDB(), GoalComponentTest.makeClassifyEDB()); 
		Tester tee = new Tester(new PprProver(), milk);
		
		RawPosNegExample rawX = new RawPosNegExampleStreamer()
			.exampleFromString("isa(tweetie,X)	+isa(tweetie,duck)	+isa(tweetie,chicken)	-isa(tweetie,platypus)", false);
		ExampleSolutionScore s = tee.testExample(rawX, milk);
		assertEquals("pairs",2,s.numPairs);
		assertEquals("errors",0,s.numErrors);
		assertEquals("ap",1.0,s.averagePrecision,1e-6);
		System.out.println("pairs "+s.numPairs+" errors "+s.numErrors+" ap "+s.averagePrecision); 
	}
	
	@Test
	public void testTestExamples_textcat() {
		// predict(r10172,Y)	+predict(r10172,grain)	+predict(r10172,corn)	+predict(r10172,wheat)	-predict(r10172,sugar)
		LogicProgram textcat = new LogicProgram(
				Component.loadComponents("testcases/textcat/textcat.crules:testcases/textcat/labels.cfacts:testcases/textcat/words.graph".split(":"), DprProver.MINALPH_DEFAULT+DprProver.EPS_DEFAULT));
		Tester tee = new Tester(new DprProver(), textcat);
		TestResults results = tee.testExamples(new File("testcases/textcat/test.1.data"));
		System.out.println("pairs "+results.pairTotal+" errors "+results.pairErrors+" errorRate "+results.errorRate+" map "+results.map);
	}

	@Test
	public void testAveragePrecision() {
		Tester tee = new Tester(null,null);
		
		HashMap<Goal,Double> scores = new HashMap<Goal,Double>();
		Goal[] pos = {
				new Goal("cat"),
				new Goal("dog"),
				new Goal("mouse")
		};
		scores.put(pos[0],1.0);
		scores.put(new Goal("peas"),0.9);
		scores.put(new Goal("spinach"),0.8);
		scores.put(pos[1],0.7);
		scores.put(pos[2], 0.6);
		
		// R:entity       totPrec nPosSeen numFP
		// 1:cat             1        1      0   totPrec += (rank=1 - numFP=0) / (rank=1) = 1/1 = 1
		// 2:peas            1        1      1
		// 3:spinach         1        1      2
		// 4:dog             1.5      2      2   totPrec += (rank=4 - numFP=2) / (rank=4) = 2/4 = 0.5
		// 5:mouse           2.1      3      2   totPrec += (rank=5 - numFP=2) / (rank=5) = 3/5 = 0.6
		// AP = 2.1 / 3 = 0.7
		
		double ap = tee.averagePrecision(scores, pos);
		assertEquals(0.7,ap,1e-6);
	}

}
