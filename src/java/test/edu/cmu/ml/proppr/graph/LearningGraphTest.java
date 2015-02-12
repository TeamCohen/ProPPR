package edu.cmu.ml.proppr.graph;

import static org.junit.Assert.*;

import org.junit.Test;

import edu.cmu.ml.proppr.graph.LearningGraph.GraphFormatException;

public class LearningGraphTest {
	
	@Test
	public void testArray() throws GraphFormatException {
		String s = "2	1	foo	1->2:1";
		LearningGraphBuilder b = new ArrayLearningGraph.ArrayLearningGraphBuilder();
		ArrayLearningGraph g = (ArrayLearningGraph) b.deserialize(s);
		assertEquals(2,g.nodeSize());
		assertEquals(1,g.edgeSize());
		assertEquals(1,g.node_near_hi[1] - g.node_near_lo[1]);
		assertEquals(1,g.edge_labels_hi[0] - g.edge_labels_lo[0]);
	}

}
