package edu.cmu.ml.praprolog.prove;

import static org.junit.Assert.*;

import org.junit.Test;

import edu.cmu.ml.praprolog.prove.wam.Argument;
import edu.cmu.ml.praprolog.prove.wam.ConstantArgument;

public class ArgumentTest {

	@Test
	public void testIsVariableAtom() {
		Argument a_ = new ConstantArgument("_foo");
		assertTrue("Starts with _",a_.isVariableAtom());
		String alpha = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		for (int i=0; i<26; i++) {
			Argument a = new ConstantArgument(alpha.charAt(i)+"foo");
			assertTrue("Starts with "+alpha.charAt(i),a.isVariableAtom());
		}
	}

}
