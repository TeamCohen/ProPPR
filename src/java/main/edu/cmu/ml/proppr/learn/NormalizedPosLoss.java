package edu.cmu.ml.proppr.learn;

import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.learn.tools.LossData;
import edu.cmu.ml.proppr.learn.tools.LossData.LOSS;
import edu.cmu.ml.proppr.util.SRWOptions;
import edu.cmu.ml.proppr.util.math.ParamVector;
import gnu.trove.iterator.TIntDoubleIterator;
import gnu.trove.map.TIntDoubleMap;

public class NormalizedPosLoss extends LossFunction {

	@Override
	public int computeLossGradient(ParamVector params, PosNegRWExample example, TIntDoubleMap gradient, LossData lossdata, SRWOptions c) {
		PosNegRWExample ex = (PosNegRWExample) example;
		int nonzero = 0;
		double mag = 0;

		double sumPos = 0;
		for (int a : ex.getPosList()) {
			sumPos += clip(ex.p[a]);
		}


		for (int a : ex.getPosList()) {
			for (TIntDoubleIterator da = ex.dp[a].iterator(); da.hasNext();) {
				da.advance();
				if (da.value() == 0)
					continue;
				nonzero++;
				double aterm = -da.value() / sumPos;
				gradient.adjustOrPutValue(da.key(), aterm, aterm);
			}
		}

		lossdata.add(LOSS.LOG, -Math.log(sumPos));
		
		double sumPosNeg = 0;
		for (double pa : ex.getPosList()) {
			sumPosNeg += clip(pa);
		}
		for (double pa : ex.getNegList()) {
			sumPosNeg += clip(pa);
		}

		for (TIntDoubleMap dpa : ex.dp) {

			if (dpa == null)
				continue;
			for (TIntDoubleIterator da = dpa.iterator(); da.hasNext();) {
				da.advance();
				if (da.value() == 0)
					continue;
				nonzero++;
				double nterm = da.value() / sumPosNeg;
				gradient.adjustOrPutValue(da.key(), nterm, nterm);
			}
		}
		lossdata.add(LOSS.LOG, Math.log(sumPosNeg));

		return nonzero;
	}

}
