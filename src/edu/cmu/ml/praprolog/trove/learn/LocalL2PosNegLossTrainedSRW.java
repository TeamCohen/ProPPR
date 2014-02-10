package edu.cmu.ml.praprolog.trove.learn;

import java.util.Map;
import java.util.Set;

public class LocalL2PosNegLossTrainedSRW extends L2PosNegLossTrainedSRW {
	public LocalL2PosNegLossTrainedSRW(int maxT, double mu, double eta, int wScheme) {
		super(maxT,mu,eta,wScheme);
	}

	@Override
	public Set<String> localFeatures(Map<String,Double> paramVec, PosNegRWExample example) {
		return example.getGraph().getFeatureSet();
	}
}
