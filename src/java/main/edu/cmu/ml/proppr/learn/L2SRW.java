package edu.cmu.ml.proppr.learn;


import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.learn.SRW.SgdExample;
import edu.cmu.ml.proppr.learn.tools.LossData.LOSS;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.ParamVector;
import edu.cmu.ml.proppr.util.SRWOptions;
import gnu.trove.map.TIntDoubleMap;

public class L2SRW extends SRW {
	private static final Logger log = Logger.getLogger(L2SRW.class);

	public L2SRW(SRWOptions params) {
		super(params);
	}

	public L2SRW() {
		super();
	}

	/**
	 * Loss is mu * theta_f^2
	 * d/df Loss is then 2 * mu * theta_f
	 * @param f
	 * @param paramVec
	 * @return
	 */
	@Override	
	protected void regularization(ParamVector params, SgdExample ex, TIntDoubleMap gradient) {
		for (String f : localFeatures(params, ex.g)) {
			double value = Dictionary.safeGet(params, f);
			double ret = untrainedFeatures.contains(f) ? 0.0 : 2*c.mu*value;
			this.cumloss.add(LOSS.REGULARIZATION, c.mu * Math.pow(value,2));
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
