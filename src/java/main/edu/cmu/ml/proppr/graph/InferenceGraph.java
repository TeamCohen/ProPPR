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
	public abstract State getState(int id);
	public abstract int nodeSize();
	public abstract int edgeSize();
	/**
	 * Serialization format: tab-delimited fields
	 * 1: node count
	 * 2: edge count
	 * 3: featurename1:featurename2:featurename3:...:featurenameN
	 * 4..N: srcId->dstId:fId_1,fId_2,...,fId_k
	 * 
	 * All IDs are 1-indexed.
	 * 
	 * @return
	 */
	public abstract String serialize();
	public abstract String serialize(boolean includeFeatureIndex);
	
	/**
	 * only used for unit tests
	 */
	public abstract void setOutlinks(int id, List<Outlink> outlinks);
	public abstract int getId(State s);
	
}
