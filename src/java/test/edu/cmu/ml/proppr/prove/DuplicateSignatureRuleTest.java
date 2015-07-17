package edu.cmu.ml.proppr.prove;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.junit.Test;

import edu.cmu.ml.proppr.prove.Prover;
import edu.cmu.ml.proppr.prove.TracingDfsProver;
import edu.cmu.ml.proppr.prove.wam.StateProofGraph;
import edu.cmu.ml.proppr.prove.wam.WamProgram;
import edu.cmu.ml.proppr.prove.wam.LogicProgramException;
import edu.cmu.ml.proppr.prove.wam.ProofGraph;
import edu.cmu.ml.proppr.prove.wam.Query;
import edu.cmu.ml.proppr.prove.wam.WamBaseProgram;
import edu.cmu.ml.proppr.util.APROptions;

/** 
 * Bug: When multiple goals in a rule have the same signature, ProPPR doesn't resolve their variables properly.
 * 
 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
 */
public class DuplicateSignatureRuleTest {
	private static final String PROGRAM="src/testcases/duplicateSignatureTest.wam";

	@Test
	public void test2() throws LogicProgramException, IOException {
		APROptions apr = new APROptions("depth=10");
		WamProgram program = WamBaseProgram.load(new File(PROGRAM));
		ProofGraph pg = new StateProofGraph(Query.parse("canExit(steve,X)"),apr,program);
		
		Prover p = new TracingDfsProver(apr);
		Map<Query,Double> result = p.solvedQueries(pg);
		for (Map.Entry<Query, Double> e : result.entrySet()) {
			System.out.println(e.getValue()+"\t"+e.getKey());
			assertEquals("Steve not allowed to exit "+e.getKey()+"\n",
					"canExit(steve,kitchen).",e.getKey().toString());
		}
	}
}
