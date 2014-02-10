package edu.cmu.ml.praprolog.trove.learn;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import edu.cmu.ml.praprolog.util.Dictionary;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;

public class L2PosNegLossTrainedSRW extends SRW<PosNegRWExample> {
	private static final Logger log = Logger.getLogger(L2PosNegLossTrainedSRW.class);
	private static final double bound = 1.0e-15; //Prevent infinite log loss.

	public L2PosNegLossTrainedSRW(int maxT, double mu, double eta, int wScheme) {
		super(maxT,mu,eta,wScheme);
	}

	public L2PosNegLossTrainedSRW() {
		super();
	}

	/**
	 * Compute the local gradient of the parameters, associated
        with a particular start vector and pos/neg examples.
        
        loss function is F(w) = <regularization> + sum_{x in pos}[ -ln p[x]] + sum_{x in neg}[ -ln 1-p[x]]
        d/df F(w) = <regularization> + sum_{x in pos}[ - 1/p[x] * d/df p[x] ] + sum_{x in neg}[ - 1/(1-p[x]) * d/df p[x] ]
	 */
	public TObjectDoubleHashMap<String> gradient(Map<String, Double> paramVec, PosNegRWExample example) {
		
		TIntDoubleMap p = rwrUsingFeatures(example.getGraph(), example.getQueryVec(), paramVec);
		TIntObjectMap<TObjectDoubleHashMap<String>> d = derivRWRbyParams(example.getGraph(), example.getQueryVec(), paramVec);
		
		//introduce the regularizer
		TObjectDoubleHashMap<String> derivFparamVec = new TObjectDoubleHashMap<String>();
		for (String f : paramVec.keySet()) derivFparamVec.put(f,derivRegularization(f,paramVec));
		
		//compute gradient
		
		Set<String> trainables = trainableFeatures(paramVec); 
		for (int x : example.getPosList()) {
			TObjectDoubleHashMap<String> dx = d.get(x);
			double px = p.get(x);
			for (String f : trainables) {
				if (Dictionary.safeContains(d,x,f)) {
					Dictionary.increment(derivFparamVec, f, -dx.get(f)/px);
				}
			}
		}
		for (int x : example.getNegList()) {
			TObjectDoubleHashMap<String> dx = d.get(x);
			double px = p.get(x);
			for (String f : trainables) {
				if (Dictionary.safeContains(d,x,f)) 
					Dictionary.increment(derivFparamVec, f, dx.get(f)/(1-px));
			}
		}
		int length = example.length();
		for (String f : trainables) {
			derivFparamVec.put(f, Dictionary.safeGet(derivFparamVec, f) / length);
		}
		return derivFparamVec;
	}

	/**
	 * [Originally from L2RegularizedLearner; Java doesn't do multiple inheritance]
	 * @param f
	 * @param paramVec
	 * @return
	 */
	protected Double derivRegularization(String f, Map<String, Double> paramVec) {
		return untrainedFeatures.contains(f) ? 0.0 : Dictionary.safeGet(paramVec, f)*mu;
	}

	public double empiricalLoss(Map<String, Double> paramVec,
			PosNegRWExample example) {
		TIntDoubleMap p = rwrUsingFeatures(example.getGraph(), example.getQueryVec(), paramVec);
		double loss = 0;
		for (int x : example.getPosList()) 
		{
			double prob = Dictionary.safeGet(p,x);
			loss -= Math.log(checkProb(prob));
		}
		for (int x : example.getNegList()) 
			loss -= Math.log(1.0-Dictionary.safeGet(p,x));
		return loss;
	}

	public double checkProb(double prob)
	{
	     if(prob == 0)
           {
	      prob = bound;
	    }
	    return prob;
	}

	
}
