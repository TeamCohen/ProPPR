package edu.cmu.ml.proppr.learn;


import edu.cmu.ml.proppr.learn.tools.LossData;
import edu.cmu.ml.proppr.learn.tools.LossData.LOSS;
import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.SRWOptions;
import edu.cmu.ml.proppr.util.SymbolTable;
import edu.cmu.ml.proppr.util.math.MuParamVector;
import edu.cmu.ml.proppr.util.math.ParamVector;
import gnu.trove.map.TIntDoubleMap;

public class RegularizeL1 extends Regularize {

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
	protected void synchronousUpdate(SRWOptions c, ParamVector<String,?> params, String f, 
			TIntDoubleMap gradient, LossData loss, SymbolTable<String> featureLibrary) {
			double value = Dictionary.safeGet(params, f);
			// want to take theta toward zero, but not past it: gradient can't be bigger than theta
			
			double ret = Math.signum(value) * Math.min( Math.abs(value), c.mu);
			loss.add(LOSS.REGULARIZATION, c.mu * Math.abs(value));
			gradient.adjustOrPutValue(featureLibrary.getId(f), ret, ret);
	}
	
	/**
	 * We want to do g regularization updates simultaneously, as if they had been applied in previous gradient updates.
	 * 
	 * A single full regularization update for L1 is:
	 *   theta_f' = theta_f - learningRate * (sign(theta_f) * min(abs(theta_f), mu))
	 * 
	 * So we take at most learningRate * mu off the value of theta. In other words,
	 *   theta_f' = theta_f - (sign(theta_f) * min(abs(theta_f), learningRate * mu))
	 *   
	 * This makes it easier to take g updates -- if we do that g times, we'll take at most g * learningRate * mu off the value of theta.
	 *   theta_f' = theta_f - (sign(theta_f) * min(abs(theta_f), g * learningRate * mu))
	 * @param params
	 * @param apply
	 * @param f
	 */
	@Override
	protected void lazyUpdate(SRWOptions c, MuParamVector<String> params, ParamVector<String,?> apply, String f,
			LossData loss, double learningRate) {
//		if (!parent.trainable(f)) return;
		int gap = getGap(params,f);
		if (gap==0) return;

		//L1 with a proximal operator
		//
		//signum(w) * max(0.0, abs(w) - shrinkageVal)

		double shrinkageVal = gap * learningRate * c.mu;
		double weightDecay;
		if((c.mu != 0) && (!Double.isInfinite(shrinkageVal))){
			double value = Dictionary.safeGet(params,f);
			weightDecay = Math.signum(value) * Math.min(Math.abs(value), shrinkageVal);
			apply.adjustValue(f, weightDecay);
			//FIXME: opportunity for out-of-date `value`; may want to convert to a try loop

			loss.add(LOSS.REGULARIZATION, gap * c.mu * Math.abs(value));
		}
	}
}
