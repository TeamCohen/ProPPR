package edu.cmu.ml.proppr.graph.v1;

import org.junit.Before;

import edu.cmu.ml.proppr.graph.v1.AnnotatedIntGraph;

public class AnnotatedIntGraphTest extends AnnotatedGraphTest {

	@Before @Override
	public void setup() {
		g = new AnnotatedIntGraph();
	}
}
