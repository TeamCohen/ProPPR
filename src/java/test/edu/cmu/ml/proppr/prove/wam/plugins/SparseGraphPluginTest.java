package edu.cmu.ml.proppr.prove.wam.plugins;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import edu.cmu.ml.proppr.prove.wam.LogicProgramException;
import edu.cmu.ml.proppr.prove.wam.ProofGraph;
import edu.cmu.ml.proppr.prove.wam.Query;
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
		assertTrue("child",plugin.claim("child/2"));
		assertTrue("sister",plugin.claim("sister/2"));
		assertTrue("spouse",plugin.claim("spouse/2"));
	}

	@Test
	public void testDegree() throws LogicProgramException {
		Query q = Query.parse("child(pam,X)");
		ProofGraph pg = new ProofGraph(q,apr,program,plugin);
		assertEquals(3,pg.pgDegree(pg.getStartState()));
	}
	
	@Test
	public void testRowEnd() throws LogicProgramException {
		ProofGraph pg = new ProofGraph(Query.parse("sister(yvette,X)"),apr,program,plugin);
		assertEquals("Yvette should have no sisters\n",0,pg.pgDegree(pg.getStartState()));
		pg = new ProofGraph(Query.parse("sister(theresa,X)"),apr,program,plugin);
		assertEquals("Theresa should have 1 sister\n",1,pg.pgDegree(pg.getStartState()));
	}

}
