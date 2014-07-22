package edu.cmu.ml.praprolog.learn;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import edu.cmu.ml.praprolog.learn.tools.LossData;
import edu.cmu.ml.praprolog.learn.tools.LossData.LOSS;
import edu.cmu.ml.praprolog.learn.tools.PosNegRWExample;
import edu.cmu.ml.praprolog.learn.tools.WeightingScheme;
import edu.cmu.ml.praprolog.util.Dictionary;
import edu.cmu.ml.praprolog.util.ParamVector;

public class L2PosNegLossTrainedSRW<T> extends SRW<PosNegRWExample<T>> {
	private static final Logger log = Logger.getLogger(L2PosNegLossTrainedSRW.class);
	private static final double bound = 1.0e-15; //Prevent infinite log loss.
	protected LossData cumloss;

	public L2PosNegLossTrainedSRW(int maxT, double mu, double eta, WeightingScheme wScheme, double delta) {
		super(maxT,mu,eta,wScheme,delta);
		this.cumloss = new LossData();
	}

	public L2PosNegLossTrainedSRW() {
		super();
		this.cumloss = new LossData();
	}

	/**
	 * Compute the local gradient of the parameters, associated
        with a particular start vector and pos/neg examples.

        loss function is F(w) = <regularization> + sum_{x in pos}[ -ln p[x]] + sum_{x in neg}[ -ln 1-p[x]]
        d/df F(w) = <regularization> + sum_{x in pos}[ - 1/p[x] * d/df p[x] ] + sum_{x in neg}[ + 1/(1-p[x]) * d/df p[x] ]
	 */
	public Map<String, Double> gradient(ParamVector paramVec, PosNegRWExample<T> example) {
		
		// compute regularization
		Map<String,Double> derivFparamVec = new TreeMap<String,Double>();
		for (String f : localFeatures(paramVec,example)) {
			derivFparamVec.put(f,derivRegularization(f,paramVec));
		}
		
		// compute p
		Map<T,Double> p = rwrUsingFeatures(example.getGraph(), example.getQueryVec(), paramVec);
		Map<T,Map<String,Double>> d = derivRWRbyParams(example.getGraph(), example.getQueryVec(), paramVec);

		Set<String> trainableFeatures = trainableFeatures(localFeatures(paramVec,example));

		//compute gradient
		double pmax = 0;

		for (T x : example.getPosList()) {
			if (log.isDebugEnabled()) log.debug("pos example "+x);
			Map<String,Double> dx = d.get(x);
			double px = p.get(x);
			if(px > pmax) pmax = px;
			for (String f : trainableFeatures) {
				if (Dictionary.safeContains(d,x,f)) {
					if (log.isDebugEnabled()) log.debug(String.format(" - delta %s is - %f * %f", f,dx.get(f),1.0/px));
					Dictionary.increment(derivFparamVec, f, -dx.get(f)/px);
				}
			}
			this.cumloss.add(LOSS.LOG, -Math.log(checkProb(px)));
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
			this.cumloss.add(LOSS.LOG, -Math.log(checkProb(1.0-px)));
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
		double value = Dictionary.safeGet(paramVec, f);
		double ret = untrainedFeatures.contains(f) ? 0.0 : 2*mu*value;
		this.cumloss.add(LOSS.REGULARIZATION, this.mu * Math.pow(value,2));
		return ret;
	}

//	public double empiricalLoss(ParamVector paramVec, PosNegRWExample<T> example) {
//		Map<T,Double> p = rwrUsingFeatures(example.getGraph(), example.getQueryVec(), paramVec);
//		double loss = 0;
//		for (T x : example.getPosList()) 
//		{
//			double prob = Dictionary.safeGet(p,x);
//			loss -= Math.log(checkProb(prob));
//		}
//		for (T x : example.getNegList()) 
//			loss -= Math.log(1.0-Dictionary.safeGet(p,x));
//		return loss;
//	}

	public double checkProb(double prob)
	{
		if(prob <= 0)
		{
			prob = bound;
		}
		return prob;
	}	

	@Override
	public LossData cumulativeLoss() {
		return cumloss.copy();
	}
	@Override
	public void clearLoss() {
		cumloss.clear(); // ?
	}
}
