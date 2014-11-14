package edu.cmu.ml.proppr.graph;

import static org.junit.Assert.*;

import org.junit.Test;

import edu.cmu.ml.proppr.graph.LearningGraph.GraphFormatException;
import edu.cmu.ml.proppr.graph.SimpleLearningGraph.SLGBuilder;

public class LearningGraphTest {

	@Test
	public void test() throws GraphFormatException {
		String s = "2	1	foo	1->2:1";
		LearningGraphBuilder b = new SLGBuilder();
		LearningGraph g = b.deserialize(s);
		assertEquals(2,g.nodeSize());
		assertEquals(1,g.edgeSize());
		assertEquals(1,g.near(1).size());
		assertEquals(1,g.getFeatures(1, 2).size());
	}

}
