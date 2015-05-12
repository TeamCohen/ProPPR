package edu.cmu.ml.proppr.examples;

import edu.cmu.ml.proppr.graph.LearningGraph;
import gnu.trove.map.TIntDoubleMap;

public class DprExample extends PosNegRWExample {
	public double[] r;
	public TIntDoubleMap[] dr;
	public DprExample(String name, LearningGraph graph, TIntDoubleMap queryVec,
			int[] pos, int[] neg) {
		super(name, graph, queryVec, pos, neg);
	}
}
