package edu.cmu.ml.praprolog.learn;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import edu.cmu.ml.praprolog.util.Dictionary;

public class L2PosNegLossTrainedSRW<T> extends SRW<PosNegRWExample<T>> {
	private static final Logger log = Logger.getLogger(L2PosNegLossTrainedSRW.class);
	private static final double bound = 1.0e-15; //Prevent infinit log loss.

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
        d/df F(w) = <regularization> + sum_{x in pos}[ - 1/p[x] * d/df p[x] ] + sum_{x in neg}[ + 1/(1-p[x]) * d/df p[x] ]
	 */
	public Map<String, Double> gradient(Map<String, Double> paramVec, PosNegRWExample<T> example) {
		
		Map<T,Double> p = rwrUsingFeatures(example.getGraph(), example.getQueryVec(), paramVec);
		Map<T,Map<String,Double>> d = derivRWRbyParams(example.getGraph(), example.getQueryVec(), paramVec);
		
		//introduce the regularizer
		Map<String,Double> derivFparamVec = new TreeMap<String,Double>();
		for (String f : localFeatures(paramVec,example)) {
			derivFparamVec.put(f,derivRegularization(f,paramVec));
		}
		
		Set<String> trainableFeatures = trainableFeatures(localFeatures(paramVec,example));
		
		//compute gradient
		double pmax = 0;

		for (T x : example.getPosList()) {
			if (log.isDebugEnabled()) log.debug("pos example "+x);
			for (String f : trainableFeatures) {
				if (Dictionary.safeContains(d,x,f)) {
					double px = p.get(x);
					if(px > pmax) pmax = px;
					if (log.isDebugEnabled()) log.debug(String.format(" - delta %s is - %f * %f", f,d.get(x).get(f),1.0/p.get(x)));
					Dictionary.increment(derivFparamVec, f, -d.get(x).get(f)/p.get(x));
				}
			}
		}

		//negative instance booster
		double h = pmax + delta;
		double beta = 1;
		if(delta < 0.5) beta = (Math.log(1/h))/(Math.log(1/(1-h)));

		for (T x : example.getNegList()) {
			Map<String,Double> dx = d.get(x);
			double px = p.get(x);
			for (String f : trainableFeatures) {
				if (Dictionary.safeContains(d,x,f)) 
					Dictionary.increment(derivFparamVec, f, beta*dx.get(f)/(1-px));
			}
		}
		for (String f : trainableFeatures) {
			double derivF = Dictionary.safeGet(derivFparamVec, f);
			derivFparamVec.put(f,  derivF / example.length());
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

	public double empiricalLoss(Map<String, Double> paramVec, PosNegRWExample<T> example) {
		Map<T,Double> p = rwrUsingFeatures(example.getGraph(), example.getQueryVec(), paramVec);
		double loss = 0;
		for (T x : example.getPosList()) 
		{
			double prob = Dictionary.safeGet(p,x);
			loss -= Math.log(checkProb(prob));
		}
		for (T x : example.getNegList()) 
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
