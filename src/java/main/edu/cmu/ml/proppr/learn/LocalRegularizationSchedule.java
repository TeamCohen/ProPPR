package edu.cmu.ml.proppr.learn;

import java.util.Set;

import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.graph.LearningGraph;
import edu.cmu.ml.proppr.util.math.MuParamVector;
import edu.cmu.ml.proppr.util.math.ParamVector;

public class LocalRegularizationSchedule extends RegularizationSchedule {
	public LocalRegularizationSchedule(SRW srw, Regularize r) {
		super(srw, r);
	}

	public ParamVector<String,?> setupParams(ParamVector<String,?> params) { return new MuParamVector(params); }
	public Set<String> localFeatures(ParamVector paramVec, LearningGraph graph) { return graph.getFeatureSet(); }
	public void prepareForExample(ParamVector params, LearningGraph graph, ParamVector apply) {
		for (String f : localFeatures(params, graph)) {
			this.reg.lazyUpdate(parent.c, params, apply, f, parent.cumulativeLoss(), parent.learningRate());
		}
	}
	public void prepareForSgd(ParamVector params, PosNegRWExample ex) {
		((MuParamVector)params).count();
		((MuParamVector)params).setLast(localFeatures(params,ex.getGraph()));
	}
	public void cleanupParams(ParamVector<String,?> params, ParamVector apply) {
		for(String f : (Set<String>) params.keySet()) {
			// finish catching up the regularization:
			// Bj = Bj - lambda * (Rj)
			this.reg.lazyUpdate(parent.c, params, apply, f, parent.cumulativeLoss(), parent.learningRate());
		}
		((MuParamVector)params).setLast(params.keySet());
	}
	@Override
	public RegularizationSchedule copy(SRW srw) {
		return new LocalRegularizationSchedule(srw,this.reg);
	}
}
