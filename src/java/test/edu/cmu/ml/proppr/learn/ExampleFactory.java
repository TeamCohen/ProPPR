package edu.cmu.ml.proppr.learn;

import edu.cmu.ml.proppr.examples.DprExample;
import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.examples.PprExample;
import edu.cmu.ml.proppr.graph.LearningGraph;
import gnu.trove.map.TIntDoubleMap;

public abstract class ExampleFactory {
		public abstract PosNegRWExample makeExample(String name, LearningGraph graph, TIntDoubleMap queryVec,
			int[] pos, int[] neg);
	public static class PprExampleFactory extends ExampleFactory {
		@Override
		public PosNegRWExample makeExample(String name, LearningGraph graph,
				TIntDoubleMap query, int[] pos, int[] neg) {
			return new PprExample(name, graph, query, pos, neg);
		}
	}
	public static class DprExampleFactory extends ExampleFactory {
		@Override
		public PosNegRWExample makeExample(String name, LearningGraph graph,
				TIntDoubleMap query, int[] pos, int[] neg) {
			return new DprExample(name,graph, query, pos, neg);
		}
	}
}
