package edu.cmu.ml.proppr.prove;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.skjegstad.utils.BloomFilter;

import edu.cmu.ml.proppr.learn.tools.Linear;
import edu.cmu.ml.proppr.learn.tools.SquashingFunction;
import edu.cmu.ml.proppr.prove.wam.Feature;
import edu.cmu.ml.proppr.prove.wam.Goal;
import edu.cmu.ml.proppr.prove.wam.Query;
import edu.cmu.ml.proppr.util.Dictionary;

/**
 * featureDictWeighter that weights each feature with a default
    value of 1.0, but allows a one to plug in a dictionary of
    non-default weights.
 * @author krivard
 *
 */
public class InnerProductWeighter extends FeatureDictWeighter {
	private static final int MAX_UNKNOWN_FEATURE_WARNINGS = 10;
	private int numUnknownFeatures = 0;
	private int numKnownFeatures = 0;
	private static final Logger log = Logger.getLogger(InnerProductWeighter.class);
	protected final BloomFilter<Feature> unknownFeatures;
	protected final BloomFilter<Feature> knownFeatures;
	private static SquashingFunction DEFAULT_SQUASHING_FUNCTION() {
		return new Linear();
	}
	public InnerProductWeighter() {
		this(new HashMap<Feature,Double>());
	}
	public InnerProductWeighter(Map<Feature,Double> weights) {
		this(DEFAULT_SQUASHING_FUNCTION(), weights);

	}
	public InnerProductWeighter(SquashingFunction f, Map<Feature,Double> weights) {
		super(f);
		this.weights = weights;
		this.unknownFeatures = new BloomFilter<Feature>(.01,Math.max(100, weights.size()));
		this.knownFeatures = new BloomFilter<Feature>(.01,Math.max(100, weights.size()));
	}
	@Override
	public double w(Map<Feature, Double> featureDict) {
		// track usage of known & unknown features
		for (Feature g : featureDict.keySet()) {
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
		return this.squashingFunction.edgeWeight(this.weights, featureDict);
	}
	public static FeatureDictWeighter fromParamVec(Map<String, Double> paramVec) {
		return fromParamVec(paramVec, DEFAULT_SQUASHING_FUNCTION());
	}
	public static InnerProductWeighter fromParamVec(Map<String, Double> paramVec, SquashingFunction f) {
		Map<Feature,Double> weights = new HashMap<Feature,Double>();
		for (Map.Entry<String,Double> s : paramVec.entrySet()) {
			weights.put(new Feature(s.getKey()), s.getValue());
		}
		return new InnerProductWeighter(f, weights);
	}
	public Map<Feature,Double> getWeights() {
		return this.weights;
	}
	public int seenUnknownFeatures() {
		return numUnknownFeatures;
	}
	public int seenKnownFeatures() {
		return numKnownFeatures;
	}
}
