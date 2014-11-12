package edu.cmu.ml.proppr.util;

import static org.junit.Assert.*;

import org.junit.Test;

import edu.cmu.ml.proppr.util.Configuration;

public class ConfigurationTest {

	@Test
	public void test() {
		assertFalse(Configuration.isOn(Configuration.USE_PARAMS, Configuration.USE_TRAINTEST));
	}

}
