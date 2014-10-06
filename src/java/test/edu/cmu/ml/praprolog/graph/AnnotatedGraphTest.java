package edu.cmu.ml.praprolog.graph;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import edu.cmu.ml.praprolog.Trainer;
import edu.cmu.ml.praprolog.graph.AnnotatedGraph;
import edu.cmu.ml.praprolog.graph.AnnotatedGraph.GraphFormatException;
import edu.cmu.ml.praprolog.graph.Feature;
import edu.cmu.ml.praprolog.learn.tools.PosNegRWExample;

public class AnnotatedGraphTest {
	AnnotatedGraph g;
	@Before
	public void setup() {
		g = new AnnotatedStringGraph();
	}

	@Test
	public void testPhi() {
		g.addDirectedEdge("a", "c");

		ArrayList<Feature> ff = new ArrayList<Feature>();
		ff.add(new Feature("foo",1.0));
		g.addDirectedEdge("a", "b", ff);
		
//		assertEquals(1,g.phi("a", "b").size());
//		assertEquals(0,g.phi("a", "c").size());
		assertEquals(1,g.phiFromString("a", "b").size());
		assertEquals(0,g.phiFromString("a", "c").size());
		
		ff = new ArrayList<Feature>();
		ff.add(new Feature("bar",1.0));
		g.addDirectedEdge("b","c",ff);
		
		assertEquals(1,g.phiFromString("a", "b").size());
		assertEquals(0,g.phiFromString("a", "c").size());
		assertEquals(1,g.phiFromString("b", "c").size());
	}
	
	public List<Feature> makeFeatures() {
		ArrayList<Feature> ff = new ArrayList<Feature>();
		ff.add(new Feature("foo"+Math.round(Math.random()*10),1.0));
		return ff;
	}
	
	@Test
	public void testPhiPoof() {
		g.addDirectedEdge("190","968",makeFeatures());
		assertEquals("init",1,g.phiFromString("190","968").size());
		g.addDirectedEdge("141","773",makeFeatures());
		assertEquals("after",1,g.phiFromString("190","968").size());
		
	}
	
	@Test @Ignore
	public void testFromStringParts() throws GraphFormatException {
		String testGraph = "16	39	w(a,pos):id(trueLoopRestart):id(trueLoop):w(a,neg):w(pricy,neg):w(pricy,pos):id(demo/textcat/toylabels.facts):w(house,neg):alphaBooster:w(doll,pos):r:w(doll,neg):w(house,pos):id(defaultRestart):id(demo/textcat/toywords.graph)"
				+"	1->1:13	1->2:10"
				+"	2->1:13,8	2->3:14	2->4:14	2->5:14	2->6:14"
				+"	3->1:13	3->7:6	3->8:6"
				+"	9->1:1	9->9:2"
				+"	8->1:13	8->10:0"
				+"	4->1:13	4->11:6	4->12:6"
				+"	5->1:13	5->13:6	5->14:6"
				+"	6->1:13	6->15:6	6->16:6"
				+"	7->1:13	7->9:3"
				+"	10->1:1	10->10:2"
				+"	11->1:13	11->9:4"
				+"	12->1:13	12->10:5"
				+"	13->1:13	13->9:11"
				+"	14->1:13	14->10:9"
				+"	15->1:13	15->9:7"
				+"	16->1:13	16->10:12"
				;
		g = AnnotatedGraph.fromStringParts(testGraph, g);
		assertEquals("node count",16,g.getNodes().size());
		assertEquals("near 11",2,g.near("11").size());
		assertEquals("near 2",5,g.near("2").size());
		assertEquals("feature count",15,g.getFeatureSet().size());
		
		String andAgain = g.toString(); System.out.println(andAgain);
		String[] testGraphParts = testGraph.split("\t");
		String[] andAgainParts = andAgain.split("\t");
		assertEquals("graph string parts",testGraphParts.length, andAgainParts.length);
		Arrays.sort(testGraphParts);
		Arrays.sort(andAgainParts);
		for (int i=0; i<testGraphParts.length; i++) {
			if (testGraphParts[i].contains("(")) {
				String[] pp1 = testGraphParts[i].split(":");
				String[] pp2 = andAgainParts[i].split(":"); 
				assertEquals("part "+i+" parts",pp1.length, pp2.length);
				Arrays.sort(pp1);
				Arrays.sort(pp2);
				for (int j=0; j<pp1.length; j++) {
					assertEquals("part "+i+" subpart "+j,pp1[j],pp2[j]);
				}
			} else if (testGraphParts[i].contains(":")) {
				assertEquals("part "+i,testGraphParts[i].substring(0,testGraphParts[i].indexOf(":")),
						andAgainParts[i].substring(0,andAgainParts[i].indexOf(":")));
			} else {
				assertEquals("part "+i,testGraphParts[i],andAgainParts[i]);
			}
		}
	}
}
