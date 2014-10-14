package edu.cmu.ml.praprolog.prove.v1;

import static org.junit.Assert.*;

import java.util.HashSet;

import org.junit.Test;

import edu.cmu.ml.praprolog.prove.v1.Goal;
import edu.cmu.ml.praprolog.prove.v1.LogicProgramState;
import edu.cmu.ml.praprolog.prove.v1.ProPPRLogicProgramState;

public class LogicProgramStateTest {

	LogicProgramState s1, r1, r2;

	private void setupFunctorStates() {
		s1 = new ProPPRLogicProgramState(new Goal("apples"));
		r1 = s1.restart();
		r2 = new ProPPRLogicProgramState(new Goal("apples"));
	}
	
	private void setup1varStates() {
		s1 = new ProPPRLogicProgramState(new Goal("food","X"));
		r1 = s1.restart();
		r2 = new ProPPRLogicProgramState(new Goal("food","X"));
	}
	
	@Test
	public void testFunctorEquals() {
		setupFunctorStates();
		doEqualsTests();
	}
	
	@Test
	public void test1varEquals() {
		setup1varStates();
		doEqualsTests();
	}
	
	private void doEqualsTests() {
		assertEquals("reset",s1,r1);
		assertEquals("independent duplicate",s1,r2);
	}
	
	@Test
	public void testFunctorHashContainment() {
		setupFunctorStates();
		doHashContainmentTests();
	}// lpState: goal(mem,c[d],c[l_de]) ...
	
	@Test
	public void test1varHashContainment() {
		setup1varStates();
		doHashContainmentTests();
	}
	
	private void doHashContainmentTests() {
		HashSet<ProPPRLogicProgramState> set = new HashSet<ProPPRLogicProgramState>();
		set.add((ProPPRLogicProgramState)s1);
		assertTrue("from reset",set.contains(r1));
		assertTrue("from independent duplicate",set.contains(r2));
	}

	
	
}
