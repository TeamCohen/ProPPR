package edu.cmu.ml.proppr.learn;

import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.learn.tools.LossData;
import edu.cmu.ml.proppr.learn.tools.LossData.LOSS;
import edu.cmu.ml.proppr.util.SRWOptions;
import edu.cmu.ml.proppr.util.math.ParamVector;
import gnu.trove.iterator.TIntDoubleIterator;
import gnu.trove.map.TIntDoubleMap;

public class PosNegLoss extends LossFunction {

	@Override
	public int computeLossGradient(ParamVector params, PosNegRWExample example,
			TIntDoubleMap gradient, LossData lossdata, SRWOptions c) {
		PosNegRWExample ex = (PosNegRWExample) example;
		int nonzero=0;
		
		// add empirical loss gradient term
		// positive examples
		double pmax = 0;
		for (int a : ex.getPosList()) {
			double pa = clip(ex.p[a]);
			if(pa > pmax) pmax = pa;
			for (TIntDoubleIterator da = ex.dp[a].iterator(); da.hasNext(); ) {
				da.advance();
				if (da.value()==0) continue;
				nonzero++;
				double aterm = -da.value() / pa;
				gradient.adjustOrPutValue(da.key(), aterm, aterm);
			}
			if (log.isDebugEnabled()) log.debug("+p="+pa);
			lossdata.add(LOSS.LOG, -Math.log(pa));
		}

		//negative instance booster
		double h = pmax + c.delta;
		double beta = 1;
		if(c.delta < 0.5) beta = (Math.log(1/h))/(Math.log(1/(1-h)));

		// negative examples
		for (int b : ex.getNegList()) {
			double pb = clip(ex.p[b]);
			for (TIntDoubleIterator db = ex.dp[b].iterator(); db.hasNext(); ) {
				db.advance();
				if (db.value()==0) continue;
				nonzero++;
				double bterm = beta * db.value() / (1 - pb);
				gradient.adjustOrPutValue(db.key(), bterm, bterm);
			}
			if (log.isDebugEnabled()) log.debug("-p="+pb);
			lossdata.add(LOSS.LOG, -Math.log(1.0-pb));
		}
		return nonzero;
	}

}
