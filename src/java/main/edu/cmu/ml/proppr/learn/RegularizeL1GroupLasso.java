package edu.cmu.ml.proppr.learn;

import java.util.Set;
import java.util.List;

import edu.cmu.ml.proppr.learn.tools.LossData;
import edu.cmu.ml.proppr.learn.tools.LossData.LOSS;
import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.SRWOptions;
import edu.cmu.ml.proppr.util.math.MuParamVector;
import edu.cmu.ml.proppr.util.math.ParamVector;
import gnu.trove.map.TObjectDoubleMap;

public class RegularizeL1GroupLasso extends RegularizeL1 {

	@Override
	protected void lazyUpdate(SRWOptions c, MuParamVector<String> params, ParamVector<String,?> apply, String f,
			LossData loss, double learningRate) {
		int gap = getGap(params,f);
		if (gap==0) return;
		double value = Dictionary.safeGet(params,f);

		double sumofsquares = 0;
		String target = "#" + f;
		if(c.diagonalDegree.containsKey(target)){
			double positive = c.diagonalDegree.get(target)*value;
			double negativeSum = 0;
			sumofsquares = value*value;
			List<String> sims = c.affinity.get(target);
			for (String s : sims) {
				double svalue = Dictionary.safeGet(params,s);
				negativeSum -= svalue;
				sumofsquares = sumofsquares + svalue*svalue;
			}
		}

		//Group Lasso
		double weightDecay = 0;
		if(c.zeta != 0){
			double grouplasso = 0.5 * Math.pow(sumofsquares,-0.5);
			if(!Double.isInfinite(grouplasso)){
				//System.out.println("f: " + f +" group lasso:" + grouplasso);
				weightDecay = Math.signum(value) * Math.max(0.0, Math.abs(value) - (gap * learningRate * c.zeta * grouplasso));
				Dictionary.set(params, f, weightDecay);
				
				loss.add(LOSS.REGULARIZATION, gap * learningRate * c.zeta * grouplasso);             		
			}
		}     


		//L1 with a proximal operator              
		//signum(w) * max(0.0, abs(w) - shrinkageVal)

		double shrinkageVal = gap * learningRate * c.mu;
		if((c.mu != 0) && (!Double.isInfinite(shrinkageVal))){
			weightDecay = Math.signum(value) * Math.max(0.0, Math.abs(value) - shrinkageVal);
			Dictionary.set(params, f, weightDecay);
			//FIXME: why is this being set instead of incremented?
			//FIXME: opportunity for out-of-date `value`; probably out to convert to a try loop
		}
		loss.add(LOSS.REGULARIZATION, gap * c.mu);             		
	}
}
