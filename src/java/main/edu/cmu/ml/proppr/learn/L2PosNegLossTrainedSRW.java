package edu.cmu.ml.proppr.learn;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.learn.tools.LossData;
import edu.cmu.ml.proppr.learn.tools.WeightingScheme;
import edu.cmu.ml.proppr.learn.tools.LossData.LOSS;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.ParamVector;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;

public class L2PosNegLossTrainedSRW<F> extends SRW<F,PosNegRWExample<F>> {
	private static final Logger log = Logger.getLogger(L2PosNegLossTrainedSRW.class);
	private static final double bound = 1.0e-15; //Prevent infinite log loss.
	private final TObjectDoubleMap<F> EMPTY = new TObjectDoubleHashMap<F>();
	protected LossData cumloss;

	public L2PosNegLossTrainedSRW(int maxT, double mu, double eta, WeightingScheme<F> wScheme, double delta) {
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
	public TObjectDoubleMap<F> gradient(ParamVector<F,?> paramVec, PosNegRWExample<F> example) {
		
		// compute regularization
		TObjectDoubleMap<F> derivFparamVec = new TObjectDoubleHashMap<F>();
		for (F f : localFeatures(paramVec,example)) {
			derivFparamVec.put(f,derivRegularization(f,paramVec));
		}
		
		// compute p
		TIntDoubleMap p = rwrUsingFeatures(example.getGraph(), example.getQueryVec(), paramVec);
		TIntObjectMap<TObjectDoubleMap<F>> d = derivRWRbyParams(example.getGraph(), example.getQueryVec(), paramVec);

		Set<F> trainableFeatures = trainableFeatures(localFeatures(paramVec,example));

		//compute gradient
		double pmax = 0;

		for (int x : example.getPosList()) {
			if (log.isDebugEnabled()) log.debug("pos example "+x);
			TObjectDoubleMap<F> dx = Dictionary.safeGet(d,x,EMPTY);//d.get(x);
			double px = clip(Dictionary.safeGet(p,x,weightingScheme.defaultWeight()));//p.get(x));
			if(px > pmax) pmax = px;
			for (F f : trainableFeatures) {
				if (Dictionary.safeContains(d,x,f)) {
					if (log.isDebugEnabled()) log.debug(String.format(" - delta %s is - %f * %f", f,dx.get(f),1.0/px));
					Dictionary.increment(derivFparamVec, f, -dx.get(f)/px);
				}
			}
			this.cumloss.add(LOSS.LOG, -Math.log(clip(px)));
		}

		//negative instance booster
		double h = pmax + delta;
		double beta = 1;
		if(delta < 0.5) beta = (Math.log(1/h))/(Math.log(1/(1-h)));

		for (int x : example.getNegList()) {
			TObjectDoubleMap<F> dx = Dictionary.safeGet(d, x, EMPTY);//d.get(x);
			double px = Dictionary.safeGet(p,x,weightingScheme.defaultWeight());//p.get(x);
			for (F f : trainableFeatures) {
				if (Dictionary.safeContains(d,x,f)) 
					Dictionary.increment(derivFparamVec, f, beta*dx.get(f)/clip(1-px));
			}
			this.cumloss.add(LOSS.LOG, -Math.log(clip(1.0-px)));
		}
		return derivFparamVec;
	}

	/**
	 * Loss is mu * theta_f^2
	 * d/df Loss is then 2 * mu * theta_f
	 * @param f
	 * @param paramVec
	 * @return
	 */
	protected Double derivRegularization(F f, ParamVector<F,?> paramVec) {
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

	public double clip(double prob)
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
