package edu.cmu.ml.proppr.learn;

import java.util.Set;

import edu.cmu.ml.proppr.learn.SRW.SgdExample;
import edu.cmu.ml.proppr.learn.tools.LossData.LOSS;
import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.graph.LearningGraph;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.MuParamVector;
import edu.cmu.ml.proppr.util.ParamVector;
import edu.cmu.ml.proppr.util.SRWOptions;
import gnu.trove.map.TObjectDoubleMap;

public class LocalL1SRW extends L1SRW {
	public LocalL1SRW(SRWOptions params) {
		super(params);
	}
	public LocalL1SRW() { super(); }

	@Override
	public Set<String> localFeatures(ParamVector paramVec, LearningGraph graph) {
		return graph.getFeatureSet();
	}

	@Override
	protected void sgd(ParamVector params, SgdExample ex) {
		((MuParamVector)params).count();
		((MuParamVector)params).setLast(localFeatures(params,ex.g));
		super.sgd(params, ex);
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
	public void initializeFeatures(ParamVector params, LearningGraph graph) {
		super.initializeFeatures(params, graph);
	}
	
	@Override
	public void prepareForExample(ParamVector params, LearningGraph graph) {
		for (String f : localFeatures(params, graph)) {
			prepareFeature(params,f);
		}
	}

	private void prepareFeature(ParamVector params, String f) {
		if (!trainable(f)) return;
		int gap = ((MuParamVector)params).getLast(f);
		if (gap==0) return;
		double value = Dictionary.safeGet(params,f);

		//L1 with a proximal operator
		//
		//signum(w) * max(0.0, abs(w) - shrinkageVal)

		double shrinkageVal = gap * this.learningRate() * c.mu;
		double weightDecay;
		if((c.mu != 0) && (!Double.isInfinite(shrinkageVal))){
			weightDecay = Math.signum(value) * Math.max(0.0, Math.abs(value) - shrinkageVal);
			Dictionary.set(params, f, weightDecay);
			//FIXME: why is this being set instead of incremented?
			//FIXME: opportunity for out-of-date `value`; probably out to convert to a try loop

			this.cumloss.add(LOSS.REGULARIZATION, gap * c.mu);
		}


	}
}
