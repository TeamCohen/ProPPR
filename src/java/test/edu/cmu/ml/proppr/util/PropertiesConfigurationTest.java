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
				Configuration.USE_PROVER | Configuration.USE_WEIGHTINGSCHEME);
		assertTrue("Didn't fetch properties from file",c.paramsFile != null);
		assertTrue("Didn't prefer command line properties",c.prover instanceof DfsProver);
		assertTrue("Didn't fetch unary argument",c.force);
		assertEquals("Didn't fetch apr options properly",0.01,c.apr.alpha,1e-10);
	}
	
	@Test
	public void testWeibo() {
		System.setProperty(Configuration.PROPFILE, "src/testcases/weibo.properties");
		ModuleConfiguration c = new ModuleConfiguration(new String[]{},
				Configuration.USE_QUERIES,
				Configuration.USE_GROUNDED,
				Configuration.USE_WAM | Configuration.USE_THREADS,
				Configuration.USE_GROUNDER | Configuration.USE_PROVER);
		assertEquals(10,c.epochs);
		assertEquals(0.1,c.apr.alpha,1e-10);
	}
}
