package edu.cmu.ml.praprolog.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.cmu.ml.praprolog.graph.v1.Edge;
import edu.cmu.ml.praprolog.prove.wam.Feature;
import edu.cmu.ml.praprolog.prove.wam.Goal;
import edu.cmu.ml.praprolog.prove.wam.Outlink;
import edu.cmu.ml.praprolog.prove.wam.State;
import edu.cmu.ml.praprolog.trove.graph.AnnotatedTroveGraph.GraphFormatException;

/**
 * Template for a weighted graph.
 * @author krivard
 *
 */
public abstract class AnnotatedGraph<K> {
	private static final Logger log = Logger.getLogger(AnnotatedGraph.class);
	public static class GraphFormatException extends Exception {
		public GraphFormatException(String msg) { super(msg); }
	}
	/** Return the root of the graph. */
	public abstract K getRoot();
	/** Return the neighbors of node u. */
	public List<Outlink> getOutlinks(K u) {
		List<Outlink> result = new ArrayList<Outlink>();
		for (K v : near(u)) {
			Map<Goal,Double> fd = getFeatures(u,v);
			result.add(new Outlink(fd,asState(v)));
		}
		return result;
	}
	/** Store the neighbors of node u */
	public abstract void setOutlinks(K u, List<Outlink> outlinks);
	/** See if the outlinks for u have been stored. */
	public abstract boolean outlinksDefined(K u);
	public abstract List<K> near(K u);
	public abstract Map<Goal,Double> getFeatures(K u, K v);
	public abstract State asState(K u);
	public abstract int nodeSize();
	public abstract int edgeSize();
	public abstract String serialize();
	public abstract void deserialize(String s);
	
	public String toString() {
		return this.serialize();
	}
}