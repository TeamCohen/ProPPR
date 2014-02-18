package edu.cmu.ml.praprolog.prove;

import java.util.Map;

import edu.cmu.ml.praprolog.learn.LinearWeightingScheme;

/**
 * Weight all features uniformly as 1.0
 * @author krivard
 *
 */
public class UniformWeighter extends FeatureDictWeighter {
	public UniformWeighter() {
		super(new LinearWeightingScheme());
	}

	@Override
	public double w(Map<Goal, Double> featureDict) {
		return this.weightingScheme.edgeWeight(this.weights,featureDict);
	}

}
