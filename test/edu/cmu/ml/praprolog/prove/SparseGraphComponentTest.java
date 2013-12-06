package edu.cmu.ml.praprolog.prove;

import static org.junit.Assert.*;

import org.junit.Test;

public class SparseGraphComponentTest {

	@Test
	public void testDegree() {
		SparseGraphComponent c = SparseGraphComponent.load("testcases/sparseGraph/family.sparse");
		ProPPRLogicProgramState s = new ProPPRLogicProgramState(Goal.decompile("child,pam,-1"));
		assertEquals(3,c.degree(s));
	}

}
