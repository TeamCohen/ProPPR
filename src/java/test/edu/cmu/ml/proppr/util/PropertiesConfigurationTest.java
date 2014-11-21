package edu.cmu.ml.proppr.util;

import static org.junit.Assert.*;

import org.junit.Test;

import edu.cmu.ml.proppr.prove.DfsProver;
import edu.cmu.ml.proppr.prove.PprProver;
import edu.cmu.ml.proppr.util.Configuration;
import edu.cmu.ml.proppr.util.ModuleConfiguration;

public class PropertiesConfigurationTest {

	@Test
	public void test() {
		// config.properties defines train, test, params, prover, queries, force (unary), and two nonexistant options.
		System.setProperty(Configuration.PROPFILE, "src/testcases/config.properties");
		ModuleConfiguration c = new ModuleConfiguration("--prover dfs".split(" "), 
				0,
				Configuration.USE_PARAMS,
				Configuration.USE_FORCE,
				Configuration.USE_PROVER);
		assertTrue("Didn't fetch properties from file",c.paramsFile != null);
		assertTrue("Didn't prefer command line properties",c.prover instanceof DfsProver);
		assertTrue("Didn't fetch unary argument",c.force);
	}
}
