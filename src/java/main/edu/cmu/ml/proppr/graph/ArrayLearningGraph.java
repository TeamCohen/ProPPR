package edu.cmu.ml.proppr.graph;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

import edu.cmu.ml.proppr.util.SymbolTable;
import gnu.trove.iterator.TObjectDoubleIterator;

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
	
	public static class ArrayLearningGraphBuilder extends LearningGraphBuilder {
		SymbolTable<String> featureLibrary = new SymbolTable<String>();
		ArrayLearningGraph current = null;
		public ArrayList<RWOutlink>[] outlinks = null;
		int labelSize=0;
		int index=0;
		
		@Override
		public LearningGraph create() {
			if (current != null) throw new IllegalStateException("ArrayLearningGraphBuilder not threadsafe");
			current =  new ArrayLearningGraph(featureLibrary);
			return current;
		}

		@Override
		public void index(int i0) {
			if (outlinks != null) throw new IllegalStateException("Bad Programmer: You must call index() BEFORE setGraphSize().");
			this.index = i0;
			current.setIndex(i0);
		}
		
		@Override
		public void setGraphSize(LearningGraph g, int nodeSize, int edgeSize) {
			if (!current.equals(g)) throw new IllegalStateException("ArrayLearningGraphBuilder not threadsafe");
			nodeSize += index;
			current.node_hi = nodeSize;
			current.node_near_hi = new int[nodeSize];
			current.node_near_lo = new int[nodeSize];
			outlinks = new ArrayList[nodeSize];
			if (edgeSize < 0) return;
			initEdges(edgeSize);
		}

		private void initEdges(int edgeSize) {
			current.edge_dest = new int[edgeSize];
			current.edge_labels_hi = new int[edgeSize];
			current.edge_labels_lo = new int[edgeSize];
			
		}
		
		@Override
		public void addOutlink(LearningGraph g, int u, RWOutlink rwOutlink) {
			if (!current.equals(g)) throw new IllegalStateException("ArrayLearningGraphBuilder not threadsafe");
			if (outlinks[u] == null) outlinks[u] = new ArrayList<RWOutlink>();
			if (rwOutlink != null) {
				outlinks[u].add(rwOutlink);
				labelSize += rwOutlink.fd.size();
			} else labelSize++;
		}

		@Override
		public void freeze(LearningGraph g) {
			if (!current.equals(g)) throw new IllegalStateException("ArrayLearningGraphBuilder not threadsafe");
			current.label_feature_id = new int[labelSize];
			current.label_feature_weight = new double[labelSize];
			if (current.edge_dest == null) {
				// then figure out size empirically and initialize
				int edgeSize=0;
				for (int u=0; u<current.node_hi; u++) {
					if (outlinks[u] == null) continue;
					edgeSize += outlinks[u].size();
				}
				initEdges(edgeSize);
			}
			int edge_cursor=0;
			int label_cursor=0;
			for (int u=0; u<current.node_hi; u++) {
				current.node_near_lo[u]=edge_cursor;
				if (outlinks[u] != null) {
					for (RWOutlink o : outlinks[u]) {
						current.edge_dest[edge_cursor] = o.nodeid;
						current.edge_labels_lo[edge_cursor] = label_cursor;
						for (TObjectDoubleIterator<String> it = o.fd.iterator(); it.hasNext(); ) {
							it.advance();
							current.label_feature_id[label_cursor] = featureLibrary.getId(it.key());
							current.label_feature_weight[label_cursor] = it.value();
							label_cursor++;
						}
						current.edge_labels_hi[edge_cursor] = label_cursor;
						edge_cursor++;
					}
				}
				current.node_near_hi[u]=edge_cursor;
			}
			init();
		}
		
		private void init() {
			current = null;
			outlinks = null;
			labelSize = 0;
			index = 0;
		}
		
	}
}
