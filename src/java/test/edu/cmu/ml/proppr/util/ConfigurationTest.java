package edu.cmu.ml.proppr.util;

import static org.junit.Assert.*;

import org.junit.Test;

import edu.cmu.ml.proppr.util.Configuration;

public class ConfigurationTest {

	@Test
	public void test() {		
		int inputFiles = Configuration.USE_TRAIN;
		int outputFiles = Configuration.USE_PARAMS;
		int constants = Configuration.USE_EPOCHS | Configuration.USE_TRACELOSSES | Configuration.USE_FORCE | Configuration.USE_THREADS;
		int modules = Configuration.USE_TRAINER | Configuration.USE_SRW | Configuration.USE_WEIGHTINGSCHEME;
		ModuleConfiguration c = new ModuleConfiguration(
				"--train examples/textcattoy/train.examples.grounded --params params.wts --threads 3 --srw l2plocal:0.001:1.0 --epochs 20".split(" "),
				inputFiles,outputFiles,constants,modules);
		assertNotNull(c.weightingScheme);
	}

}
