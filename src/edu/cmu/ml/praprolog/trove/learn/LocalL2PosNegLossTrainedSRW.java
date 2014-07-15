package edu.cmu.ml.praprolog.trove.learn;

import java.util.Map;
import java.util.Set;

import edu.cmu.ml.praprolog.trove.learn.tools.PosNegRWExample;
import edu.cmu.ml.praprolog.learn.tools.WeightingScheme;
import edu.cmu.ml.praprolog.learn.tools.LossData.LOSS;
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
		Set<Map.Entry<String, Double>> parameters = paramVec.entrySet(); 
		for (Map.Entry<String, Double> e : parameters) {
			// finish catching up the regularization:
			// Bj = Bj - lambda * (Rj)
			double once = this.derivRegularization(e.getKey(), paramVec);
			e.setValue(e.getValue() - this.learningRate() * once);
		}
		((MuParamVector)paramVec).setLast(paramVec.keySet());
	}
	
	@Override
	public void prepareGradient(ParamVector paramVec, PosNegRWExample example) {
		for (String f : localFeatures(paramVec,example)) {
			int gap = ((MuParamVector)paramVec).getLast(f);
			double value = Dictionary.safeGet(paramVec,f);
			// use gap-1 here because superclass will apply regularization for this clock cycle
			// during the gradient() call
			double powerTerm = Math.pow(1 - 2 * this.mu * this.learningRate(), gap - 1);
			double weightDecay = value * (powerTerm - 1);
			Dictionary.increment(paramVec, f, weightDecay);
			this.cumloss.add(LOSS.REGULARIZATION, weightDecay);
		}
	}
}
