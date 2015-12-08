package edu.cmu.ml.proppr.learn;

import java.util.Set;

import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.graph.LearningGraph;
import edu.cmu.ml.proppr.util.math.MuParamVector;
import edu.cmu.ml.proppr.util.math.ParamVector;

/**
 * Local AKA Lazy regularization.
 * 
 * This regularization schedule must be used with a MuParamVector.
 * 
 * @author krivard
 *
 */
public class LocalRegularizationSchedule extends RegularizationSchedule {
	public LocalRegularizationSchedule(SRW srw, Regularize r) {
		super(srw, r);
	}

	public ParamVector<String,?> setupParams(ParamVector<String,?> params) { return new MuParamVector<String>(params); }
	public Set<String> localFeatures(ParamVector<String,?> paramVec, LearningGraph graph) { return graph.getFeatureSet(); }
	public void prepareForExample(ParamVector<String,?> params, LearningGraph graph, ParamVector<String,?> apply) {
		if (!(params instanceof MuParamVector)) throw new IllegalArgumentException("LocalRegularizationSchedule requires a MuParamVector");
		for (String f : localFeatures(params, graph)) {
			if (!parent.trainable(f)) continue;
			this.reg.lazyUpdate(parent.c, (MuParamVector<String>) params, apply, f, parent._cumulativeLoss(), parent.learningRate(f));
		}
	}
	public void prepareForSgd(ParamVector<String,?> params, PosNegRWExample ex) {
		if (!(params instanceof MuParamVector)) throw new IllegalArgumentException("LocalRegularizationSchedule requires a MuParamVector");
		((MuParamVector)params).count();
		((MuParamVector)params).setLast(localFeatures(params,ex.getGraph()));
	}
	public void cleanupParams(ParamVector<String,?> params, ParamVector<String,?> apply) {
		if (!(params instanceof MuParamVector)) throw new IllegalArgumentException("LocalRegularizationSchedule requires a MuParamVector");
		for(String f : (Set<String>) params.keySet()) {
			// finish catching up the regularization:
			// Bj = Bj - lambda * (Rj)
			this.reg.lazyUpdate(parent.c, (MuParamVector<String>) params, apply, f, parent.cumulativeLoss(), parent.learningRate(f));
		}
		((MuParamVector)params).setLast(params.keySet());
	}
	@Override
	public RegularizationSchedule copy(SRW srw) {
		return new LocalRegularizationSchedule(srw,this.reg);
	}
}
