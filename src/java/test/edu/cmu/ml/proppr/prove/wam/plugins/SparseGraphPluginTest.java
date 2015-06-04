package edu.cmu.ml.proppr.prove.wam.plugins;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import edu.cmu.ml.proppr.prove.wam.LogicProgramException;
import edu.cmu.ml.proppr.prove.wam.ProofGraph;
import edu.cmu.ml.proppr.prove.wam.Query;
import edu.cmu.ml.proppr.prove.wam.StateProofGraph;
import edu.cmu.ml.proppr.prove.wam.WamBaseProgram;
import edu.cmu.ml.proppr.prove.wam.WamProgram;
import edu.cmu.ml.proppr.prove.wam.plugins.SparseGraphPlugin;
import edu.cmu.ml.proppr.util.APROptions;


public class SparseGraphPluginTest {
	public static final String PLUGIN="src/testcases/sparseGraph/family.sparse";
	SparseGraphPlugin plugin;
	APROptions apr = new APROptions();
	WamProgram program = new WamBaseProgram();
	@Before
	public void setup() {
		plugin =  SparseGraphPlugin.load(apr,new File(PLUGIN));
	}
	
	@Test
	public void testClaim() {
		assertTrue("child",plugin._claim("child/2"));
		assertTrue("sister",plugin._claim("sister/2"));
		assertTrue("spouse",plugin._claim("spouse/2"));
	}

	@Test
	public void testDegree() throws LogicProgramException {
		Query q = Query.parse("child(pam,X)");
		StateProofGraph pg = new StateProofGraph(q,apr,program,plugin);
		// minus 1 for reset
		assertEquals(3,pg.pgDegree(pg.getStartState())-1);
	}
	
	@Test
	public void testRowEnd() throws LogicProgramException {
		StateProofGraph pg = new StateProofGraph(Query.parse("sister(yvette,X)"),apr,program,plugin);
		// minus 1 for reset
		assertEquals("Yvette should have no sisters\n",0,pg.pgDegree(pg.getStartState())-1);
		pg = new StateProofGraph(Query.parse("sister(theresa,X)"),apr,program,plugin);
		// minus 1 for reset
		assertEquals("Theresa should have 1 sister\n",1,pg.pgDegree(pg.getStartState())-1);
	}

}
