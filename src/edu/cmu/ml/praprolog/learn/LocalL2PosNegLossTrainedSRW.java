package edu.cmu.ml.praprolog.learn;

import java.util.Map;
import java.util.Set;

import edu.cmu.ml.praprolog.util.Dictionary;
import edu.cmu.ml.praprolog.util.MuParamVector;
import edu.cmu.ml.praprolog.util.ParamVector;

public class LocalL2PosNegLossTrainedSRW<T> extends L2PosNegLossTrainedSRW<T> {
	public LocalL2PosNegLossTrainedSRW(int maxT, double mu, double eta, WeightingScheme wScheme, double delta) {
		super(maxT,mu,eta,wScheme,delta);
	}
	public LocalL2PosNegLossTrainedSRW() { super(); }

	@Override
	public Set<String> localFeatures(ParamVector paramVec, PosNegRWExample<T> example) {
		return example.graph.getFeatureSet();
	}
	
	@Override
	protected Double derivRegularization(String f, ParamVector paramVec) {
		if (untrainedFeatures.contains(f)) return 0.0;
		double once = super.derivRegularization(f, paramVec);
		return ((MuParamVector)paramVec).getLast(f) * once;
	}
	
	@Override
	public Map<String, Double> gradient(ParamVector paramVec, PosNegRWExample<T> example) {
		Map<String,Double> ret = super.gradient(paramVec, example);
		((MuParamVector)paramVec).count();
		((MuParamVector)paramVec).setLast(localFeatures(paramVec,example));
		return ret;
	}
	
	@Override
	public ParamVector setupParams(ParamVector paramVec) { return new MuParamVector(paramVec); }
	
	@Override
	public void cleanupParams(ParamVector paramVec) { 
		Set<Map.Entry<String, Double>> parameters = paramVec.entrySet(); 
		for (Map.Entry<String, Double> e : parameters) {
			// finish catching up the regularization:
			// Bj = Bj - lambda * m * (2 mu Bj)
			double once = super.derivRegularization(e.getKey(), paramVec);
			e.setValue(e.getValue() - (Math.pow(this.epoch,-2) * this.eta / 2) * ((MuParamVector)paramVec).getLast(e.getKey()) * once);
		}
		((MuParamVector)paramVec).setLast(paramVec.keySet());
		
	}
}
