package edu.cmu.ml.proppr.prove;

import java.util.HashMap;
import java.util.Map;

import com.skjegstad.utils.BloomFilter;

import edu.cmu.ml.proppr.learn.tools.SquashingFunction;
import edu.cmu.ml.proppr.prove.wam.Feature;
import edu.cmu.ml.proppr.prove.wam.Goal;
import edu.cmu.ml.proppr.util.Dictionary;

public abstract class FeatureDictWeighter {
	protected Map<Feature, Double> weights;
	protected SquashingFunction squashingFunction;
	protected int numUnknownFeatures = 0;
	protected int numKnownFeatures = 0;
	protected BloomFilter<Feature> unknownFeatures;
	protected BloomFilter<Feature> knownFeatures;
	public FeatureDictWeighter(SquashingFunction ws) {
		this(ws,new HashMap<Feature,Double>());
	}
	public FeatureDictWeighter(SquashingFunction ws, Map<Feature,Double> w) {
		this.squashingFunction = ws;
		this.weights = w;
		this.unknownFeatures = new BloomFilter<Feature>(.01,Math.max(100, weights.size()));
		this.knownFeatures = new BloomFilter<Feature>(.01,Math.max(100, weights.size()));
	}
	public void put(Feature goal, double i) {
		weights.put(goal,i);
	}
	public abstract double w(Map<Feature, Double> fd);
	public String listing() {
		return "feature dict weighter <no string available>";
	}
	public SquashingFunction getSquashingFunction() { return squashingFunction; }
	public void countFeature(Feature g) {
		if (!this.weights.containsKey(g)) {
			if (!unknownFeatures.contains(g)) {
				unknownFeatures.add(g);
				numUnknownFeatures++;
			}
		} else if (!knownFeatures.contains(g)) {
			knownFeatures.add(g);
			numKnownFeatures++;
		}
	}
}
