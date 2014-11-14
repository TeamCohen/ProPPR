package edu.cmu.ml.proppr.util;

import static org.junit.Assert.*;

import org.junit.Test;

import edu.cmu.ml.proppr.prove.v1.PprProver;
import edu.cmu.ml.proppr.util.Configuration;
import edu.cmu.ml.proppr.util.ModuleConfiguration;

public class PropertiesConfigurationTest {

	@Test
	public void test() {
		// config.properties defines programFiles, data, test, output, prover, queries, strict (unary), and two nonexistant options.
		System.setProperty(Configuration.PROPFILE, "testcases/config.properties");
		ModuleConfiguration c = new ModuleConfiguration("--prover ppr".split(" "), 
				Configuration.USE_DEFAULTS | Configuration.USE_LEARNINGSET | Configuration.USE_TEST);
		assertNotNull(c.programFiles);
		assertTrue("Didn't fetch properties from file",c.programFiles.length > 0);
		assertTrue("Didn't prefer command line properties",c.prover instanceof PprProver);
		assertTrue("Didn't fetch unary argument",c.strict);
	}

}
