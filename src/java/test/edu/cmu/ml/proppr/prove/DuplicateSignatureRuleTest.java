package edu.cmu.ml.proppr.prove;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.junit.Test;

import edu.cmu.ml.proppr.prove.Prover;
import edu.cmu.ml.proppr.prove.TracingDfsProver;
import edu.cmu.ml.proppr.prove.wam.AWamProgram;
import edu.cmu.ml.proppr.prove.wam.LogicProgramException;
import edu.cmu.ml.proppr.prove.wam.ProofGraph;
import edu.cmu.ml.proppr.prove.wam.Query;
import edu.cmu.ml.proppr.prove.wam.WamProgram;

/** 
 * Bug: When multiple goals in a rule have the same signature, ProPPR doesn't resolve their variables properly.
 * 
 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
 */
public class DuplicateSignatureRuleTest {
	private static final String PROGRAM="src/testcases/duplicateSignatureTest.wam";

	@Test
	public void test2() throws LogicProgramException, IOException {
		AWamProgram program = WamProgram.load(new File(PROGRAM));
		ProofGraph pg = new ProofGraph(Query.parse("canExit(steve,X)"),program);
		
		Prover p = new TracingDfsProver(10);
		Map<Query,Double> result = p.solvedQueries(pg);
		for (Map.Entry<Query, Double> e : result.entrySet()) {
			System.out.println(e.getValue()+"\t"+e.getKey());
			assertEquals("Steve not allowed to exit "+e.getKey()+"\n",
					"canExit(steve,kitchen).",e.getKey().toString());
		}
	}
}
