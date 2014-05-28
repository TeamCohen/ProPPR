package edu.cmu.ml.praprolog.trove.learn;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import edu.cmu.ml.praprolog.learn.WeightingScheme;
import edu.cmu.ml.praprolog.util.Dictionary;
import edu.cmu.ml.praprolog.util.ParamVector;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;

public class L2PosNegLossTrainedSRW extends SRW<PosNegRWExample> {
	private static final Logger log = Logger.getLogger(L2PosNegLossTrainedSRW.class);
	private static final double bound = 1.0e-15; //Prevent infinite log loss.

	public L2PosNegLossTrainedSRW(int maxT, double mu, double eta, WeightingScheme wScheme, double delta) {
		super(maxT,mu,eta,wScheme,delta);
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
	public TObjectDoubleHashMap<String> gradient(ParamVector paramVec, PosNegRWExample example) {
		
		TIntDoubleMap p = rwrUsingFeatures(example.getGraph(), example.getQueryVec(), paramVec);
		TIntObjectMap<TObjectDoubleHashMap<String>> d = derivRWRbyParams(example.getGraph(), example.getQueryVec(), paramVec);
		
		//introduce the regularizer
		TObjectDoubleHashMap<String> derivFparamVec = new TObjectDoubleHashMap<String>();
		for (String f : localFeatures(paramVec,example)) {
			derivFparamVec.put(f,derivRegularization(f,paramVec));
		}
		
		Set<String> trainableFeatures = trainableFeatures(localFeatures(paramVec,example)); 
		
		//compute gradient
		double pmax = 0;

		for (int x : example.getPosList()) {
			TObjectDoubleHashMap<String> dx = d.get(x);
			double px = p.get(x);
			if(px > pmax) pmax = px;
			for (String f : trainableFeatures) {
				if (Dictionary.safeContains(d,x,f)) {
					Dictionary.increment(derivFparamVec, f, -dx.get(f)/px);
				}
			}
		}

		//negative instance booster
    	double h = pmax + delta;
		double beta = 1;
		if(delta < 0.5) beta = (Math.log(1/h))/(Math.log(1/(1-h)));
		
		for (int x : example.getNegList()) {
			TObjectDoubleHashMap<String> dx = d.get(x);
			double px = p.get(x);
			for (String f : trainableFeatures) {
				if (Dictionary.safeContains(d,x,f)) 
					Dictionary.increment(derivFparamVec, f, beta*dx.get(f)/(1-px));
			}
		}
		int length = example.length();
		for (String f : trainableFeatures) {
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
	protected Double derivRegularization(String f, ParamVector paramVec) {
		// added 2* (kmm)
		return untrainedFeatures.contains(f) ? 0.0 : 2*mu*Dictionary.safeGet(paramVec, f);
	}

	public double empiricalLoss(ParamVector paramVec,
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
