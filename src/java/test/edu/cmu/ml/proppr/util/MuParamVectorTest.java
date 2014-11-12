package edu.cmu.ml.proppr.util;

import static org.junit.Assert.*;

import java.util.Collections;

import org.junit.Test;

import edu.cmu.ml.proppr.util.MuParamVector;

public class MuParamVectorTest {
	private static final double EPS=1e-6;

	@Test
	public void testMap() {
		MuParamVector foo = new MuParamVector();
		foo.put("abc",1.0);
		foo.put("def",10.0);
		
		assertEquals(2,foo.size());
		assertEquals(1.0,foo.get("abc"),EPS);
		assertEquals(10.0,foo.get("def"),EPS);
	}
	
	@Test
	public void testTimestamp() {
		MuParamVector foo = new MuParamVector();
		foo.put("abc",1.0);
		foo.put("def",10.0);
		
		assertEquals(0,foo.getLast("abc"));
		foo.count();
		assertEquals(1,foo.getLast("abc"));
		assertEquals(1,foo.getLast("def"));
		foo.setLast(Collections.singleton("abc"));
		assertEquals(0,foo.getLast("abc"));
		assertEquals(1,foo.getLast("def"));
		
	}

}
