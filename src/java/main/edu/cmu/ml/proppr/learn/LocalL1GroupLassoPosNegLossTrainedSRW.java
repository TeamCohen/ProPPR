package edu.cmu.ml.proppr.learn;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.List;

import edu.cmu.ml.proppr.learn.tools.LossData.LOSS;
import edu.cmu.ml.proppr.learn.tools.SRWParameters;
import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.learn.tools.WeightingScheme;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.MuParamVector;
import edu.cmu.ml.proppr.util.ParamVector;
import gnu.trove.map.TObjectDoubleMap;

public class LocalL1GroupLassoPosNegLossTrainedSRW extends L1PosNegLossTrainedSRW {
	public LocalL1GroupLassoPosNegLossTrainedSRW(SRWParameters params) {
		super(params);
	}
	public LocalL1GroupLassoPosNegLossTrainedSRW() { super(); }


	@Override
	public Set<String> localFeatures(ParamVector<String,?> paramVec, PosNegRWExample example) {
		return example.getGraph().getFeatureSet();
	}

	@Override
	protected double derivRegularization(String f, ParamVector<String,?> paramVec) {
		// NB superclass records regularization loss for this clock cycle
		double ret = super.derivRegularization(f, paramVec);
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
	public void prepareGradient(ParamVector<String,?> paramVec, PosNegRWExample example) {
		for (String f : localFeatures(paramVec,example)) {
			prepareFeature(paramVec,f);
		}
	}

	private void prepareFeature(ParamVector<String,?> paramVec, String f) {
		// use last-1 here because superclass will apply regularization for this clock cycle
		// during the gradient() call
		int gap = ((MuParamVector)paramVec).getLast(f);
		if (gap==0) return;
		double value = Dictionary.safeGet(paramVec,f);

		double sumofsquares = 0;
		String target = "#" + f;
		if(c.diagonalDegree.containsKey(target)){
			double positive = c.diagonalDegree.get(target)*value;
			double negativeSum = 0;
			sumofsquares = value*value;
			List<String> sims = c.affinity.get(target);
			for (String s : sims) {
				double svalue = Dictionary.safeGet(paramVec,s);
				negativeSum -= svalue;
				sumofsquares = sumofsquares + svalue*svalue;
			}
		}

		//Group Lasso
		double weightDecay = 0;
		if(c.zeta != 0){
			double grouplasso = 0.5 * Math.pow(sumofsquares,-0.5);
			if(!Double.isInfinite(grouplasso)){
				//System.out.println("f: " + f +" group lasso:" + grouplasso);
				weightDecay = Math.signum(value) * Math.max(0.0, Math.abs(value) - (gap * this.learningRate() * c.zeta * grouplasso));
				Dictionary.reset(paramVec, f, weightDecay);
				this.cumloss.add(LOSS.REGULARIZATION, gap * this.learningRate() * c.zeta * grouplasso);             		
			}
		}     


		//L1 with a proximal operator              
		//signum(w) * max(0.0, abs(w) - shrinkageVal)

		double shrinkageVal = gap * this.learningRate() * c.mu;
		if((c.mu != 0) && (!Double.isInfinite(shrinkageVal))){
			weightDecay = Math.signum(value) * Math.max(0.0, Math.abs(value) - shrinkageVal);
			Dictionary.reset(paramVec, f, weightDecay);
		}
		this.cumloss.add(LOSS.REGULARIZATION, gap * c.mu);             		
	}
}
