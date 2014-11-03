package edu.cmu.ml.praprolog.graph;

import org.junit.Before;

import edu.cmu.ml.praprolog.graph.v1.AnnotatedIntGraph;

public class AnnotatedIntGraphTest extends AnnotatedGraphTest {

	@Before @Override
	public void setup() {
		g = new AnnotatedIntGraph();
	}
}
