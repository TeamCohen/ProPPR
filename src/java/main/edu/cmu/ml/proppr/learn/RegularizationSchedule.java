package edu.cmu.ml.proppr.learn;

import java.util.Set;

import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.examples.RWExample;
import edu.cmu.ml.proppr.graph.LearningGraph;
import edu.cmu.ml.proppr.util.math.ParamVector;
import gnu.trove.map.TIntDoubleMap;

public class RegularizationSchedule {
	protected Regularize reg;
	protected SRW parent;
	
	public RegularizationSchedule(SRW srw, Regularize r) {
		this.parent = srw;
		this.reg = r;
	}

	public void regularization(ParamVector params, RWExample ex, TIntDoubleMap gradient) {
		for (String f : localFeatures(params, ex.getGraph())) {
			if (!parent.trainable(f)) continue;
			reg.synchronousUpdate(parent.c, params, f, gradient, parent.cumulativeLoss(), ex.getGraph().featureLibrary);
		}
	}
	public ParamVector<String,?> setupParams(ParamVector<String,?> params) { return params; }
	public Set<String> localFeatures(ParamVector paramVec, LearningGraph graph) { return paramVec.keySet(); }
	public void prepareForExample(ParamVector params, LearningGraph graph, ParamVector apply) {}
	public void prepareForSgd(ParamVector params, PosNegRWExample ex) {}
	public void cleanupParams(ParamVector<String,?> params, ParamVector apply) {}

	public RegularizationSchedule copy(SRW srw) {
		return new RegularizationSchedule(srw,this.reg);
	}
}
