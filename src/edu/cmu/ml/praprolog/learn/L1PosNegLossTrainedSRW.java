package edu.cmu.ml.praprolog.learn;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import edu.cmu.ml.praprolog.learn.tools.LossData;
import edu.cmu.ml.praprolog.learn.tools.LossData.LOSS;
import edu.cmu.ml.praprolog.learn.tools.PosNegRWExample;
import edu.cmu.ml.praprolog.learn.tools.SRWParameters;
import edu.cmu.ml.praprolog.learn.tools.WeightingScheme;
import edu.cmu.ml.praprolog.util.Dictionary;
import edu.cmu.ml.praprolog.util.ParamVector;

public class L1PosNegLossTrainedSRW<T> extends SRW<PosNegRWExample<T>> {
	private static final Logger log = Logger.getLogger(L2PosNegLossTrainedSRW.class);
	private static final double bound = 1.0e-15; //Prevent infinite log loss.
	protected LossData cumloss;

	public L1PosNegLossTrainedSRW(SRWParameters params) {
		super(params);
		this.cumloss = new LossData();
	}

	public L1PosNegLossTrainedSRW() {
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
			Map<String,Double> dx = Dictionary.safeGet(d,x,Collections.EMPTY_MAP);//d.get(x);
			double px = clip(Dictionary.safeGet(p,x,c.weightingScheme.defaultWeight()));//p.get(x));
			if(px > pmax) pmax = px;
			for (String f : trainableFeatures) {
				if (Dictionary.safeContains(d,x,f)) {
					if (log.isDebugEnabled()) log.debug(String.format(" - delta %s is - %f * %f", f,dx.get(f),1.0/px));
					Dictionary.increment(derivFparamVec, f, -dx.get(f)/px);
				}
			}
			this.cumloss.add(LOSS.LOG, -Math.log(clip(px)));
		}

		//negative instance booster
		double h = pmax + c.delta;
		double beta = 1;
		if(c.delta < 0.5) beta = (Math.log(1/h))/(Math.log(1/(1-h)));

		for (T x : example.getNegList()) {
			Map<String,Double> dx = Dictionary.safeGet(d, x, Collections.EMPTY_MAP);//d.get(x);
			double px = Dictionary.safeGet(p,x,c.weightingScheme.defaultWeight());//p.get(x);
			for (String f : trainableFeatures) {
				if (Dictionary.safeContains(d,x,f)) 
					Dictionary.increment(derivFparamVec, f, beta*dx.get(f)/clip(1-px));
			}
			this.cumloss.add(LOSS.LOG, -Math.log(clip(1.0-px)));
		}
		return derivFparamVec;
	}

	/**
	 * though non-continuous, the d/df of L1 can be approximated by mu.
        * the proximal operator implementation in localL1 is more stable.
	 * @param f
	 * @param paramVec
	 * @return
	 */
	protected Double derivRegularization(String f, ParamVector paramVec) {
		double value = Dictionary.safeGet(paramVec, f);
		double ret = untrainedFeatures.contains(f) ? 0.0 : c.mu;
              this.cumloss.add(LOSS.REGULARIZATION, c.mu);

		return ret;
	}

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
