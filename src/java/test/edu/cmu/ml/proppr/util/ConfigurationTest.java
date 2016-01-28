package edu.cmu.ml.proppr.util;

import static org.junit.Assert.*;

import org.junit.Test;

import edu.cmu.ml.proppr.util.Configuration;

public class ConfigurationTest {

	@Test
	public void test() {		
		int inputFiles = Configuration.USE_TRAIN;
		int outputFiles = Configuration.USE_PARAMS;
		int constants = Configuration.USE_EPOCHS | Configuration.USE_FORCE | Configuration.USE_THREADS;
		int modules = Configuration.USE_TRAINER | Configuration.USE_SRW | Configuration.USE_SQUASHFUNCTION;
		ModuleConfiguration c = new ModuleConfiguration(
				"--train src/testcases/train.examples.grounded --params params.wts --threads 3 --srw ppr:reg=l2:sched=local:mu=0.001:eta=1.0 --epochs 20".split(" "),
				inputFiles,outputFiles,constants,modules);
		assertNotNull(c.squashingFunction);
	}

}
