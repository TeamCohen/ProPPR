package edu.cmu.ml.praprolog.examples;

import java.util.Map;

import edu.cmu.ml.praprolog.graph.AnnotatedGraph;


/**
 * Example for a supervised random walk, which links a query with the graph over which it is executed.
 * Example results are specified by the subclass.
 * @author krivard
 *
 */
public abstract class RWExample<T> {
	protected Map<T,Double>queryVec;
	protected AnnotatedGraph<T> graph;
	public RWExample(AnnotatedGraph<T> graph, Map<T,Double>queryVec) {
		this.queryVec = queryVec;
		this.graph = graph;
	}
	public Map<T, Double> getQueryVec() {
		return queryVec;
	}
	public AnnotatedGraph<T> getGraph() {
		return graph;
	}
	public String toString() {
		StringBuilder sb = new StringBuilder(queryVec.size()+" queries:");
		for(Map.Entry<T,Double> k:queryVec.entrySet()) { sb.append(" ").append(k.getKey()).append(":").append(k.getValue());}
		return sb.toString();
	}
	/**
	 * Give the length of the example value.
	 * @return
	 */
	public abstract int length();
}
