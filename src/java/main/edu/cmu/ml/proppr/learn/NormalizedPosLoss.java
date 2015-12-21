package edu.cmu.ml.proppr.learn;

import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.learn.tools.LossData;
import edu.cmu.ml.proppr.learn.tools.LossData.LOSS;
import edu.cmu.ml.proppr.util.SRWOptions;
import edu.cmu.ml.proppr.util.math.ParamVector;
import edu.cmu.ml.proppr.util.math.SimpleSparse.FloatVector;
import gnu.trove.iterator.TIntDoubleIterator;
import gnu.trove.map.TIntDoubleMap;

public class NormalizedPosLoss extends LossFunction {

    @Override
	public int computeLossGradient(ParamVector params, PosNegRWExample example, TIntDoubleMap gradient, LossData lossdata, SRWOptions c) {
	PosNegRWExample ex = (PosNegRWExample) example;
	int nonzero = 0;
	double mag = 0;

	//if there are no negative nodes or no positive nodes, the probability of positive nodes
	//is zero or 1, and the empirical loss gradient is zero.
	if (ex.getNegList().length ==0 || ex.getPosList().length ==0) return nonzero;
		
	double sumPos = 0;
	for (int a : ex.getPosList()) {
	    sumPos += clip(ex.p[a]);
	}
	sumPos = clip(sumPos);


	for (int a : ex.getPosList()) {
	    /* wwc
	    for (TIntDoubleIterator da = ex.dp[a].iterator(); da.hasNext();) {
		da.advance();
		if (da.value() == 0)
		    continue;
		nonzero++;
		double aterm = -da.value() / sumPos;
		gradient.adjustOrPutValue(da.key(), aterm, aterm);
	    }
	    */
	    for (int k=0; k < ex.dp[a].index.length; k++) {
		if (ex.dp[a].val[k]!=0) {
		    nonzero++;
		    double aterm = -ex.dp[a].val[k] / sumPos;
		    gradient.adjustOrPutValue(ex.dp[a].index[k], aterm, aterm);
		}
	    }
	}

	lossdata.add(LOSS.LOG, -Math.log(sumPos));
		
	double sumPosNeg = 0;
	for (int pa : ex.getPosList()) {
	    sumPosNeg += clip(ex.p[pa]);
	}
	for (int pa : ex.getNegList()) {
	    sumPosNeg += clip(ex.p[pa]);
	}
	sumPosNeg = clip(sumPosNeg);
		
	for (int a : ex.getPosList()) {
	    /* wwc
	    for (TIntDoubleIterator da = ex.dp[a].iterator(); da.hasNext();) {
		da.advance();
		if (da.value() == 0)
		    continue;
		nonzero++;
		double bterm = da.value() / sumPosNeg;
		gradient.adjustOrPutValue(da.key(), bterm, bterm);
	    }
	    */
	    for (int k=0; k < ex.dp[a].index.length; k++) {
		if (ex.dp[a].val[k]!=0) {
		    nonzero++;
		    double bterm = ex.dp[a].val[k] / sumPosNeg;
		    gradient.adjustOrPutValue(ex.dp[a].index[k], bterm, bterm);
		}
	    }
	}
	for (int b : ex.getNegList()) {
	    /* wwc
	    for (TIntDoubleIterator db = ex.dp[b].iterator(); db.hasNext();) {
		db.advance();
		if (db.value() == 0)
		    continue;
		nonzero++;
		double bterm = db.value() / sumPosNeg;
		gradient.adjustOrPutValue(db.key(), bterm, bterm);
	    }
	    */
	    for (int k=0; k < ex.dp[b].index.length; k++) {
		if (ex.dp[b].val[k]!=0) {
		    nonzero++;
		    double bterm = ex.dp[b].val[k] / sumPosNeg;
		    gradient.adjustOrPutValue(ex.dp[b].index[k], bterm, bterm);
		}
	    }
	}

	lossdata.add(LOSS.LOG, Math.log(sumPosNeg));

	return nonzero;
    }	
}
