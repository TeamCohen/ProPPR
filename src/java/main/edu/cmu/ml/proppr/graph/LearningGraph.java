package edu.cmu.ml.proppr.graph;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.procedure.TIntProcedure;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;


public abstract class LearningGraph {
	private static final Logger log = Logger.getLogger(LearningGraph.class);
	public static class GraphFormatException extends Exception {
		public GraphFormatException(String msg) { super(msg); }
	}
	/** Return the neighbors of node u. */
	public List<RWOutlink> getOutlinks(final int u) {
		final List<RWOutlink> result = new ArrayList<RWOutlink>();
		near(u).forEach(new TIntProcedure() {
			@Override
			public boolean execute(int v) {
				TObjectDoubleMap<String> fd = getFeatures(u,v);
				result.add(new RWOutlink(fd,v));
				return true;
			}
		});
		return result;
	}
	/** Store the neighbors of node u */
	public abstract void addOutlink(int u, RWOutlink outlink);
	/** */
	public abstract TIntArrayList near(int u);
	public abstract TObjectDoubleMap<String> getFeatures(int u, int v);
	public abstract Set<String> getFeatureSet();
	public abstract int[] getNodes();
	public abstract int nodeSize();
	public abstract int edgeSize();
	/** No further edits will be made; optimize the graph. */
	public abstract void freeze();
}
