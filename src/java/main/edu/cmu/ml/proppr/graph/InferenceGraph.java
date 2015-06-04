package edu.cmu.ml.proppr.graph;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.prove.wam.Goal;
import edu.cmu.ml.proppr.prove.wam.Outlink;
import edu.cmu.ml.proppr.prove.wam.State;

/**
 * Template for a weighted graph.
 * @author krivard
 *
 */
public interface InferenceGraph {
//	private static final Logger log = Logger.getLogger(InferenceGraph.class);
//	/** Return the root of the graph. */
//	public abstract State getRoot();
//	/** Store the neighbors of node u */
//	public abstract void setOutlinks(State u, List<Outlink> outlinks);
//	/** See if the outlinks for u have been stored. */
//	public abstract boolean outlinksDefined(State u);
//	public abstract List<State> near(State u);
//	public abstract Map<Goal,Double> getFeatures(State u, State v);
//	public abstract int getId(State state);
	
	public abstract State getState(int id);
	public abstract int nodeSize();
	public abstract int edgeSize();
	public abstract String serialize();
	
}
