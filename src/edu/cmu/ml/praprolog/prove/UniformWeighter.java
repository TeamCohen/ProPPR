package edu.cmu.ml.praprolog.prove;

import java.util.Map;

/**
 * Weight all features uniformly as 1.0
 * @author krivard
 *
 */
public class UniformWeighter extends FeatureDictWeighter {

	@Override
	public double w(Map<Goal, Double> featureDict) {
		double result = 0;
		for (Double d : featureDict.values()) result += d;
		return result;
	}

}
