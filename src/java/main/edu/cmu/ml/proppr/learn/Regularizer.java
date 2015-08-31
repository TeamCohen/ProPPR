package edu.cmu.ml.proppr.learn;

import java.util.Set;

import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.graph.LearningGraph;
import edu.cmu.ml.proppr.util.math.ParamVector;
import gnu.trove.map.TIntDoubleMap;

public abstract class Regularizer {
	SRW parent;
	
	public Regularizer(SRW srw) {
		this.parent = srw;
	}

	protected abstract void regularization(ParamVector params, PosNegRWExample ex, TIntDoubleMap gradient);

	public ParamVector<String,?> setupParams(ParamVector<String,?> params) { return params; }
	public Set<String> localFeatures(ParamVector paramVec, LearningGraph graph) { return paramVec.keySet(); }
	public void prepareForExample(ParamVector params, LearningGraph graph, ParamVector apply) {}
	public void prepareForSgd(ParamVector params, PosNegRWExample ex) {}
	public void cleanupParams(ParamVector<String,?> params, ParamVector apply) {}
}
