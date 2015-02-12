package edu.cmu.ml.proppr.learn;

import edu.cmu.ml.proppr.learn.SRW.SgdExample;
import edu.cmu.ml.proppr.learn.tools.LossData;
import edu.cmu.ml.proppr.learn.tools.LossData.LOSS;
import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.ParamVector;
import edu.cmu.ml.proppr.util.SRWOptions;
import gnu.trove.map.TIntDoubleMap;

public class L1SRW extends SRW {
	protected LossData cumloss;

	public L1SRW(SRWOptions params) {
		super(params);
		this.cumloss = new LossData();
	}

	public L1SRW() {
		super();
		this.cumloss = new LossData();
	}

	/**
	 * though non-continuous, the d/df of L1 can be approximated by mu.
	 * the proximal operator implementation in localL1 is more stable.
	 * @param f
	 * @param paramVec
	 * @return
	 */
	@Override
	protected void regularization(ParamVector params, SgdExample ex, TIntDoubleMap gradient) {
		for (String f : localFeatures(params, ex.g)) {
//			double value = Dictionary.safeGet(params, f);
			double ret = untrainedFeatures.contains(f) ? 0.0 : c.mu;
			this.cumloss.add(LOSS.REGULARIZATION, c.mu);
			gradient.adjustOrPutValue(ex.getFeatureId(f), ret, ret);
		}
	}

//	@Override
//	protected GradientComponents makeGradientComponents(
//			ParamVector<String, ?> paramVec, PosNegRWExample example) {
//		GradientComponents g = new GradientComponents();
//		g.p = rwrUsingFeatures(example.getGraph(), example.getQueryVec(), paramVec);
//		g.d = derivRWRbyParams(example.getGraph(), example.getQueryVec(), paramVec);
//		return g;
//	}
}
