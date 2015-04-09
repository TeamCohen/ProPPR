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

//		int last = parts[3], next;
		//for (String p : string.substring(parts[3]+1).split("\t")) {
		for (int last = start, next=0; true ; last=next, next = string.indexOf('\t',last+1) ) {
			if (next == -1) next = string.length();
			if (next == last) break;
			
			int edgeDelim = string.indexOf(EDGE_FEATURE_DELIM,last+1);
//			String[] pair = p.split(EDGE_FEATURE_DELIM);
//			String edgeStr = pair[0], featStr = pair[1];

//			String[] raw = edgeStr.split(SRC_DST_DELIM);
			int nodeDelim = string.indexOf(SRC_DST_DELIM, last+1);
			int[] nodes = {Integer.parseInt(string.substring(last+1,nodeDelim)), 
					Integer.parseInt(string.substring(nodeDelim+SRC_DST_DELIM.length(), edgeDelim)) };
//			int[] nodes = new int[raw.length];
//			for (int i=0; i<raw.length; i++) { nodes[i] = Integer.parseInt(raw[i]); }

			TObjectDoubleMap<String> fd = new TObjectDoubleHashMap<String>();
			handleEdgeFeatures(string, edgeDelim, next, fd, featureList);
			if (fd.isEmpty()) {
				throw new GraphFormatException("Can't have no features on an edge for ("+nodes[0]+", "+nodes[1]+")");
			}
			addOutlink(graph, nodes[0], new RWOutlink(fd, nodes[1]));
		}
	}
	
	private void handleEdgeFeatures(String string, int start, int next, TObjectDoubleMap<String> fd, String[] featureList) {

//		for (String f : featStr.split(FEATURE_DELIM)) {
		for (int lastF = start, nextF = 0; true ; lastF=nextF, nextF = Math.min(next, string.indexOf(FEATURE_DELIM, lastF+1)) ) {
			if (nextF == -1) nextF = next;
			if (nextF == lastF) break;
			
			double wt = 1.0;
			String f = string.substring(lastF+1, nextF);
			if (featureList.length > 0) {
				int weightDelim = f.indexOf(FEATURE_WEIGHT_DELIM);
				if (weightDelim>0) { // @ is never the first char in the feature string
					wt = Double.parseDouble(f.substring(weightDelim+1));
					f = f.substring(0,weightDelim);
				}
//				String[] fw = f.split(FEATURE_WEIGHT_DELIM);
				fd.put(featureList[Integer.parseInt(f)-1],wt);
			} else {
				fd.put(f,wt);
			}
		}
	}
}
