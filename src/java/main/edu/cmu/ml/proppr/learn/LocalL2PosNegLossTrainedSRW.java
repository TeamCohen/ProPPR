package edu.cmu.ml.proppr.learn;

import java.util.Map;
import java.util.Set;

import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.learn.tools.WeightingScheme;
import edu.cmu.ml.proppr.learn.tools.LossData.LOSS;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.MuParamVector;
import edu.cmu.ml.proppr.util.ParamVector;
import gnu.trove.map.TObjectDoubleMap;

public class LocalL2PosNegLossTrainedSRW<F> extends L2PosNegLossTrainedSRW<F> {
	public LocalL2PosNegLossTrainedSRW(int maxT, double mu, double eta, WeightingScheme<F> wScheme, double delta) {
		super(maxT,mu,eta,wScheme,delta);
	}
	public LocalL2PosNegLossTrainedSRW() { super(); }

	@Override
	public Set<F> localFeatures(ParamVector<F,?> paramVec, PosNegRWExample<F> example) {
		return example.getGraph().getFeatureSet();
	}
	
	@Override
	protected Double derivRegularization(F f, ParamVector<F,?> paramVec) {
		// NB superclass records regularization loss for this clock cycle
		Double ret = super.derivRegularization(f, paramVec);
		return ret;
	}
	
	@Override
	public TObjectDoubleMap<F> gradient(ParamVector<F,?> paramVec, PosNegRWExample<F> example) {
		TObjectDoubleMap<F> ret = super.gradient(paramVec, example);
		((MuParamVector)paramVec).count();
		((MuParamVector)paramVec).setLast(localFeatures(paramVec,example));
		return ret;
	}
	
	@Override
	public ParamVector<F,?> setupParams(ParamVector<F,?> paramVec) { return new MuParamVector<F>(paramVec); }
	
	@Override
	public void cleanupParams(ParamVector<F,?> paramVec) { 
		for(F f : (Set<F>) paramVec.keySet()) {
			// finish catching up the regularization:
			// Bj = Bj - lambda * (Rj)
			prepareFeature(paramVec,f);
		}
		((MuParamVector)paramVec).setLast(paramVec.keySet());
	}
	
	@Override
	public void prepareGradient(ParamVector<F,?> paramVec, PosNegRWExample<F> example) {
		for (F f : localFeatures(paramVec,example)) {
			prepareFeature(paramVec,f);
		}
	}
	
	private void prepareFeature(ParamVector<F,?> paramVec, F f) {
		// use last-1 here because superclass will apply regularization for this clock cycle
		// during the gradient() call
		int gap = ((MuParamVector)paramVec).getLast(f);
		if (gap==0) return;
		double value = Dictionary.safeGet(paramVec,f);
		double powerTerm = Math.pow(1 - 2 * this.mu * this.learningRate(), gap);
		double weightDecay = value * (powerTerm - 1);
		Dictionary.increment(paramVec, f, weightDecay);
		this.cumloss.add(LOSS.REGULARIZATION, gap * this.mu * Math.pow(value, 2));
		
	}
}
