package edu.cmu.ml.praprolog.graph;

import org.junit.Before;

public class AnnotatedIntGraphTest extends AnnotatedGraphTest {

	@Before @Override
	public void setup() {
		g = new AnnotatedIntGraph();
	}
}
