package edu.cmu.ml.proppr.graph;

import java.util.Set;
import java.util.TreeSet;

import edu.cmu.ml.proppr.util.SymbolTable;

public class ArrayLearningGraph extends LearningGraph {
	public final SymbolTable<String> featureLibrary;
	// length = #feature assignments (= sum(edge) #features on that edge)
	public int[] label_feature_id;
	public double[] label_feature_weight;
	
	// length = #edges
	public int[] edge_dest;
	public int[] edge_labels_lo;
	public int[] edge_labels_hi;
	
	// length = #nodes
	public int[] node_near_lo;
	public int[] node_near_hi;
	
	// node_lo = 0;
	public int node_hi;
	
	private int index=0;
	
	public ArrayLearningGraph(SymbolTable<String> fL) {
		this.featureLibrary = fL;
	}

	@Override
	public Set<String> getFeatureSet() {
		TreeSet<String> features = new TreeSet<String>();
		for (int i : label_feature_id) {
			String f = featureLibrary.getSymbol(i);
			if (!features.contains(f)) features.add(f);
		}
		return features;
	}

	@Override
	public int[] getNodes() {
		int[] nodes = new int[node_hi];
		for (int i=0; i<node_hi; i++) nodes[i] = i;
		return nodes;
	}

	@Override
	public int nodeSize() {
		return node_hi-index;
	}

	@Override
	public int edgeSize() {
		return edge_dest.length;
	}
	
	public void setIndex(int i) {
		this.index = i;
	}
	
	public void serialize(StringBuilder serialized) {
		serialized.append(nodeSize()) // nodes
		.append("\t").append(edgeSize()) //edges
		.append("\t");
		for (int i = 0; i<getFeatureSet().size(); i++) {
			if (i>0) serialized.append(":");
			serialized.append(featureLibrary.getSymbol(i+1));
		}
		for (int u=0; u<node_hi; u++) {
			for (int ec=node_near_lo[u]; ec<node_near_hi[u]; ec++) {
				int v = edge_dest[ec];
				serialized.append("\t").append(u).append("->").append(v).append(":");
				for (int lc = edge_labels_lo[ec]; lc < edge_labels_hi[ec]; lc++) {
					if (lc > edge_labels_lo[ec]) serialized.append(",");
					serialized.append(label_feature_id[lc]).append("@").append(label_feature_weight[lc]);
				}
			}
		}
	}
}
