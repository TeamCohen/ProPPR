package edu.cmu.ml.praprolog.graph;

import static org.junit.Assert.*;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.Test;

import edu.cmu.ml.praprolog.graph.v1.Edge;

public class EdgeTest {
	Set<Edge> edges;
	@Before
	public void setup() {
		edges = new TreeSet<Edge>();
		edges.add(new Edge("a","b"));
		edges.add(new Edge("b","a"));
		
	}

	@Test
	public void equality() {
		assertEquals(2,edges.size());
		assertTrue("ab",edges.contains(new Edge("a","b")));
		assertFalse("bc",edges.contains(new Edge("b","c")));
	}

	@Test
	public void testHashCode() {
		String[][] edges = {
				{"8","81"}, {"88","1"}, {"1","88"}};
		
		Set<Edge> edgeset = new TreeSet<Edge>();
		int i=0;
		for (String[] e : edges) {
			Edge o = new Edge(e[0],e[1]);
			for (Edge oops : edgeset) {
				assertFalse(o+"=="+oops,o.hashCode() == oops.hashCode());
			}
			edgeset.add(o);
		i++;}
	}
	
	@Test
	public void testEquals() {
		Edge e,f;
		e = new Edge("190","968");
		f = new Edge("190","968");
		assertTrue(e.equals(f));
		assertTrue(f.equals(e));
		assertEquals(0,e.compareTo(f));
		assertEquals(0,f.compareTo(e));
		
		f = new Edge("711","1");
		assertFalse(e.equals(f));
		assertFalse(f.equals(e));
		assertFalse(0==e.compareTo(f));
		assertFalse(0==f.compareTo(e));
	}
	
	
	@Test
	public void testWTF() {
		String[][] edgestrs = {
			{"199","1"}, {"199","1006"}, {"199","1005"}, {"199","1004"},
			{"199","1007"}, {"198","1"}, {"198","1003"}, {"198","1002"},
			{"198","1001"}, {"198","1000"}, {"344","1"}, {"344","220"},
			{"345","1"}, {"345","221"}, {"346","1"}, {"346","222"},
			{"347","1"}, {"347","223"}, {"340","1"}, {"340","220"},
			{"341","1"}, {"341","221"}, {"342","1"}, {"342","222"},
			{"343","1"}, {"343","223"}, {"810","1"}, {"810","222"},
			{"811","1"}, {"811","223"}, {"812","1"}, {"812","220"},
			{"813","1"}, {"813","221"}, {"348","1"}, {"348","220"},
			{"349","1"}, {"349","221"}, {"816","1"}, {"816","220"},
			{"817","1"}, {"817","221"}, {"719","1"}, {"719","223"},
			{"718","1"}, {"718","222"}, {"717","1"}, {"717","221"},
			{"716","1"}, {"716","220"}, {"715","1"}, {"715","223"},
			{"714","1"}, {"714","222"}, {"713","1"}, {"713","221"},
			{"712","1"}, {"712","220"}, {"711","1"}
		};
		Edge e = new Edge("190","968");
		Edge[] keys = new Edge[59];
		for (int i = 0; i<59; i++) {
			keys[i] = new Edge(edgestrs[i][0],edgestrs[i][1]);
			assertTrue("en equals "+i,!e.equals(keys[i]));
			assertTrue("ne equals "+i,!keys[i].equals(e));
			assertTrue("en compare "+i,0!=e.compareTo(keys[i]));
			assertTrue("ne compare "+i,0!=keys[i].compareTo(e));
			assertTrue("hashcode "+i,e.hashCode() != keys[i].hashCode());
		}
		for (int i=0; i<keys.length; i++) {
			for (int j=(i+1); j<keys.length; j++) {
				assertTrue("ji equals "+i,!keys[j].equals(keys[i]));
				assertTrue("ij equals "+i,!keys[i].equals(keys[j]));
				assertTrue("ji compare "+i,0!=keys[j].compareTo(keys[i]));
				assertTrue("ij compare "+i,0!=keys[i].compareTo(keys[j]));
				assertTrue("hashcode "+i,keys[j].hashCode() != keys[i].hashCode());
			}
		}
		
		
	}
}
