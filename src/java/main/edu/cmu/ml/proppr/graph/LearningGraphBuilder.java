package edu.cmu.ml.proppr.graph;

import edu.cmu.ml.proppr.graph.LearningGraph.GraphFormatException;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;

import java.util.HashMap;
import java.util.StringTokenizer;

public abstract class LearningGraphBuilder {
	public static final char TAB = '\t';
	public static final char NODE_DELIM = ',';
	public static final char FEATURE_INDEX_DELIM = ':';
	public static final String SRC_DST_DELIM = "->";
	public static final char EDGE_DELIM = ':';
	public static final char EDGE_FEATURE_DELIM = ',';
	public static final char FEATURE_WEIGHT_DELIM = '@';

	public abstract LearningGraphBuilder copy();
	public abstract LearningGraph create();
	public abstract void setGraphSize(LearningGraph g, int nodeSize, int edgeSize);
	public abstract void addOutlink(LearningGraph g, int u, RWOutlink rwOutlink);
	public abstract void freeze(LearningGraph g);
	public abstract void index(int i0);
	public LearningGraph deserialize(String string) throws GraphFormatException {
		//		int[] parts = new int[3];
		//		parts[0] = string.indexOf('\t');
		//		for (int i=1; i<parts.length; i++) parts[i] = string.indexOf('\t', parts[i-1]+1);
		//		
		//		if (parts[2] == -1) {
		//			throw new GraphFormatException("Need 4 distinct tsv fields in the graph:"+string);
		//		}
		//
		//		int nodeSize = Integer.parseInt(string.substring(0, parts[0]));
		//		int edgeSize = Integer.parseInt(string.substring(parts[0]+1,parts[1]));
		//		LearningGraph graph = create();
		//		index(1);
		//		setGraphSize(graph, nodeSize, edgeSize);
		//
		//		String[] featureList;
		//		if(parts[2]-parts[1]==2 && string.charAt(parts[1]+1) == ('-')) featureList = new String[0];
		//		else featureList = string.substring(parts[1]+1,parts[2]).split(String.valueOf(FEATURE_INDEX_DELIM));
		//
		//		//if (true) return graph; // timepoint 0
		//
		//		//handleEdgeList(string, parts[2], graph, featureList);
		//		handleEdgeList(new StringTokenizer(string.substring(parts[2]+1),"\t"), graph, featureList);
		//		
		//		if (true) return graph; //timepoint 2; 2+1=timepoint 1A2
		//		freeze(graph); // might be slow? but saves memory
		//		return graph;

		// first parse the graph metadata
		String[] parts = new String[3];
		int last = 0,i=0;
		for (int next = last; i<parts.length; last=next+1,i++) {
			if (next == -1) 
				throw new GraphFormatException("Need 4 distinct tsv fields in the graph:"+string);
			next=string.indexOf(TAB,last);
			parts[i] = next<0?string.substring(last):string.substring(last,next);
		}
//		// query metadata is
//		// query
//		// query nodes
//		// + labels
//		// - labels
//		TIntDoubleMap queryVec = new TIntDoubleHashMap();
//		for (int q : parseNodes(parts[1])) queryVec.put(q,1.0);
//
//		int[] posList = parseNodes(parts[2]);
//		int[] negList = parseNodes(parts[3]);

		// graph metadata is
		// #nodes
		// #edges

		int nodeSize = Integer.parseInt(parts[0]);
		int edgeSize = Integer.parseInt(parts[1]);
		ArrayLearningGraphBuilder b = new ArrayLearningGraphBuilder();
		LearningGraph g = b.create();
		b.index(1);
		b.setGraphSize(g,nodeSize,edgeSize);

		// now parse the feature library
		String[] features = split(parts[2],EDGE_DELIM);

		// now parse out each edge
		int[] nodes = {-1,-1};
		for (int next=last; next!=-1; last=next+1) {
			next = string.indexOf(TAB,last);

			int srcDest = string.indexOf(SRC_DST_DELIM,last);
			int edgeDelim = string.indexOf(EDGE_DELIM,srcDest);
			String[] edgeFeatures = split(next<0?string.substring(edgeDelim+1):string.substring(edgeDelim+1,next),EDGE_FEATURE_DELIM);
			nodes[0] = Integer.parseInt(string.substring(last,srcDest));
			nodes[1] = Integer.parseInt(string.substring(srcDest+2,edgeDelim));
			// As it turns out, a trove map is slower here
			//TObjectDoubleHashMap<String> fd = new TObjectDoubleHashMap<String>();
			HashMap<String,Double> fd = new HashMap<String,Double>();
			for (String f : edgeFeatures) {
				int wtDelim = f.indexOf(FEATURE_WEIGHT_DELIM);
				int featureId = Integer.parseInt(wtDelim<0?f:f.substring(0,wtDelim));
				double featureWt = wtDelim<0?1.0:Double.parseDouble(f.substring(wtDelim+1));
				fd.put(features[featureId-1],featureWt);
			}
			b.addOutlink(g,nodes[0],new RWOutlink(fd,nodes[1]));
		}
		b.freeze(g);
		return g;
	}
	private int[] parseNodes(String string) {
		String[] nodeStrings = split(string,NODE_DELIM);
		int[] nodes = new int[nodeStrings.length];
		for (int i=0; i<nodeStrings.length; i++) {
			nodes[i] = Integer.parseInt(nodeStrings[i]);
		}
		return nodes;
	}

	private String[] split(String string, char delim) {
		if (string.length() == 0) return new String[0];
		int nitems=1;
		for (int i=0; i<string.length(); i++) if (string.charAt(i) == delim) nitems++;
		String[] items = new String[nitems];
		int last=0;
		for (int next=last,i=0; i<items.length && next!=-1; last=next+1,i++) {
			next=string.indexOf(delim,last);
			items[i]=next<0?string.substring(last):string.substring(last,next);
		}
		return items;		
	}
//	
//	private void handleEdgeList(StringTokenizer string, LearningGraph graph, String[] featureList) throws GraphFormatException {
//		int[] nodes = {-1,-1};
//		while(string.hasMoreTokens()) {
//			//String edge = string.nextToken();
//			//if (true) continue; // timepoint v
//			//int nodeDelim = edge.indexOf(SRC_DST_DELIM);
//			//int featureStart = edge.indexOf(EDGE_FEATURE_DELIM,nodeDelim);
//			nodes[0] = Integer.parseInt(string.nextToken(SRC_DST_DELIM));
//			nodes[1] = Integer.parseInt(string.nextToken(EDGE_FEATURE_DELIM));//SRC_DST_DELIM.length()));
//			handleOutlink(string.nextToken("\t"),0,featureList,nodes,graph); // w
//		}
//		// timepoint X
//	}
//	/*
//	private void handleEdgeList(String string, int start, LearningGraph graph, String[] featureList) throws GraphFormatException {
//		StringBuilder edge = new StringBuilder();
//		int featureStart = -1;
//		int[] nodes = {-1,-1};
//		int outlinks = 0;
//		int e=0;
//		for (int i=start+1; i<string.length(); i++) {
//			char c = string.charAt(i);
//			if (c == '\t') { edge = new StringBuilder(); } else edge.append(c); // timepoint e
//			if(true) continue; //timepoint d
//			if (c == EDGE_FEATURE_DELIM) {
//				int nodeDelim = edge.indexOf(SRC_DST_DELIM);
//				if(true) continue; // timepoint ii
//				nodes[0] = Integer.parseInt(edge.substring(0,nodeDelim));
//				nodes[1] = Integer.parseInt(edge.substring(nodeDelim+2));//SRC_DST_DELIM.length()));
//				featureStart = e;// edge.length(); // timepoint b
//			} else if (c == '\t') {
//				handleOutlink(edge.toString(), featureStart, featureList, nodes, graph);
//				outlinks++;
//				edge = new StringBuilder(); e=0;
//			} else { edge.append(c); e++; }
//		}
//		if (e > 0) {
//			handleOutlink(edge.toString(), featureStart, featureList, nodes, graph);
//		}
//	}
//	 */
//
//	private void handleOutlink(String edge, int featureStart, String[] featureList, int[] nodes, LearningGraph graph)  throws GraphFormatException {
//		if(true) return; // timepoint i
//		TObjectDoubleMap<String> fd = new TObjectDoubleHashMap<String>();
//		handleEdgeFeatureList(edge, featureStart, fd, featureList);
//		if (fd.isEmpty()) {
//			throw new GraphFormatException("Can't have no features on an edge for ("+nodes[0]+", "+nodes[1]+")");
//		}
//		addOutlink(graph, nodes[0], new RWOutlink(fd, nodes[1])); //timepoint c			
//	}
//
//	private void handleEdgeFeatureList(String edge, int start, TObjectDoubleMap<String> fd, String[] featureList) {
//		fd.put(featureList[0],1.0); if(true) return; // timepoint 1
//		StringBuilder f = new StringBuilder();
//		for (int i=start; i<edge.length(); i++) {
//			char c = edge.charAt(i);
//			if (c == FEATURE_DELIM) {
//				handleEdgeFeature(f.toString(),fd,featureList);
//				f = new StringBuilder();
//			} else f.append(c);
//		}
//		handleEdgeFeature(f.toString(),fd,featureList);
//	}
//	private void handleEdgeFeature(String f, TObjectDoubleMap<String> fd, String[] featureList) {
//		double wt = 1.0;
//		if (featureList.length > 0) {
//			int weightDelim = f.indexOf(FEATURE_WEIGHT_DELIM);
//			if (weightDelim>0) { // @ is never the first char in the feature string
//				wt = Double.parseDouble(f.substring(weightDelim+1));
//				f = f.substring(0,weightDelim);
//			}
//			fd.put(featureList[Integer.parseInt(f)-1],wt);
//		} else {
//			fd.put(f,wt);
//		}
//	}
}
