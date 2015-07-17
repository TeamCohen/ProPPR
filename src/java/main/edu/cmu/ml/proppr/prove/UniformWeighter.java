package edu.cmu.ml.proppr.prove;

import java.util.Map;

import edu.cmu.ml.proppr.learn.tools.Linear;
import edu.cmu.ml.proppr.learn.tools.SquashingFunction;
import edu.cmu.ml.proppr.prove.wam.Feature;
import edu.cmu.ml.proppr.prove.wam.Goal;

/**
 * Weight all features uniformly as 1.0
 * @author krivard
 *
 */
public class UniformWeighter extends FeatureDictWeighter {
	public UniformWeighter(SquashingFunction f) {
		super(f);
	}
	public UniformWeighter() {
		this(new Linear());
	}

	@Override
	public double w(Map<Feature, Double> featureDict) {
		return this.squashingFunction.edgeWeight(this.weights,featureDict);
	}

}
