package edu.cmu.ml.proppr.graph;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;

import org.junit.Test;

import edu.cmu.ml.proppr.graph.LearningGraph.GraphFormatException;

public class LearningGraphTest {
	
	@Test
	public void testArray() throws GraphFormatException {
		String s = "3	2	foo	1->2:1	3->2:1";
		LearningGraphBuilder b = new ArrayLearningGraphBuilder();
		ArrayLearningGraph g = (ArrayLearningGraph) b.deserialize(s);
		assertEquals("#nodes",3,g.nodeSize());
		assertEquals("#edges",2,g.edgeSize());
		assertEquals("#edges on 1",1,g.node_near_hi[1] - g.node_near_lo[1]);
		assertEquals("#features on 1->2",1,g.edge_labels_hi[0] - g.edge_labels_lo[0]);
	}

	
	@Test
	public void testCoraArray() throws GraphFormatException, IOException {
		LineNumberReader r = new LineNumberReader(new FileReader("examples/cora/train.examples.1.grounded"));
		String s = r.readLine().split("\t",5)[4];
		r.close();
		LearningGraphBuilder b = new ArrayLearningGraphBuilder();
		ArrayLearningGraph g = (ArrayLearningGraph) b.deserialize(s);
	}
}
