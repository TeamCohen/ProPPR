package edu.cmu.ml.proppr.prove.wam.plugins;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Map;

import org.junit.Test;

import edu.cmu.ml.proppr.prove.DprProver;
import edu.cmu.ml.proppr.prove.Prover;
import edu.cmu.ml.proppr.prove.wam.ConstantArgument;
import edu.cmu.ml.proppr.prove.wam.Goal;
import edu.cmu.ml.proppr.prove.wam.AWamProgram;
import edu.cmu.ml.proppr.prove.wam.LogicProgramException;
import edu.cmu.ml.proppr.prove.wam.ProofGraph;
import edu.cmu.ml.proppr.prove.wam.Query;
import edu.cmu.ml.proppr.prove.wam.WamProgram;
import edu.cmu.ml.proppr.prove.wam.plugins.FactsPlugin;

public class FactsPluginTest {

	@Test
	public void test() throws LogicProgramException {
		FactsPlugin p = FactsPlugin.load(new File("testcases/textcattoy/toylabels.cfacts"), false);
		AWamProgram program = new WamProgram();
		Query q = new Query(new Goal("isLabel",new ConstantArgument("X")));
		ProofGraph pg = new ProofGraph(q,program,p);
		Prover prover = new DprProver();
		Map<String,Double> sols = prover.solutions(pg);
		assertEquals(2,sols.size());
	}

}
