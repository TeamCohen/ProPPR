package edu.cmu.ml.proppr.learn;

import java.io.File;
import java.util.Map;
import java.util.Set;

import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.graph.LearningGraph;
import edu.cmu.ml.proppr.learn.SRW.SgdExample;
import edu.cmu.ml.proppr.learn.tools.WeightingScheme;
import edu.cmu.ml.proppr.learn.tools.LossData.LOSS;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.MuParamVector;
import edu.cmu.ml.proppr.util.ParamVector;
import edu.cmu.ml.proppr.util.SRWOptions;
import gnu.trove.map.TObjectDoubleMap;

public class LocalL2SRW extends L2SRW {
	public LocalL2SRW(SRWOptions params) {
		super(params);
	}
	public LocalL2SRW() { super(); }

	@Override
	public Set<String> localFeatures(ParamVector<String,?> paramVec, LearningGraph graph) {
		return graph.getFeatureSet();
	}
	
	@Override
	protected void sgd(ParamVector params, SgdExample ex) {
		((MuParamVector)params).count();
		((MuParamVector)params).setLast(localFeatures(params,ex.g));
		super.sgd(params, ex);
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
	public void initializeFeatures(ParamVector params, LearningGraph graph) {
		super.initializeFeatures(params, graph);
		for (String f : localFeatures(params,graph)) {
			prepareFeature(params,f);
		}
	}
	
	private void prepareFeature(ParamVector<String,?> paramVec, String f) {
		if (!trainable(f)) return;
		int gap = ((MuParamVector)paramVec).getLast(f);
		if (gap==0) return;
		double value = Dictionary.safeGet(paramVec,f);
		double powerTerm = Math.pow(1 - 2 * c.mu * this.learningRate(), gap);
		double weightDecay = value * (powerTerm - 1);
		//FIXME: opportunity for out-of-date `value`; probably out to convert to a try loop
		paramVec.adjustValue(f, weightDecay);
		this.cumloss.add(LOSS.REGULARIZATION, gap * c.mu * Math.pow(value, 2));
	}
}
