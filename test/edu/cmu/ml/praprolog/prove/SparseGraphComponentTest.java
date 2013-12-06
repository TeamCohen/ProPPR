package edu.cmu.ml.praprolog.prove;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class SparseGraphComponentTest {
	SparseGraphComponent component;
	@Before
	public void setup() {
		component = SparseGraphComponent.load("testcases/sparseGraph/family.sparse");
	}

	@Test
	public void testDegree() {
		ProPPRLogicProgramState s = new ProPPRLogicProgramState(Goal.decompile("child,pam,-1"));
		assertEquals(3,component.degree(s));
	}
	
	@Test
	public void testRowEnd() {
		ProPPRLogicProgramState s = new ProPPRLogicProgramState(Goal.decompile("sister,yvette,-1"));
		assertEquals("Yvette should have no sisters\n",0,component.degree(s));
		s = new ProPPRLogicProgramState(Goal.decompile("sister,theresa,-1"));
		assertEquals("Theresa should have 1 sister\n",1,component.degree(s));
	}

}
