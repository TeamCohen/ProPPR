package edu.cmu.ml.praprolog.trove.learn;

import java.util.Map;
import java.util.Set;

import edu.cmu.ml.praprolog.learn.WeightingScheme;

public class LocalL2PosNegLossTrainedSRW extends L2PosNegLossTrainedSRW {
	public LocalL2PosNegLossTrainedSRW(int maxT, double mu, double eta, WeightingScheme wScheme, double delta) {
		super(maxT,mu,eta,wScheme,delta);
	}

	@Override
	public Set<String> localFeatures(Map<String,Double> paramVec, PosNegRWExample example) {
		return example.getGraph().getFeatureSet();
	}
}
