package edu.cmu.ml.proppr.prove.v1;

import static org.junit.Assert.*;

import org.junit.Test;

import edu.cmu.ml.proppr.prove.v1.Component;
import edu.cmu.ml.proppr.prove.v1.Goal;
import edu.cmu.ml.proppr.prove.v1.GoalComponent;
import edu.cmu.ml.proppr.prove.v1.LogicProgram;
import edu.cmu.ml.proppr.prove.v1.LogicProgramException;
import edu.cmu.ml.proppr.prove.v1.ProPPRLogicProgramState;

public class ComponentConsolidationTest {

	@Test
	public void test() {
		LogicProgram lp = new LogicProgram(
				Component.loadComponents("testcases/family.crules:testcases/family.cfacts:testcases/family-more.cfacts".split(":"), 
						Component.ALPHA_DEFAULT,
						null));
		assertEquals(2,lp.components.length);
		try {
			lp.lpOutlinks(new ProPPRLogicProgramState(Goal.decompile("sister,katie,-1")), false, false);
		} catch (LogicProgramException e) {
			fail("katie not found!");
		}
		
		Component[] components = Component.loadComponents(new String[] {"testcases/family.cfacts"}, 
				Component.ALPHA_DEFAULT,
				null);
		assertEquals(1,components.length);
		assertEquals("'testcases/family.cfacts'", ((GoalComponent) components[0]).label);
	}

}
