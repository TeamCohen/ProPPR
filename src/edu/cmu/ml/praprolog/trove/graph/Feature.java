package edu.cmu.ml.praprolog.trove.graph;
/**
 * Encodes a named feature value suitable for use as a key or a set member.
 * @author krivard
 *
 */
public class Feature implements Comparable<Feature> {
	public String featureName;
	public double weight;
	public Feature(String f, double w) {
		featureName = f;
		weight = w;
	}
	
	@Override
	public int hashCode() { return featureName.hashCode(); }
	@Override
	public int compareTo(Feature o) {
		int result = o.featureName.compareTo(featureName);
		if(result == 0) return Double.compare(o.weight, weight);
		return result;
	}

}

