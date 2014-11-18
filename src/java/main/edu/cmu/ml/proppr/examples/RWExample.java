package edu.cmu.ml.proppr.examples;

import java.util.Map;

import edu.cmu.ml.proppr.graph.InferenceGraph;
import edu.cmu.ml.proppr.graph.LearningGraph;
import edu.cmu.ml.proppr.util.Dictionary;
import gnu.trove.map.TIntDoubleMap;


/**
 * Example for a supervised random walk, which links a query with the graph over which it is executed.
 * Example results are specified by the subclass.
 * @author krivard
 *
 */
public abstract class RWExample {
	protected TIntDoubleMap queryVec;
	protected LearningGraph graph;
	public RWExample(LearningGraph graph, TIntDoubleMap queryVec) {
		this.queryVec = queryVec;
		this.graph = graph;
	}
	public TIntDoubleMap getQueryVec() {
		return queryVec;
	}
	public LearningGraph getGraph() {
		return graph;
	}
	public String toString() {
		StringBuilder sb = new StringBuilder(queryVec.size()+" queries:");
		Dictionary.buildString(queryVec,sb," ");
		return sb.toString();
	}
	/**
	 * Give the length of the example value.
	 * @return
	 */
	public abstract int length();
}
