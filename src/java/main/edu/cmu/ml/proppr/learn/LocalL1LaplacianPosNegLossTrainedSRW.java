package edu.cmu.ml.proppr.learn;

import java.util.Set;
import java.util.List;

import edu.cmu.ml.proppr.learn.tools.LossData.LOSS;
import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.MuParamVector;
import edu.cmu.ml.proppr.util.ParamVector;
import edu.cmu.ml.proppr.util.SRWOptions;
import gnu.trove.map.TObjectDoubleMap;

public class LocalL1LaplacianPosNegLossTrainedSRW extends LocalL1SRW {
	public LocalL1LaplacianPosNegLossTrainedSRW(SRWOptions params) {
		super(params);
	}
	public LocalL1LaplacianPosNegLossTrainedSRW() { super(); }

	private void prepareFeature(ParamVector paramVec, String f) {
		if (!trainable(f)) return;
		int gap = ((MuParamVector)paramVec).getLast(f);
		if (gap==0) return;
		double value = Dictionary.safeGet(paramVec,f);

		double laplacian = 0;
		double sumofsquares = 0;

		String target = "#" + f;
		if(c.diagonalDegree.containsKey(target)){
			double positive = c.diagonalDegree.get(target)*value;
			double negativeSum = 0;
			sumofsquares = value*value;
			List<String> sims = c.affinity.get(target);
			for (String s : sims) {
				double svalue = Dictionary.safeGet(paramVec,s);
				negativeSum -= svalue;
				sumofsquares = sumofsquares + svalue*svalue;
			}
			laplacian = positive + negativeSum;
			//System.out.println("f: " + f +" laplacian:" + laplacian);
		}

		//Laplacian
		double powerTerm = Math.pow(1 - 2 * c.zeta * this.learningRate() * laplacian, gap);
		double weightDecay = laplacian * (powerTerm - 1);
		Dictionary.increment(paramVec, f, weightDecay);
		this.cumloss.add(LOSS.REGULARIZATION, gap * c.zeta * Math.pow(value, 2));

		//L1 with a proximal operator              
		//signum(w) * max(0.0, abs(w) - shrinkageVal)

		double shrinkageVal = gap * this.learningRate() * c.mu;
		if((c.mu != 0) && (!Double.isInfinite(shrinkageVal))){
			weightDecay = Math.signum(value) * Math.max(0.0, Math.abs(value) - shrinkageVal);
			Dictionary.set(paramVec, f, weightDecay);
			//FIXME: why is this being set instead of incremented?
			//FIXME: opportunity for out-of-date `value`; probably out to convert to a try loop
		}
		this.cumloss.add(LOSS.REGULARIZATION, gap * c.mu);             		
	}
}
