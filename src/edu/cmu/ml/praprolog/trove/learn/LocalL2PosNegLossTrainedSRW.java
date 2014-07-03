package edu.cmu.ml.praprolog.trove.learn;

import java.util.Map;
import java.util.Set;

import edu.cmu.ml.praprolog.trove.learn.PosNegRWExample;
import edu.cmu.ml.praprolog.learn.WeightingScheme;
import edu.cmu.ml.praprolog.util.Dictionary;
import edu.cmu.ml.praprolog.util.MuParamVector;
import edu.cmu.ml.praprolog.util.ParamVector;
import gnu.trove.map.hash.TObjectDoubleHashMap;

public class LocalL2PosNegLossTrainedSRW extends L2PosNegLossTrainedSRW {
	public LocalL2PosNegLossTrainedSRW(int maxT, double mu, double eta, WeightingScheme wScheme, double delta) {
		super(maxT,mu,eta,wScheme,delta);
	}
	public LocalL2PosNegLossTrainedSRW() { super(); }

	@Override
	public Set<String> localFeatures(ParamVector paramVec, PosNegRWExample example) {
		return example.getGraph().getFeatureSet();
	}	
	@Override
	protected Double derivRegularization(String f, ParamVector paramVec) {
		if (untrainedFeatures.contains(f)) return 0.0;
		double powerTerm = Math.pow(1 - 2 * this.mu * this.learningRate(), ((MuParamVector)paramVec).getLast(f));
		double factorTerm = (1 - powerTerm) / this.learningRate();
		return factorTerm * Dictionary.safeGet(paramVec,f);
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
		Set<Map.Entry<String, Double>> parameters = paramVec.entrySet(); 
		for (Map.Entry<String, Double> e : parameters) {
			// finish catching up the regularization:
			// Bj = Bj - lambda * (Rj)
			double once = this.derivRegularization(e.getKey(), paramVec);
			e.setValue(e.getValue() - this.learningRate() * once);
		}
		((MuParamVector)paramVec).setLast(paramVec.keySet());
		
	}
}
