package edu.cmu.ml.proppr.learn;


import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.learn.tools.LossData.LOSS;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.ParamVector;
import edu.cmu.ml.proppr.util.SRWOptions;

public class L2PosNegLossTrainedSRW extends PosNegLossTrainedSRW {
	private static final Logger log = Logger.getLogger(L2PosNegLossTrainedSRW.class);

	public L2PosNegLossTrainedSRW(SRWOptions params) {
		super(params);
	}

	public L2PosNegLossTrainedSRW() {
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
	protected double derivRegularization(String f, ParamVector<String,?> paramVec) {
		double value = Dictionary.safeGet(paramVec, f);
		double ret = untrainedFeatures.contains(f) ? 0.0 : 2*c.mu*value;
		this.cumloss.add(LOSS.REGULARIZATION, c.mu * Math.pow(value,2));
		return ret;
	}

	@Override
	protected GradientComponents makeGradientComponents(
			ParamVector<String, ?> paramVec, PosNegRWExample example) {
		GradientComponents g = new GradientComponents();
		g.p = rwrUsingFeatures(example.getGraph(), example.getQueryVec(), paramVec);
		g.d = derivRWRbyParams(example.getGraph(), example.getQueryVec(), paramVec);
		return g;
	}
}
