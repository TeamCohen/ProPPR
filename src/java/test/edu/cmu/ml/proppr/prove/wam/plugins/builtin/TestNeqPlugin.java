package edu.cmu.ml.proppr.prove.wam.plugins.builtin;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import edu.cmu.ml.proppr.prove.DprProver;
import edu.cmu.ml.proppr.prove.Prover;
import edu.cmu.ml.proppr.prove.wam.LogicProgramException;
import edu.cmu.ml.proppr.prove.wam.ProofGraph;
import edu.cmu.ml.proppr.prove.wam.Query;
import edu.cmu.ml.proppr.prove.wam.StateProofGraph;
import edu.cmu.ml.proppr.prove.wam.WamBaseProgram;
import edu.cmu.ml.proppr.prove.wam.WamProgram;
import edu.cmu.ml.proppr.util.APROptions;

public class TestNeqPlugin {
	public static final String PROGRAM="src/testcases/ne.wam";

	@Test
	public void test() throws IOException, LogicProgramException {
		APROptions apr = new APROptions();
		WamProgram program = WamProgram.load(new File(PROGRAM));
		Query different = Query.parse("different(door,cat)");
		Query same = Query.parse("different(lake,lake)");
		Prover p = new DprProver(apr);
		assertEquals("different should have 1 solution",1,p.solutions(new StateProofGraph(different,apr,program)).size());
		assertEquals("same should have no solution",0,p.solutions(new StateProofGraph(same,apr,program)).size());
	}

}
