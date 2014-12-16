package edu.cmu.ml.proppr.learn;

import java.io.File;
import java.util.Map;
import java.util.Set;

import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.learn.tools.WeightingScheme;
import edu.cmu.ml.proppr.learn.tools.LossData.LOSS;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.MuParamVector;
import edu.cmu.ml.proppr.util.ParamVector;
import edu.cmu.ml.proppr.util.SRWOptions;
import gnu.trove.map.TObjectDoubleMap;

public class LocalL2PosNegLossTrainedSRW extends L2PosNegLossTrainedSRW {
	public LocalL2PosNegLossTrainedSRW(SRWOptions params) {
		super(params);
	}
	public LocalL2PosNegLossTrainedSRW() { super(); }

	@Override
	public Set<String> localFeatures(ParamVector<String,?> paramVec, PosNegRWExample example) {
		return example.getGraph().getFeatureSet();
	}
	
	@Override
	protected double derivRegularization(String f, ParamVector<String,?> paramVec) {
		// NB superclass records regularization loss for this clock cycle
		Double ret = super.derivRegularization(f, paramVec);
		return ret;
	}
	
	@Override
	public TObjectDoubleMap<String> gradient(ParamVector<String,?> paramVec, PosNegRWExample example) {
		TObjectDoubleMap<String> ret = super.gradient(paramVec, example);
		((MuParamVector)paramVec).count();
		((MuParamVector)paramVec).setLast(localFeatures(paramVec,example));
		return ret;
	}
	
	@Override
	public ParamVector<String,?> setupParams(ParamVector<String,?> paramVec) { return new MuParamVector<String>(paramVec); }
	
	@Override
	public void cleanupParams(ParamVector<String,?> paramVec) { 
		for(String f : (Set<String>) paramVec.keySet()) {
			// finish catching up the regularization:
			// Bj = Bj - lambda * (Rj)
			prepareFeature(paramVec,f);
		}
		((MuParamVector)paramVec).setLast(paramVec.keySet());
	}
	
	@Override
	public void prepareGradient(ParamVector<String,?> paramVec, PosNegRWExample example) {
		for (String f : localFeatures(paramVec,example)) {
			prepareFeature(paramVec,f);
		}
	}
	
	private void prepareFeature(ParamVector<String,?> paramVec, String f) {
		if (!trainable(f)) return;
		// use last-1 here because superclass will apply regularization for this clock cycle
		// during the gradient() call
		int gap = ((MuParamVector)paramVec).getLast(f);
		if (gap==0) return;
		double value = Dictionary.safeGet(paramVec,f);
		double powerTerm = Math.pow(1 - 2 * c.mu * this.learningRate(), gap);
		double weightDecay = value * (powerTerm - 1);
	    Dictionary.increment(paramVec, f, weightDecay);
		this.cumloss.add(LOSS.REGULARIZATION, gap * c.mu * Math.pow(value, 2));
	}
}
