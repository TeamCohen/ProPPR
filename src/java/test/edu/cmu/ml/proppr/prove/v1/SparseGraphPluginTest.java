package edu.cmu.ml.proppr.prove.v1;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import edu.cmu.ml.proppr.prove.wam.plugins.SparseGraphPlugin;


public class SparseGraphPluginTest {
	SparseGraphPlugin plugin;
	@Before
	public void setup() {
		plugin =  SparseGraphPlugin.load("testcases/sparseGraph/family.sparse");
	}

	@Test
	public void testDegree() {
		ProPPRLogicProgramState s = new ProPPRLogicProgramState(Goal.decompile("child,pam,-1"));
		assertEquals(3,plugin.degree(s));
	}
	
	@Test
	public void testRowEnd() {
		ProPPRLogicProgramState s = new ProPPRLogicProgramState(Goal.decompile("sister,yvette,-1"));
		assertEquals("Yvette should have no sisters\n",0,plugin.degree(s));
		s = new ProPPRLogicProgramState(Goal.decompile("sister,theresa,-1"));
		assertEquals("Theresa should have 1 sister\n",1,plugin.degree(s));
	}

}
