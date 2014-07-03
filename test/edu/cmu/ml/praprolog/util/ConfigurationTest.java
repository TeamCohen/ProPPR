package edu.cmu.ml.praprolog.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class ConfigurationTest {

	@Test
	public void test() {
		assertFalse(Configuration.isOn(Configuration.USE_PARAMS, Configuration.USE_TRAINTEST));
	}

}
