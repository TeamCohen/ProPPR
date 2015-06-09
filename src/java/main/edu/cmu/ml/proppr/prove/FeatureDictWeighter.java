package edu.cmu.ml.proppr.prove;

import java.util.HashMap;
import java.util.Map;

import edu.cmu.ml.proppr.learn.tools.SquashingFunction;
import edu.cmu.ml.proppr.prove.wam.Goal;
import edu.cmu.ml.proppr.util.Dictionary;

public abstract class FeatureDictWeighter {
	protected Map<Goal,Double> weights = new HashMap<Goal,Double>();
	protected SquashingFunction squashingFunction;
	public FeatureDictWeighter(SquashingFunction ws) {
		this.squashingFunction = ws;
	}
	public void put(Goal goal, double i) {
		weights.put(goal,i);
	}
	public abstract double w(Map<Goal, Double> featureDict);
	public String listing() {
		return "feature dict weighter <no string available>";
	}
}
