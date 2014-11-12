package edu.cmu.ml.proppr.graph;

import java.util.Set;
import java.util.TreeSet;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;

/**
 * Straightforward implementation using three hash maps for u -> v -> f -> w.
 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
 *
 */
public class SimpleLearningGraph extends LearningGraph<String> {
	private static final TIntArrayList EMPTY_LIST = new TIntArrayList();
	private static final TObjectDoubleMap<String> EMPTY_MAP = new TObjectDoubleHashMap<String>();
	protected TIntObjectMap<TIntArrayList> near = new TIntObjectHashMap<TIntArrayList>();
	protected TIntObjectMap<TIntObjectMap<TObjectDoubleMap<String>>> phi = 
			new TIntObjectHashMap<TIntObjectMap<TObjectDoubleMap<String>>>();
	protected Set<String> features = new TreeSet<String>();
	protected int edgeSize = 0;

	public static class SLGBuilder extends LearningGraphBuilder<String> {
		@Override
		public LearningGraph<String> create() {
			return new SimpleLearningGraph();
		}
		@Override
		public String parseFeature(String f) {
			return f;
		}
	}
	
	private void ensureNode(int n) {
		if (!near.containsKey(n)) {
			near.put(n, new TIntArrayList());
			phi.put(n, new TIntObjectHashMap<TObjectDoubleMap<String>>());
		}
	}

	@Override
	public void addOutlink(int u, RWOutlink<String> outlink) {
		ensureNode(u);
		ensureNode(outlink.nodeid);
		near.get(u).add(outlink.nodeid);
		phi.get(u).put(outlink.nodeid,outlink.fd);
		for (String f : outlink.fd.keys(new String[0])) features.add(f);
		edgeSize++;
	}


	@Override
	public TIntArrayList near(int u) {
		if (near.containsKey(u)) return near.get(u);
		return EMPTY_LIST;
	}


	@Override
	public TObjectDoubleMap<String> getFeatures(int u, int v) {
		if (phi.containsKey(u)) {
			TIntObjectMap<TObjectDoubleMap<String>> phiU = phi.get(u);
			if (phiU.containsKey(v)) return phiU.get(v);
		}
		return EMPTY_MAP;
	}


	@Override
	public Set<String> getFeatureSet() {
		return this.features;
	}


	@Override
	public int[] getNodes() {
		return near.keys();
	}


	@Override
	public int nodeSize() {
		return near.size();
	}


	@Override
	public int edgeSize() {
		return edgeSize;
	}
}
