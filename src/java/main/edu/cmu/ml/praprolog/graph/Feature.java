package edu.cmu.ml.praprolog.graph;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.cmu.ml.praprolog.prove.Goal;

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

	/**
	 * Converts from a goal weighting to a feature list. TODO: Generalize to any weighted object map,
	 * but use some interface to specify the saved string. we need "id(trueLoop)" not "goal(id,c[trueLoop])".
	 * @param featureDict
	 * @return
	 */
	public static List<Feature> toFeatureList(Map<Goal, Double> featureDict) {
		ArrayList<Feature> result = new ArrayList<Feature>();
		for (Map.Entry<Goal, Double> e : featureDict.entrySet()) {
			result.add(new Feature(e.getKey().toSaveString(),e.getValue()));
		}
		return result;
	}

	public String toString() { 
		return "F{"+featureName+":"+weight+"}"; 
	}
	
	public static boolean contains(List<Feature> list, String feature) {
		for(Feature f : list) {
			if(feature.equals(f.featureName)) {
				return true;
			}
		}
		return false;
	}
}

