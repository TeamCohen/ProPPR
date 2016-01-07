package edu.cmu.ml.proppr.learn.tools;

import static org.junit.Assert.*;

import org.junit.Test;

import edu.cmu.ml.proppr.util.Configuration;
import edu.cmu.ml.proppr.util.ModuleConfiguration;

public class FixedWeightRulesTest {

	@Test
	public void test() {
		Configuration c = new Configuration("--fixedWeights f(thing,pos)".split(" "), 
				0, 0, Configuration.USE_FIXEDWEIGHTS,0);
		assertTrue("Raw fixedWeightRules", c.fixedWeightRules.isFixed("f(thing,pos)"));
	}
	
	@Test
	public void srwTest() {
		ModuleConfiguration c = new ModuleConfiguration("--fixedWeights f(thing,pos)".split(" "), 
				0, 0, Configuration.USE_FIXEDWEIGHTS,Configuration.USE_SRW);
		assertTrue("Raw fixedWeightRules", c.fixedWeightRules.isFixed("f(thing,pos)"));
		assertFalse("in an SRW", c.srw.trainable("f(thing,pos)"));
	}
	
	@Test
	public void testCascade() {
		Configuration c = new Configuration("--fixedWeights f(*=n:*=y".split(" "),
				0, 0, Configuration.USE_FIXEDWEIGHTS,0);
		assertTrue("Most rules", c.fixedWeightRules.isFixed("id(x,12,15)"));
		assertFalse("f(* rules", c.fixedWeightRules.isFixed("f(x,12,15)"));
	}

}
