package edu.cmu.ml.proppr.graph;

import edu.cmu.ml.proppr.util.SymbolTable;
import gnu.trove.iterator.TObjectDoubleIterator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class ArrayLearningGraphBuilder extends LearningGraphBuilder {
	static final HashMap<String,ArrayLearningGraphBuilder> copies = new HashMap<String,ArrayLearningGraphBuilder>();
	LearningGraph current = null;
	public ArrayList<RWOutlink>[] outlinks = null;
	int labelSize=0;
	int index=0;
	
	@Override
	public LearningGraph create() {
		if (current != null) throw new IllegalStateException("ArrayLearningGraphBuilder not threadsafe");
		current =  new LearningGraph(new SymbolTable<String>());
		return current;
	}

	@Override
	public void index(int i0) {
		if (outlinks != null) throw new IllegalStateException("Bad Programmer: You must call index() BEFORE setGraphSize().");
		this.index = i0;
		current.setIndex(i0);
	}
	
	@Override
	public void setGraphSize(LearningGraph g, int nodeSize, int edgeSize, int dependencySize) {
		if (!current.equals(g)) throw new IllegalStateException("ArrayLearningGraphBuilder not threadsafe");
		nodeSize += index;
		current.node_hi = nodeSize;
		current.node_near_hi = new int[nodeSize];
		current.node_near_lo = new int[nodeSize];
		current.setLabelDependencies(dependencySize);
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
		int label_deps=0;
		HashSet<Integer> outgoingFeatures = null;
		for (int u=0; u<current.node_hi; u++) {
			current.node_near_lo[u]=edge_cursor;
			if (current.labelDependencySize() < 0) outgoingFeatures = new HashSet<Integer>();
			if (outlinks[u] != null) {
				for (RWOutlink o : outlinks[u]) {
					current.edge_dest[edge_cursor] = o.nodeid;
					current.edge_labels_lo[edge_cursor] = label_cursor;
					for(Map.Entry<String,Double> it : o.fd.entrySet()) {
						current.label_feature_id[label_cursor] = ((LearningGraph) g).featureLibrary.getId(it.getKey());
						current.label_feature_weight[label_cursor] = it.getValue();
						if (current.labelDependencySize() < 0) outgoingFeatures.add(current.label_feature_id[label_cursor]);
						label_cursor++;
					}
					current.edge_labels_hi[edge_cursor] = label_cursor;
					edge_cursor++;
				}
				if (current.labelDependencySize() < 0) label_deps += outgoingFeatures.size();
			}
			current.node_near_hi[u]=edge_cursor;
		}
		if (current.labelDependencySize() < 0) current.setLabelDependencies(label_deps);
		init();
	}
	
	private void init() {
		current = null;
		outlinks = null;
		labelSize = 0;
		index = 0;
	}

	@Override
	public LearningGraphBuilder copy() {
		return new ArrayLearningGraphBuilder();
	}
	
	private LearningGraphBuilder threadsafeCopy() {
		String name = Thread.currentThread().getName();
		if (!copies.containsKey(name)) copies.put(name,new ArrayLearningGraphBuilder());
		return copies.get(name);
	}
}