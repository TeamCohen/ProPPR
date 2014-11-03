package edu.cmu.ml.praprolog.prove;

import java.util.Map;

import edu.cmu.ml.praprolog.learn.tools.LinearWeightingScheme;
import edu.cmu.ml.praprolog.learn.tools.WeightingScheme;
import edu.cmu.ml.praprolog.prove.wam.Goal;

/**
 * Weight all features uniformly as 1.0
 * @author krivard
 *
 */
public class UniformWeighter extends FeatureDictWeighter {
	public UniformWeighter(WeightingScheme wScheme) {
		super(wScheme);
	}
	public UniformWeighter() {
		this(new LinearWeightingScheme());
	}

	@Override
	public double w(Map<Goal, Double> featureDict) {
		return this.weightingScheme.edgeWeight(this.weights,featureDict);
	}

}
