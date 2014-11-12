package edu.cmu.ml.proppr.prove.v1;

import java.util.HashMap;
import java.util.Map;

import edu.cmu.ml.proppr.learn.tools.WeightingScheme;

public abstract class FeatureDictWeighter {
	protected Map<Goal,Double> weights = new HashMap<Goal,Double>();
	protected WeightingScheme weightingScheme;
	public FeatureDictWeighter(WeightingScheme ws) {
		this.weightingScheme = ws;
	}
	public void put(Goal goal, double i) {
		weights.put(goal,i);
	}
	public abstract double w(Map<Goal, Double> featureDict);
	public String listing() {
		return "feature dict weighter <no string available>";
	}

}
