package edu.cmu.ml.praprolog.util;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Before;
import org.junit.Test;

public class DictionaryTest {
	Map<String,Double> map;
	Map<String,Map<String,Double>> mapmap;
	static final double EPSILON = 1e-10;
	@Before
	public void setup() {
		map = new TreeMap<String,Double>();
		map.put("one", 1.0);
		map.put("zero",0.0);
		mapmap = new TreeMap<String,Map<String,Double>>();
		mapmap.put("a",map);
	}

	@Test
	public void testIncrement() {
		Dictionary.increment(map, "one", 1.0);
		assertEquals("one",2,map.get("one"),EPSILON);
		
		Dictionary.increment(map, "zero", 1.0);
		assertEquals("zero",1,map.get("zero"),EPSILON);
		
		Dictionary.increment(map, "foo",1.0);
		assertEquals("foo",1,map.get("foo"),EPSILON);
	}
	
	@Test
	public void testNestedIncrement() {
		Dictionary.increment(mapmap, "a", "one", 1.0);
		assertEquals(2.0,Dictionary.safeGetGet(mapmap,"a","one"),EPSILON);
		
		Dictionary.increment(mapmap, "b", "one", 1.0);
		assertEquals(1.0,Dictionary.safeGetGet(mapmap,"b","one"),EPSILON);
	}

	@Test
	public void testSort() {
		List<Map.Entry<String,Double>> items = Dictionary.sort(map);
		assertEquals("one",items.get(0).getKey());
		assertEquals("zero",items.get(1).getKey());
	}
}
