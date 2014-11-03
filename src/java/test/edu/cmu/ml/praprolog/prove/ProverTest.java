package edu.cmu.ml.praprolog.prove;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import edu.cmu.ml.praprolog.prove.wam.ConstantArgument;
import edu.cmu.ml.praprolog.prove.wam.Goal;
import edu.cmu.ml.praprolog.prove.wam.LogicProgramException;
import edu.cmu.ml.praprolog.prove.wam.ProofGraph;
import edu.cmu.ml.praprolog.prove.wam.Query;
import edu.cmu.ml.praprolog.prove.wam.WamInterpreter;
import edu.cmu.ml.praprolog.prove.wam.WamProgram;
import edu.cmu.ml.praprolog.prove.wam.plugins.WamPlugin;

public class ProverTest {

	@Test
	public void test() throws IOException, LogicProgramException {
		WamProgram program = WamProgram.load(new File("testcases/wam/simpleProgram.wam"));
		Query q = new Query(new Goal("coworker",new ConstantArgument("steve"),new ConstantArgument("X")));
		ProofGraph p = new ProofGraph(q,program);
		Prover prover = new DfsProver(new UniformWeighter(), 6);
		Map<String,Double> sols = prover.solutions(p);
		assertEquals(2,sols.size());
		
		HashMap<String,Integer> expected = new HashMap<String,Integer>();
		expected.put("steve", 0);
		expected.put("sven",0);
		
		for (String pair : sols.keySet()) {
			System.out.println(pair);
			String[] parts = pair.split(":");
			String v = parts[1];
			if (expected.containsKey(v)) expected.put(v, expected.get(v)+1);
		}
		for (Map.Entry<String,Integer> e : expected.entrySet()) assertEquals(e.getKey(),1,e.getValue().intValue());
	}

}
