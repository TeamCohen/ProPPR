package edu.cmu.ml.proppr.graph;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.procedure.TIntProcedure;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;


public abstract class LearningGraph<F> {
	private static final Logger log = Logger.getLogger(LearningGraph.class);
	public static class GraphFormatException extends Exception {
		public GraphFormatException(String msg) { super(msg); }
	}
	/** Return the neighbors of node u. */
	public List<RWOutlink<F>> getOutlinks(final int u) {
		final List<RWOutlink<F>> result = new ArrayList<RWOutlink<F>>();
		near(u).forEach(new TIntProcedure() {
			@Override
			public boolean execute(int v) {
				TObjectDoubleMap<F> fd = getFeatures(u,v);
				result.add(new RWOutlink<F>(fd,v));
				return true;
			}
		});
		return result;
	}
	/** Store the neighbors of node u */
	public abstract void addOutlink(int u, RWOutlink<F> outlink);
	public abstract TIntArrayList near(int u);
	public abstract TObjectDoubleMap<F> getFeatures(int u, int v);
	public abstract Set<F> getFeatureSet();
	public abstract int[] getNodes();
	public abstract int nodeSize();
	public abstract int edgeSize();
}
