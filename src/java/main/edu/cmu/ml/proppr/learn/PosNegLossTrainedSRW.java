package edu.cmu.ml.proppr.learn;


import java.util.Set;

import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.learn.tools.LossData;
import edu.cmu.ml.proppr.learn.tools.LossData.LOSS;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.ParamVector;
import edu.cmu.ml.proppr.util.SRWOptions;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;

public abstract class PosNegLossTrainedSRW extends SRW<PosNegRWExample> {
	private static final Logger log = Logger.getLogger(PosNegLossTrainedSRW.class);
	private static final double bound = 1.0e-15; //Prevent infinite log loss.
	protected LossData cumloss;
	
	public PosNegLossTrainedSRW(SRWOptions params) {
		super(params);
		this.cumloss = new LossData();
	}

	public PosNegLossTrainedSRW() {
		super();
		this.cumloss = new LossData();
	}
	
	protected abstract double derivRegularization(String f, ParamVector<String,?> paramVec);
	protected abstract GradientComponents makeGradientComponents(ParamVector<String,?> paramVec, PosNegRWExample example);
	protected class GradientComponents {
		TIntDoubleMap p;
		TIntObjectMap<TObjectDoubleMap<String>> d;
	}
	
	/**
	 * Compute the local gradient of the parameters, associated
        with a particular start vector and pos/neg examples.

        loss function is F(w) = <regularization> + sum_{x in pos}[ -ln p[x]] + sum_{x in neg}[ -ln 1-p[x]]
        d/df F(w) = <regularization> + sum_{x in pos}[ - 1/p[x] * d/df p[x] ] + sum_{x in neg}[ + 1/(1-p[x]) * d/df p[x] ]
	 */
	public TObjectDoubleMap<String> gradient(ParamVector<String,?> paramVec, PosNegRWExample example) {

		Set<String> trainableFeatures = trainableFeatures(localFeatures(paramVec,example));
		
		// compute regularization
		TObjectDoubleMap<String> derivFparamVec = new TObjectDoubleHashMap<String>();
		for (String f : trainableFeatures) {
			derivFparamVec.put(f,derivRegularization(f,paramVec));
		}
		
		// compute p
		TIntDoubleMap p = rwrUsingFeatures(example.getGraph(), example.getQueryVec(), paramVec);
		TIntObjectMap<TObjectDoubleMap<String>> d = derivRWRbyParams(example.getGraph(), example.getQueryVec(), paramVec);


		//compute gradient
		double pmax = 0;

		for (int x : example.getPosList()) {
			if (log.isDebugEnabled()) log.debug("pos example "+x);
			TObjectDoubleMap<String> dx = Dictionary.safeGet(d,x,EMPTY);//d.get(x);
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

		for (int x : example.getNegList()) {
			TObjectDoubleMap<String> dx = Dictionary.safeGet(d, x, EMPTY);//d.get(x);
			double px = Dictionary.safeGet(p,x,c.weightingScheme.defaultWeight());//p.get(x);
			for (String f : trainableFeatures) {
				if (Dictionary.safeContains(d,x,f)) 
					Dictionary.increment(derivFparamVec, f, beta*dx.get(f)/clip(1-px));
			}
			this.cumloss.add(LOSS.LOG, -Math.log(clip(1.0-px)));
		}
		return derivFparamVec;
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
