package edu.cmu.ml.proppr.graph;

import edu.cmu.ml.proppr.graph.LearningGraph.GraphFormatException;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;

public abstract class LearningGraphBuilder {
	public static final char FEATURE_INDEX_DELIM = ':';
	public static final char EDGE_FEATURE_DELIM = ':';
	public static final String SRC_DST_DELIM = "->";
	public static final char FEATURE_DELIM = ',';
	public static final char FEATURE_WEIGHT_DELIM = '@';
	
	public abstract LearningGraphBuilder copy();
	public abstract LearningGraph create();
	public abstract void setGraphSize(LearningGraph g, int nodeSize, int edgeSize);
	public abstract void addOutlink(LearningGraph g, int u, RWOutlink rwOutlink);
	public abstract void freeze(LearningGraph g);
	public abstract void index(int i0);
	public LearningGraph deserialize(String string) throws GraphFormatException {
		int[] parts = new int[3];
		parts[0] = string.indexOf('\t');
		for (int i=1; i<parts.length; i++) parts[i] = string.indexOf('\t', parts[i-1]+1);
		
		if (parts[2] == -1) {
			throw new GraphFormatException("Need 4 distinct tsv fields in the graph:"+string);
		}

		int nodeSize = Integer.parseInt(string.substring(0, parts[0]));
		int edgeSize = Integer.parseInt(string.substring(parts[0]+1,parts[1]));
		LearningGraph graph = create();
		index(1);
		setGraphSize(graph, nodeSize, edgeSize);

		String[] featureList;
		if(parts[2]-parts[1]==2 && string.charAt(parts[1]+1) == ('-')) featureList = new String[0];
		else featureList = string.substring(parts[1]+1,parts[2]).split(String.valueOf(FEATURE_INDEX_DELIM));

		handleEdgeList(string, parts[2], graph, featureList);
		
		freeze(graph); // might be slow? but saves memory
		return graph;
	}

	private void handleEdgeList(String string, int start, LearningGraph graph, String[] featureList) throws GraphFormatException {
		StringBuilder edge = new StringBuilder();
		int featureStart = -1;
		int[] nodes = {-1,-1};
		
		for (int i=start+1; i<string.length(); i++) {
			char c = string.charAt(i);
			if (c == EDGE_FEATURE_DELIM) {
				int nodeDelim = edge.indexOf(SRC_DST_DELIM);
				nodes[0] = Integer.parseInt(edge.substring(0,nodeDelim));
				nodes[1] = Integer.parseInt(edge.substring(nodeDelim+SRC_DST_DELIM.length()));
				featureStart = edge.length();
			} else if (c == '\t') {
				TObjectDoubleMap<String> fd = new TObjectDoubleHashMap<String>();
				handleEdgeFeatureList(edge.toString(), featureStart, fd, featureList);
				if (fd.isEmpty()) {
					throw new GraphFormatException("Can't have no features on an edge for ("+nodes[0]+", "+nodes[1]+")");
				}
				addOutlink(graph, nodes[0], new RWOutlink(fd, nodes[1]));
				edge = new StringBuilder();
			} else edge.append(c);
		}
		TObjectDoubleMap<String> fd = new TObjectDoubleHashMap<String>();
		handleEdgeFeatureList(edge.toString(), featureStart, fd, featureList);
		if (fd.isEmpty()) {
			throw new GraphFormatException("Can't have no features on an edge for ("+nodes[0]+", "+nodes[1]+")");
		}
		addOutlink(graph, nodes[0], new RWOutlink(fd, nodes[1]));
	}
	
	private void handleEdgeFeatureList(String edge, int start, TObjectDoubleMap<String> fd, String[] featureList) {
		StringBuilder f = new StringBuilder();
		for (int i=start; i<edge.length(); i++) {
			char c = edge.charAt(i);
			if (c == FEATURE_DELIM) {
				handleEdgeFeature(f.toString(),fd,featureList);
				f = new StringBuilder();
			} else f.append(c);
		}
		handleEdgeFeature(f.toString(),fd,featureList);
	}
	private void handleEdgeFeature(String f, TObjectDoubleMap<String> fd, String[] featureList) {
		double wt = 1.0;
		if (featureList.length > 0) {
			int weightDelim = f.indexOf(FEATURE_WEIGHT_DELIM);
			if (weightDelim>0) { // @ is never the first char in the feature string
				wt = Double.parseDouble(f.substring(weightDelim+1));
				f = f.substring(0,weightDelim);
			}
			fd.put(featureList[Integer.parseInt(f)-1],wt);
		} else {
			fd.put(f,wt);
		}
	}
}
