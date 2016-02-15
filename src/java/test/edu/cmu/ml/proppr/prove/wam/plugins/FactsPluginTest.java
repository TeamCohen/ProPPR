package edu.cmu.ml.proppr.prove.wam.plugins;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Map;

import org.junit.Test;

import edu.cmu.ml.proppr.GrounderTest;
import edu.cmu.ml.proppr.prove.DprProver;
import edu.cmu.ml.proppr.prove.Prover;
import edu.cmu.ml.proppr.prove.wam.ConstantArgument;
import edu.cmu.ml.proppr.prove.wam.Goal;
import edu.cmu.ml.proppr.prove.wam.StateProofGraph;
import edu.cmu.ml.proppr.prove.wam.WamProgram;
import edu.cmu.ml.proppr.prove.wam.LogicProgramException;
import edu.cmu.ml.proppr.prove.wam.ProofGraph;
import edu.cmu.ml.proppr.prove.wam.Query;
import edu.cmu.ml.proppr.prove.wam.WamBaseProgram;
import edu.cmu.ml.proppr.prove.wam.plugins.FactsPlugin;
import edu.cmu.ml.proppr.util.APROptions;
import edu.cmu.ml.proppr.util.StatusLogger;

public class FactsPluginTest {
	@Test
	public void test() throws LogicProgramException {
		APROptions apr = new APROptions();
		FactsPlugin p = FactsPlugin.load(apr, new File(GrounderTest.FACTS), false);
		WamProgram program = new WamBaseProgram();
		Query q = Query.parse("validClass(X)");
		StateProofGraph pg = new StateProofGraph(q,apr,program,p);
		Prover prover = new DprProver();
		Map<String,Double> sols = prover.solutions(pg, new StatusLogger());
		assertEquals(2,sols.size());
	}

}
