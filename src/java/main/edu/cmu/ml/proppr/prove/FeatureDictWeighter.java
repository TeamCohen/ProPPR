package edu.cmu.ml.proppr.prove;

import java.util.HashMap;
import java.util.Map;

import edu.cmu.ml.proppr.learn.tools.WeightingScheme;
import edu.cmu.ml.proppr.prove.wam.Goal;
import edu.cmu.ml.proppr.util.Dictionary;

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
	public double boosted(Goal feature, double wNew,
			Map<Goal, Double> featureDict) {
		double boosted = this.weightingScheme.inverseEdgeWeightFunction(wNew);
		for (Map.Entry<Goal,Double> f : featureDict.entrySet()) {
			if (f.getKey().equals(feature)) continue;
			boosted -= Dictionary.safeGet(this.weights, f.getKey(), this.weightingScheme.defaultWeight()) * f.getValue();
		}
		return boosted / Dictionary.safeGet(this.weights, feature, this.weightingScheme.defaultWeight());
	}
}
