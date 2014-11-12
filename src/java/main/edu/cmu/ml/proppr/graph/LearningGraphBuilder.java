package edu.cmu.ml.proppr.graph;

import edu.cmu.ml.proppr.graph.LearningGraph.GraphFormatException;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;

public abstract class LearningGraphBuilder<F> {
	public abstract LearningGraph<F> create();
	public abstract F parseFeature(String f);
	public LearningGraph<F> deserialize(String string) throws GraphFormatException {
		String[] parts = string.split("\t",4);
		if (parts.length != 4) {
			throw new GraphFormatException("Only "+parts.length+" tsv fields in graph; need 4 distinct parts:"+string);
		}

		String[] featureList;
		if(parts[2].equals("-")) featureList = new String[0];
		else featureList = parts[2].split(":");

		LearningGraph<F> graph = create();
		for (String p : parts[3].split("\t")) {
			String[] pair = p.split(":");
			String edgeStr = pair[0], featStr = pair[1];

			String[] raw = edgeStr.split("->");
			int[] nodes = new int[raw.length];
			for (int i=0; i<raw.length; i++) { nodes[i] = Integer.parseInt(raw[i]); }

			TObjectDoubleMap<F> fd = new TObjectDoubleHashMap<F>();
			for (String f : featStr.split(",")) {
				if (featureList.length > 0) {
					fd.put(parseFeature(featureList[Integer.parseInt(f)]),1.0);
				} else {
					fd.put(parseFeature(f),1.0);
				}
			}
			if (fd.isEmpty()) {
				throw new GraphFormatException("Can't have no features on an edge for ("+nodes[0]+", "+nodes[1]+")");
			}
			graph.addOutlink(nodes[0], new RWOutlink<F>(fd, nodes[1]));
		}
		return graph;
	}
}
