package edu.cmu.ml.praprolog.trove.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

/**
 * A Graph that supports edge features.
 * @author krivard
 *
 */
public class AnnotatedTroveGraph extends TroveGraph {
	private static final Logger log = Logger.getLogger(AnnotatedTroveGraph.class);
	protected Map<Edge,List<Feature>> features;
	protected Set<String> featureSet;
	
	public static class GraphFormatException extends Exception {
		public GraphFormatException(String msg) { super(msg); }
	}
	
	public AnnotatedTroveGraph() {
		//On map implementation choice:
		// I used to have a TreeMap here, but something is iffy with
		// Edge comparators maybe? the map kept dropping keys.
		features = new HashMap<Edge,List<Feature>>();//new TreeMap<Edge,List<Feature>>(); //
		featureSet = new HashSet<String>();
	}
	
	public List<Feature> phi(int u, int v) {
//		int u = keyToId(uid), v = keyToId(vid);
		Edge e = new Edge(u,v);
		if (features.containsKey(e)) return features.get(e);
		log.debug("Phi default: empty list");
		return Collections.emptyList();
	}
	public List<Feature> phiFromString(String u, String v) {
		return phi(keyToId(u),keyToId(v));
	}
	
	public void addDirectedEdge(String uid, String vid, List<Feature> ff) {
		addDirectedEdge(uid,vid,DEFAULT_EDGEWEIGHT,ff);
	}
	public void addDirectedEdge(String uid, String vid, double wt, List<Feature> ff) {
		addDirectedEdge(uid,vid,wt);
		
//		if (uid.equals("190") && vid.equals("968")) {
//			log.debug("190->168");
//		}
		
//		Edge e = new Edge(uid,vid);
//		if (!features.containsKey(e)) features.put(e, ff);
//		else features.get(e).addAll(ff);
		int u = keyToId(uid), v = keyToId(vid);
//		log.debug(u+","+v);
		Edge e = new Edge(u,v);
		if (features.containsKey(e)) {
			log.warn("Overwriting existing features for "+uid+"("+u+") "+":"+vid+"("+v+") (normally we expect each edge to only be added once)");
		}
		features.put(e, ff);
		for (Feature f : ff) { featureSet.add(f.featureName); }
	}


	public Set<String> getFeatureSet() {
		return featureSet;
	}
	
	
	public static AnnotatedTroveGraph fromStringParts(String string, AnnotatedTroveGraph g) throws GraphFormatException {
		String[] parts = string.split("\t",4);
		if (parts.length != 4) {
			throw new GraphFormatException("Only "+parts.length+" tsv fields in graph; need 4 distinct parts:"+string);
		}
//		AnnotatedGraph g = new AnnotatedGraph();
		
//		int numNodes = Integer.parseInt(parts[0]);
//		int numEdges = Integer.parseInt(parts[1]);
		
		String[] featureList;
		if(parts[2].equals("-")) featureList = new String[0];
		else featureList = parts[2].split(":");
		
		for (String p : parts[3].split("\t")) {
			String[] pair = p.split(":");
			String edgeStr = pair[0], featStr = pair[1];
			
			String[] nodes = edgeStr.split("->");
			ArrayList<Feature> ff = new ArrayList<Feature>();
			for (String f : featStr.split(",")) {
				if (featureList.length > 0) {
					ff.add(new Feature(featureList[Integer.parseInt(f)],1.0));
				} else {
					ff.add(new Feature(f,1.0));
				}
			}
			if (ff.isEmpty()) {
				throw new GraphFormatException("Can't have no features on an edge for ("+nodes[0]+", "+nodes[1]+")");
			}
			g.addDirectedEdge(nodes[0], nodes[1], ff);
		}
		return g;
	}

	
//	public String graphVizDump() {
//		StringBuilder sb = new StringBuilder().append("digraph annotated_graph {\n");
////		sb.append("rankdir = LR;\n");
////		sb.append("size=\"8,5\";\n");
//		sb.append("mindist=3;\nranksep=3;\n");
//		sb.append("node [shape = circle];\n");
//		for (String u : near.keySet()) {
//			for (String v : near.get(u).keySet()) {
//				sb.append(u).append(" -> ").append(v).append(" [ labeldistance=0.5 label=\"");
//				for (Feature f : phi(u,v)) {
//					sb.append(f.featureName).append(":").append(f.weight).append(" ");
//				}
//				sb.append("\" ];\n");
//			}
//		}
//		sb.append("}\n");
//		return sb.toString();
//	}
//	
//	///////////////////////////////// Dump methods -- not fully implemented /////////////////////////////////////
//	public String dump() { 
//		return dump(near.keySet().iterator().next());
//	}
//	public String dump(String rootId) {
//		StringBuilder foo = new StringBuilder().append(rootId).append("::::\n");
//		return dump(foo, rootId, 1, new TreeSet<String>()).toString();//, Collections.EMPTY_LIST).toString();
//		
//		/*
//		 * 
//		 *     def treedump(self,rootId='1',depth=0,index=None,visited=set(),incomingEdgeFeatures={}):
//        """Display a tree-like view of the graph, rooted at the given node."""
//        def fmtDict(d): return str(d)
//        if not index: 
//            index = dict((id,val) for (val,id) in self.nodeDict.items())
//        tag = ' - see above' if rootId in visited else ':'
//        print '%s%s via -%s-> [long: "%s"]%s' % (('| '*depth),rootId,fmtDict(incomingEdgeFeatures),str(index.get(rootId,'?')),tag)
//        if rootId not in visited:
//            visited.add(rootId)
//            for v in self.graph.near[rootId]:
//                self.treedump(rootId=v,depth=depth+1,index=index,visited=visited,incomingEdgeFeatures=self.graph.phi(rootId,v))
//
//		 */
//		
//
//	}
//	public StringBuilder dump(StringBuilder sb, String rootId, int depth, Set<String> visited) {//, List<Feature> incomingEdgeFeatures) {
//		for (String v : near.get(rootId).keySet()) {
//			for(int i=0; i<depth; i++) sb.append("|");
//			sb.append(v).append(":").append(Dictionary.safeGet(near,rootId,v)).append(" f{");
//			for (Feature f: phi(rootId,v)) sb.append(f.featureName).append(":").append(f.weight).append(" ");
//			sb.append("}\n");
//			if (!visited.contains(v)) {
//				visited.add(v);
//				dump(sb,v,depth+1,visited);
//			}
//		}
//		return sb;
//	}

}