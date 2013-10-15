package edu.cmu.ml.praprolog.trove.learn;

import java.util.Map;

import edu.cmu.ml.praprolog.trove.graph.AnnotatedTroveGraph;
import edu.cmu.ml.praprolog.util.Dictionary;
import gnu.trove.map.hash.TIntDoubleHashMap;

/**
 * Example for a supervised random walk, which links a query with the graph over which it is executed.
 * Example results are specified by the subclass.
 * @author krivard
 *
 */
public abstract class RWExample {
	protected TIntDoubleHashMap queryVec;
	protected AnnotatedTroveGraph graph;
	public RWExample(AnnotatedTroveGraph graph, TIntDoubleHashMap queryVec) {
		this.queryVec = queryVec;
		this.graph = graph;
	}
	public RWExample(AnnotatedTroveGraph graph, Map<String,Double> hrQueryVec) {
		queryVec = new TIntDoubleHashMap();
		for (Map.Entry<String, Double> e : hrQueryVec.entrySet()) {
			queryVec.put(graph.keyToId(e.getKey()), e.getValue());
		}
		this.graph = graph;
	}
	public TIntDoubleHashMap getQueryVec() {
		return queryVec;
	}
	public AnnotatedTroveGraph getGraph() {
		return graph;
	}
	public String toString() {
		StringBuilder sb = new StringBuilder(queryVec.size()+" queries:");
		Dictionary.buildString(queryVec, sb, " ");
		return sb.toString();
	}
	/**
	 * Give the length of the example value.
	 * @return
	 */
	public abstract int length();
}
