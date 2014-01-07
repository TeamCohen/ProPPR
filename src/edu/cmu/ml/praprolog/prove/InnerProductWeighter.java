package edu.cmu.ml.praprolog.prove;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.skjegstad.utils.BloomFilter;

import edu.cmu.ml.praprolog.util.Dictionary;

/**
 * featureDictWeighter that weights each feature with a default
    value of 1.0, but allows a one to plug in a dictionary of
    non-default weights.
 * @author krivard
 *
 */
public class InnerProductWeighter extends FeatureDictWeighter {
	private static final Logger log = Logger.getLogger(InnerProductWeighter.class);
	protected static final BloomFilter<Goal> unknownFeatures = new BloomFilter<Goal>(.01,100);
	public InnerProductWeighter() {
		this(new HashMap<Goal,Double>());
	}
	public InnerProductWeighter(Map<Goal,Double> weights) {
		this.weights = weights;
	}
	@Override
	public double w(Map<Goal, Double> featureDict) {
		double result = 0;
		for (Map.Entry<Goal,Double> e : featureDict.entrySet()) {
			if (!this.weights.containsKey(e.getKey()) && !unknownFeatures.contains(e.getKey())) {
				log.warn("Using default weight 1.0 for unknown feature "+e.getKey()+" (this message only prints once)");
				this.unknownFeatures.add(e.getKey());
			}
			result += e.getValue() * Dictionary.safeGet(this.weights, e.getKey(), 1.0);
			if (log.isDebugEnabled()) log.debug("+="+e.getKey()+":"+e.getValue()+"*"+Dictionary.safeGet(this.weights, e.getKey(), 1.0)
					+"="+(e.getValue() * Dictionary.safeGet(this.weights, e.getKey(), 1.0))
					+" = "+result);
		}
		return result;
	}
	public static FeatureDictWeighter fromParamVec(Map<String, Double> paramVec) {
		//         goalDict = dict(( (rc.parser.parseGoal(s),w) for s,w in paramVec.items() ))
		Map<Goal,Double> weights = new HashMap<Goal,Double>();
		for (Map.Entry<String,Double> s : paramVec.entrySet()) {
			weights.put(Goal.parseGoal(s.getKey().replaceAll("[(),]"," ")), s.getValue());
		}
		return new InnerProductWeighter(weights);
	}

}
