package edu.cmu.ml.praprolog.learn;

import java.util.Map;
import java.util.Set;

public class LocalL2PosNegLossTrainedSRW<T> extends L2PosNegLossTrainedSRW<T> {
	public LocalL2PosNegLossTrainedSRW(int maxT, double mu, double eta, WeightingScheme wScheme, double delta) {
		super(maxT,mu,eta,wScheme,delta);
	}

	@Override
	public Set<String> localFeatures(Map<String,Double> paramVec, PosNegRWExample<T> example) {
		return example.graph.getFeatureSet();
	}
}
