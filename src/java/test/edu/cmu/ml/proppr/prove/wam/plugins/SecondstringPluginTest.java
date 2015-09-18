package edu.cmu.ml.proppr.prove.wam.plugins;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;

import edu.cmu.ml.proppr.GrounderTest;
import edu.cmu.ml.proppr.prove.TracingDfsProver;
import edu.cmu.ml.proppr.prove.wam.LogicProgramException;
import edu.cmu.ml.proppr.prove.wam.Query;
import edu.cmu.ml.proppr.prove.wam.StateProofGraph;
import edu.cmu.ml.proppr.prove.wam.WamBaseProgram;
import edu.cmu.ml.proppr.prove.wam.WamProgram;
import edu.cmu.ml.proppr.util.APROptions;
import edu.cmu.ml.proppr.util.Dictionary;

public class SecondstringPluginTest {
	public static final String PLUGIN="src/testcases/secondstring/directory.ssdb";

	@Test
	public void test() throws LogicProgramException {
		APROptions apr = new APROptions();
		SecondstringPlugin p = SecondstringPlugin.load(apr, new File(SecondstringPluginTest.PLUGIN));
		assertTrue("Must claim personForName",p.claim("personForName/2"));

		Query q = Query.parse("personForName(william,X)");
		WamProgram program = new WamBaseProgram();
		StateProofGraph pg = new StateProofGraph(q,apr,program,p);
		System.out.println(Dictionary.buildString(new TracingDfsProver(apr).prove(pg), new StringBuilder(), "\n").toString());
	}

}
