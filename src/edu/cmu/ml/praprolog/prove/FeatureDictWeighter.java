package edu.cmu.ml.praprolog.prove;

import java.util.HashMap;
import java.util.Map;

public abstract class FeatureDictWeighter {
	protected Map<Goal,Double> weights = new HashMap<Goal,Double>();
	public void put(Goal goal, double i) {
		weights.put(goal,i);
	}
	public abstract double w(Map<Goal, Double> featureDict);
	public String listing() {
		return "feature dict weighter <no string available>";
	}

}
