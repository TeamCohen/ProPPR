package edu.cmu.ml.praprolog.trove.learn;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.ml.praprolog.trove.learn.tools.PosNegRWExample;
import edu.cmu.ml.praprolog.learn.tools.SRWParameters;
import edu.cmu.ml.praprolog.learn.tools.WeightingScheme;
import edu.cmu.ml.praprolog.learn.tools.LossData.LOSS;
import edu.cmu.ml.praprolog.util.Dictionary;
import edu.cmu.ml.praprolog.util.MuParamVector;
import edu.cmu.ml.praprolog.util.ParamVector;
import gnu.trove.map.hash.TObjectDoubleHashMap;

public class LocalL2PosNegLossTrainedSRW extends L2PosNegLossTrainedSRW {
	public LocalL2PosNegLossTrainedSRW(SRWParameters params) {
		super(params);
	}
	public LocalL2PosNegLossTrainedSRW() { super(); }

	@Override
	public Set<String> localFeatures(ParamVector paramVec, PosNegRWExample example) {
		return example.getGraph().getFeatureSet();
	}	
	@Override
	protected Double derivRegularization(String f, ParamVector paramVec) {
		// NB superclass records regularization loss for this clock cycle
		Double ret = super.derivRegularization(f, paramVec);
		return ret;
	}
	
	@Override
	public TObjectDoubleHashMap<String> gradient(ParamVector paramVec, PosNegRWExample example) {
		TObjectDoubleHashMap<String> ret = super.gradient(paramVec, example);
		((MuParamVector)paramVec).count();
		((MuParamVector)paramVec).setLast(localFeatures(paramVec,example));
		return ret;
	}
	
	@Override
	public ParamVector setupParams(ParamVector paramVec) { return new MuParamVector(paramVec); }
	
	@Override
	public void cleanupParams(ParamVector paramVec) { 
		for(String f : (Set<String>) paramVec.keySet()) {
			// finish catching up the regularization:
			// Bj = Bj - lambda * (Rj)
			prepareFeature(paramVec,f);
		}
		((MuParamVector)paramVec).setLast(paramVec.keySet());
	}
	
	@Override
	public void prepareGradient(ParamVector paramVec, PosNegRWExample example) {
		for (String f : localFeatures(paramVec,example)) {
			prepareFeature(paramVec,f);
		}
	}
	
	private void prepareFeature(ParamVector paramVec, String f) {
		if (!trainable(f)) return;
		// use last-1 here because superclass will apply regularization for this clock cycle
		// during the gradient() call
		int gap = ((MuParamVector)paramVec).getLast(f);
		if (gap==0) return;
		double value = Dictionary.safeGet(paramVec,f);		                            

              //L2
		double powerTerm = Math.pow(1 - 2 * c.mu * this.learningRate(), gap);
		double weightDecay = value * (powerTerm - 1);
		Dictionary.increment(paramVec, f, weightDecay);
		this.cumloss.add(LOSS.REGULARIZATION, gap * c.mu * Math.pow(value, 2));              
	}
}
