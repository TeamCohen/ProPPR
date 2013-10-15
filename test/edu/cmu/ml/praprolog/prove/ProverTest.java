package edu.cmu.ml.praprolog.prove;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import edu.cmu.ml.praprolog.prove.Component;
import edu.cmu.ml.praprolog.prove.Component.Outlink;
import edu.cmu.ml.praprolog.prove.DprProver;
import edu.cmu.ml.praprolog.prove.FeatureDictWeighter;
import edu.cmu.ml.praprolog.prove.Goal;
import edu.cmu.ml.praprolog.prove.GoalComponent;
import edu.cmu.ml.praprolog.prove.InnerProductWeighter;
import edu.cmu.ml.praprolog.prove.LogicProgram;
import edu.cmu.ml.praprolog.prove.LogicProgramState;
import edu.cmu.ml.praprolog.prove.PprProver;
import edu.cmu.ml.praprolog.prove.Prover;
import edu.cmu.ml.praprolog.prove.Rule;
import edu.cmu.ml.praprolog.prove.RuleComponent;
import edu.cmu.ml.praprolog.util.Dictionary;

public class ProverTest {
	private static final Logger log = Logger.getLogger(ProverTest.class);
	LogicProgram lpMilk;
	LogicProgram lpMem;
	Prover prover;
	double[] proveStateAnswers=new double[3];
	@Before
	public void setup() {
		lpMilk = new LogicProgram(RuleComponentTest.makeClassifyIDB(), GoalComponentTest.makeClassifyEDB());
		lpMem = new LogicProgram(RuleComponentTest.makeMemIDB(), GoalComponentTest.makeMemEDB());
		setProveStateAnswers();
	}
	
	public void setProveStateAnswers() {
		proveStateAnswers[0] = 0.14286;
		proveStateAnswers[1] = 0.07143;
		proveStateAnswers[2] = 0.07143;
	}

	@Test
	public void testProveState() {
		log.info("testProveState");
		FeatureDictWeighter w = new InnerProductWeighter();
		w.put(new Goal("milk"),2);
		lpMilk.setFeatureDictWeighter(w);

		Map<LogicProgramState,Double> dist = prover.proveState(lpMilk,Prover.parseQuery("isa","elsie","X"));

		for(Map.Entry<LogicProgramState, Double> s : dist.entrySet()) {
			if (s.getKey().getGoal(0).getFunctor().equals("givesMilk")) {
				assertEquals(proveStateAnswers[0], s.getValue(), 1e-5);
			} else if (s.getKey().getGoal(0).getFunctor().equals("isa")){
				assertEquals(proveStateAnswers[2], s.getValue(), 1e-5);
			} else {
				assertEquals(proveStateAnswers[1], s.getValue(), 1e-5);
			} 
		}
	}
	
	@Test
	public void testSolDelta() {

		PprProver ppr = new PprProver(20);
		DprProver dpr = new DprProver(0.00001, .03);
	    
		log.info("testSolDelta:mem(X,l_de)");
	    assertEquals(0,maxSolDelta(dpr,ppr,lpMem,"mem","X","l_de"),0.05);

	    log.info("testSolDelta:mem(X,l_abcde)");
	    assertEquals(0,maxSolDelta(dpr,ppr,lpMem,"mem","X","l_abcde"),0.05);
		
	    log.info("testSolDelta:mem2(X,Y,l_abcde)");
		assertEquals(0,maxSolDelta(dpr,ppr,lpMem,"mem2","X","Y","l_abcde"),0.05);
		
        log.info("testSolDelta:mem3(X,Y,Z,l_bcde)");
	    assertEquals(0,maxSolDelta(dpr,ppr,lpMem,"mem3","X","Y","Z","l_bcde"),0.05);
	    
	    log.info("testSolDelta:isa(elsie,X)");
	    assertEquals(0,maxSolDelta(dpr,ppr,lpMilk,"isa","elsie","X"),0.05);
	}

	public double maxSolDelta(Prover p1, Prover p2, LogicProgram lp, String goal, String ... args) {
		Map<String,Double> sol1 = p1.solutionsForQuery(lp, goal, args);
		Map<String,Double> sol2 = p2.solutionsForQuery(lp, goal, args);
		double maxDelta = 0;
		assertTrue("no solutions for 1",sol1.size() > 0 || sol2.size() == 0);
		assertTrue("no solutions for 2",sol2.size() > 0 || sol1.size() == 0);
		log.info("--- state ---\tdpr      \tppr      \tdelta");
		for (Map.Entry<String, Double> w1 : sol1.entrySet()) {
			Double w2 = Dictionary.safeGet(sol2,w1.getKey(),0.0);
			//	        assertNotNull(w1.getKey()+" not present in sol2",w2);
			double delta = Math.abs(w1.getValue() - w2);
			log.info(String.format("%s\t%f\t%f\t%f",w1.getKey(),w1.getValue(),w2,delta));
			maxDelta = Math.max(maxDelta, delta);
		}
		//	    for (String key : sol2.keySet()) assertTrue(key+" not present in sol1",sol1.containsKey(key));
		return maxDelta;
	}





}
