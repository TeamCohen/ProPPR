package edu.cmu.ml.proppr.learn;

import java.util.Set;

import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.learn.tools.LossData.LOSS;
import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.graph.LearningGraph;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.SRWOptions;
import edu.cmu.ml.proppr.util.math.MuParamVector;
import edu.cmu.ml.proppr.util.math.ParamVector;
import gnu.trove.map.TObjectDoubleMap;

public class LocalL1Regularizer extends L1Regularizer {
	private static final Logger log = Logger.getLogger(LocalL1Regularizer.class);
	public LocalL1Regularizer(SRW s) {
		super(s);
	}

	@Override
	public Set<String> localFeatures(ParamVector paramVec, LearningGraph graph) {
		return graph.getFeatureSet();
	}

	@Override
	public void prepareForSgd(ParamVector params, PosNegRWExample ex) {
		((MuParamVector)params).count();
		((MuParamVector)params).setLast(localFeatures(params,ex.getGraph()));
	}

	@Override
	public ParamVector setupParams(ParamVector paramVec) { return new MuParamVector(paramVec); }

	@Override
	public void cleanupParams(ParamVector paramVec, ParamVector apply) { 
		for(String f : (Set<String>) paramVec.keySet()) {
			// finish catching up the regularization:
			// Bj = Bj - lambda * (Rj)
			prepareFeature(paramVec,f,apply);
		}
		((MuParamVector)paramVec).setLast(paramVec.keySet());
	}

	// what was this for? [kmm]
//	@Override
//	public void initializeFeatures(ParamVector params, LearningGraph graph) {
//		super.initializeFeatures(params, graph);
//	}
	
	@Override
	public void prepareForExample(ParamVector params, LearningGraph graph, ParamVector apply) {
		for (String f : localFeatures(params, graph)) {
			prepareFeature(params,f,apply);
		}
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
	 * 
	 * 
	 * @param params
	 * @param f
	 * @param apply
	 */
	private void prepareFeature(ParamVector params, String f,ParamVector apply) {
		if (!parent.trainable(f)) return;
		int gap = ((MuParamVector)params).getLast(f);
		if (gap==0) return;

		//L1 with a proximal operator
		//
		//signum(w) * max(0.0, abs(w) - shrinkageVal)

		double shrinkageVal = gap * parent.learningRate() * parent.c.mu;
		double weightDecay;
		if((parent.c.mu != 0) && (!Double.isInfinite(shrinkageVal))){
			double value = Dictionary.safeGet(params,f);
			weightDecay = Math.signum(value) * Math.min(Math.abs(value), shrinkageVal);
			apply.adjustValue(f, weightDecay);
			//FIXME: opportunity for out-of-date `value`; may want to convert to a try loop

			if (log.isDebugEnabled()) log.debug("Regularizing "+f+" += "+ -weightDecay);
			parent.cumloss.add(LOSS.REGULARIZATION, gap * parent.c.mu * Math.abs(value));
		}
	}
}
