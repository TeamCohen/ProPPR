package edu.cmu.ml.proppr.learn;


import edu.cmu.ml.proppr.learn.tools.LossData;
import edu.cmu.ml.proppr.learn.tools.LossData.LOSS;
import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.SRWOptions;
import edu.cmu.ml.proppr.util.math.ParamVector;
import gnu.trove.map.TIntDoubleMap;

public class L1Regularizer extends Regularizer {

	public L1Regularizer(SRW srw) {
		super(srw);
	}

	/**
	 * L1 loss is mu * abs( theta_f )
	 * d/df L1 loss is then sign(theta_f) * max( abs(theta_f), mu ), where theta_f != 0
	 * 
	 * though non-continuous, the d/df of L1 can be approximated by mu.
	 * the proximal operator implementation in localL1 is more stable.
	 * @param f
	 * @param paramVec
	 * @return
	 */
	@Override
	protected void regularization(ParamVector params, PosNegRWExample ex, TIntDoubleMap gradient) {
		for (String f : localFeatures(params, ex.getGraph())) {
			double value = Dictionary.safeGet(params, f);
			// want to take theta toward zero, but not past it: gradient can't be bigger than theta
			
			double ret = Math.signum(value) * Math.min( Math.abs(value), parent.c.mu);
			parent.cumloss.add(LOSS.REGULARIZATION, parent.c.mu * Math.abs(value));
			gradient.adjustOrPutValue(ex.getGraph().featureLibrary.getId(f), ret, ret);
		}
	}
}
