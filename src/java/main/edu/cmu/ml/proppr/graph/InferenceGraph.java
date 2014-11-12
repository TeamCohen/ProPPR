package edu.cmu.ml.proppr.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.graph.v1.Edge;
import edu.cmu.ml.proppr.prove.wam.Feature;
import edu.cmu.ml.proppr.prove.wam.Goal;
import edu.cmu.ml.proppr.prove.wam.Outlink;
import edu.cmu.ml.proppr.prove.wam.State;

/**
 * Template for a weighted graph.
 * @author krivard
 *
 */
public abstract class InferenceGraph {
	private static final Logger log = Logger.getLogger(InferenceGraph.class);
	/** Return the root of the graph. */
	public abstract State getRoot();
	/** Return the neighbors of node u. */
	public List<Outlink> getOutlinks(State u) {
		List<Outlink> result = new ArrayList<Outlink>();
		for (State v : near(u)) {
			Map<Goal,Double> fd = getFeatures(u,v);
			result.add(new Outlink(fd,v));
		}
		return result;
	}
	/** Store the neighbors of node u */
	public abstract void setOutlinks(State u, List<Outlink> outlinks);
	/** See if the outlinks for u have been stored. */
	public abstract boolean outlinksDefined(State u);
	public abstract List<State> near(State u);
	public abstract Map<Goal,Double> getFeatures(State u, State v);
	public abstract State asState(State u);
	public abstract int nodeSize();
	public abstract int edgeSize();
	public abstract String serialize();
	
	public String toString() {
		return this.serialize();
	}
}