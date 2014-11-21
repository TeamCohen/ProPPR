package edu.cmu.ml.proppr.prove;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import edu.cmu.ml.proppr.prove.Prover;
import edu.cmu.ml.proppr.prove.wam.Argument;
import edu.cmu.ml.proppr.prove.wam.ConstantArgument;
import edu.cmu.ml.proppr.prove.wam.Goal;
import edu.cmu.ml.proppr.prove.wam.AWamProgram;
import edu.cmu.ml.proppr.prove.wam.LogicProgramException;
import edu.cmu.ml.proppr.prove.wam.Outlink;
import edu.cmu.ml.proppr.prove.wam.ProofGraph;
import edu.cmu.ml.proppr.prove.wam.Query;
import edu.cmu.ml.proppr.prove.wam.State;
import edu.cmu.ml.proppr.prove.wam.WamInterpreter;
import edu.cmu.ml.proppr.prove.wam.WamProgram;
import edu.cmu.ml.proppr.prove.wam.plugins.WamPlugin;
import edu.cmu.ml.proppr.util.Dictionary;

public class ProofGraphTest {

	@Test
	public void test() throws LogicProgramException, IOException {
		AWamProgram program = WamProgram.load(new File("testcases/wam/simpleProgram.wam"));
		Query q = new Query(new Goal("coworker",new ConstantArgument("steve"),new ConstantArgument("X")));
		ProofGraph pg = new ProofGraph(q,program);
		
		HashMap<String,Integer> solutions = new HashMap<String,Integer>();
		solutions.put("steve", 0);
		solutions.put("sven",0);
		
		Map<State,Double> vec = new HashMap<State,Double>();
		vec.put(pg.getStartState(),1.0);

		// step 1: coworker -> employee,boss
		System.out.println("Step 1");
		for (State s : vec.keySet()) System.out.println(s);
		List<Outlink> outlinks = pg.pgOutlinks(pg.getStartState(),false,false);
		assertEquals("1. coworker :- employee,boss",1,outlinks.size());
		vec = nextVec(vec,normalized(outlinks));
		assertEquals("1. statecount",1,vec.size());

		// step 2: 
		System.out.println("Step 2");
		for (State s : vec.keySet()) System.out.println(s);
		outlinks = pg.pgOutlinks(vec.keySet().iterator().next(),false,false);
		assertEquals("2. employee :- management,boss",1,outlinks.size());
		vec = nextVec(vec,normalized(outlinks));
		assertEquals("2. statecount",1,vec.size());

		// step 3: 
		System.out.println("Step 3");
		for (State s : vec.keySet()) System.out.println(s);
		outlinks = pg.pgOutlinks(vec.keySet().iterator().next(),false,false);
		assertEquals("3. management :- sookie",1,outlinks.size());
		vec = nextVec(vec,normalized(outlinks));
		assertEquals("3. statecount",1,vec.size());

		// step 4: 
		System.out.println("Step 4");
		for (State s : vec.keySet()) System.out.println(s);
		outlinks = pg.pgOutlinks(vec.keySet().iterator().next(),false,false);
		assertEquals("4. boss(sookie,X) :- _steve_ + sven",1,outlinks.size());
		vec = nextVec(vec,normalized(outlinks));
		assertEquals("4. statecount",1,vec.size());

		// step 5: 
		System.out.println("Step 5");
		for (State s : vec.keySet()) {
			System.out.println(s);
			System.out.println(Dictionary.buildString(pg.asDict(s), new StringBuilder(), "\n\t").substring(1));
		}
		outlinks = pg.pgOutlinks(vec.keySet().iterator().next(),false,false);
		assertEquals("5. boss(sookie,X) :- steve + sven",2,outlinks.size());
		vec = nextVec(vec,normalized(outlinks));
		assertEquals("5. statecount",2,vec.size());
		
		// step 6: 
		System.out.println("Step 6");
		for (State s : vec.keySet()) {
			System.out.println(s);
			Map<Argument,String> dict = pg.asDict(s);
			System.out.println(Dictionary.buildString(dict, new StringBuilder(), "\n\t").substring(1));
			assertTrue(s.isCompleted());
			for (String v : dict.values()) {
				if (solutions.containsKey(v)) solutions.put(v, solutions.get(v)+1);
			}
		}

		for (Map.Entry<String,Integer> e : solutions.entrySet()) assertEquals(e.getKey(),1,e.getValue().intValue());
		
	}
	private Map<State,Double> normalized(List<Outlink> outlinks) {
		Map<State,Double> normalized = new HashMap<State,Double>();
		double total = 0;
		for (Outlink o : outlinks) {
			double wt = 0;
			for (Map.Entry<Goal, Double>e : o.fd.entrySet()) { wt+=e.getValue(); }
			normalized.put(o.child, wt);
			total += wt;
		}
		for (Map.Entry<State,Double>e : normalized.entrySet()) {
			e.setValue(e.getValue() / total);
		}
		return normalized;
	}
	private Map<State,Double> nextVec(Map<State,Double> vec, Map<State,Double> normalized) {
		Map<State,Double> nextVec = new HashMap<State,Double>();
		for (Map.Entry<State,Double> e : normalized.entrySet()) {
			nextVec.put(e.getKey(),e.getValue() * Dictionary.safeGet(vec,e.getKey()));
		}
		return nextVec;
	}
}
