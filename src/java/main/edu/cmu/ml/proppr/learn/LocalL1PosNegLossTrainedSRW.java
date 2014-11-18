package edu.cmu.ml.proppr.learn;

import java.io.File;
import java.util.Map;
import java.util.Set;

import edu.cmu.ml.proppr.learn.tools.LossData.LOSS;
import edu.cmu.ml.proppr.learn.tools.SRWParameters;
import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.learn.tools.WeightingScheme;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.MuParamVector;
import edu.cmu.ml.proppr.util.ParamVector;
import gnu.trove.map.TObjectDoubleMap;

public class LocalL1PosNegLossTrainedSRW extends L1PosNegLossTrainedSRW {
	public LocalL1PosNegLossTrainedSRW(SRWParameters params) {
		super(params);
	}
	public LocalL1PosNegLossTrainedSRW() { super(); }

	@Override
	public Set<String> localFeatures(ParamVector paramVec, PosNegRWExample example) {
		return example.getGraph().getFeatureSet();
	}
	
	@Override
	protected double derivRegularization(String f, ParamVector paramVec) {
		// NB superclass records regularization loss for this clock cycle
		Double ret = super.derivRegularization(f, paramVec);
		return ret;
	}
	
	@Override
	public TObjectDoubleMap<String> gradient(ParamVector paramVec, PosNegRWExample example) {
		TObjectDoubleMap<String> ret = super.gradient(paramVec, example);
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
		// use last-1 here because superclass will apply regularization for this clock cycle
		// during the gradient() call
		int gap = ((MuParamVector)paramVec).getLast(f);
		if (gap==0) return;
		double value = Dictionary.safeGet(paramVec,f);

		//L1 with a proximal operator
              //
		//signum(w) * max(0.0, abs(w) - shrinkageVal)
              
              double shrinkageVal = gap * this.learningRate() * c.mu;
              double weightDecay;
              if((c.mu != 0) && (!Double.isInfinite(shrinkageVal))){
 		    weightDecay = Math.signum(value) * Math.max(0.0, Math.abs(value) - shrinkageVal);
		    Dictionary.reset(paramVec, f, weightDecay);
              }

		this.cumloss.add(LOSS.REGULARIZATION, gap * c.mu);

	}
}
