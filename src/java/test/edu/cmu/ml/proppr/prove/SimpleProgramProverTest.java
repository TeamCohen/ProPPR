package edu.cmu.ml.proppr.prove;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import edu.cmu.ml.proppr.prove.DfsProver;
import edu.cmu.ml.proppr.prove.Prover;
import edu.cmu.ml.proppr.prove.UniformWeighter;
import edu.cmu.ml.proppr.prove.wam.ConstantArgument;
import edu.cmu.ml.proppr.prove.wam.Goal;
import edu.cmu.ml.proppr.prove.wam.StateProofGraph;
import edu.cmu.ml.proppr.prove.wam.WamProgram;
import edu.cmu.ml.proppr.prove.wam.LogicProgramException;
import edu.cmu.ml.proppr.prove.wam.ProofGraph;
import edu.cmu.ml.proppr.prove.wam.Query;
import edu.cmu.ml.proppr.prove.wam.State;
import edu.cmu.ml.proppr.prove.wam.WamInterpreter;
import edu.cmu.ml.proppr.prove.wam.WamBaseProgram;
import edu.cmu.ml.proppr.prove.wam.plugins.WamPlugin;
import edu.cmu.ml.proppr.util.APROptions;
import edu.cmu.ml.proppr.util.StatusLogger;

public class SimpleProgramProverTest {
	public static final String PROGRAM = "src/testcases/wam/simpleProgram.wam";
	APROptions apr = new APROptions("depth=6");

	@Test
	public void test() throws IOException, LogicProgramException {
		WamProgram program = WamBaseProgram.load(new File(PROGRAM));
		Query q = new Query(new Goal("coworker",new ConstantArgument("steve"),new ConstantArgument("X")));
		System.out.println("Query: "+q.toString());
		ProofGraph p = new StateProofGraph(q,apr,program);
		Prover prover = new DfsProver(apr);
		Map<String,Double> sols = prover.solutions(p, new StatusLogger());
		assertEquals(2,sols.size());
		
		HashMap<String,Integer> expected = new HashMap<String,Integer>();
		expected.put("steve", 0);
		expected.put("sven",0);
		System.out.println("Query: "+q.toString());
		
		for (String pair : sols.keySet()) {
			System.out.println(pair);
			String[] parts = pair.split(":");
			String v = parts[1];
			System.out.println("Got solution: "+v);
			if (expected.containsKey(v)) expected.put(v, expected.get(v)+1);
		}
		for (Map.Entry<String,Integer> e : expected.entrySet()) assertEquals(e.getKey(),1,e.getValue().intValue());
	}
	
	@Test
	public void testFill() throws IOException, LogicProgramException {
		WamProgram program = WamBaseProgram.load(new File(PROGRAM));
		Query q = new Query(new Goal("coworker",new ConstantArgument("steve"),new ConstantArgument("X")));
		System.out.println("Query: "+q.toString());
		ProofGraph p = new StateProofGraph(q,apr,program);
		Prover prover = new DfsProver(apr);
		Map<State,Double> sols = prover.prove(p, new StatusLogger());
//		assertEquals(2,sols.size());
		
		HashMap<String,Integer> expected = new HashMap<String,Integer>();
		expected.put("steve", 0);
		expected.put("sven",0);
		System.out.println("Query: "+q.toString());
		
		for (State s : sols.keySet()) {
			if (!s.isCompleted()) continue;
			System.out.println(s);
			Query a = p.fill(s);
			System.out.println(a);
			String v = a.getRhs()[0].getArg(1).getName();
			System.out.println("Got solution: "+v);
			if (expected.containsKey(v)) expected.put(v, expected.get(v)+1);
		}
		for (Map.Entry<String,Integer> e : expected.entrySet()) assertEquals(e.getKey(),1,e.getValue().intValue());
	}
}
