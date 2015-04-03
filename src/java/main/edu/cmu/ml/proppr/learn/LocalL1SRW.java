package edu.cmu.ml.proppr.learn;

import java.util.Set;

import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.learn.SRW.SgdExample;
import edu.cmu.ml.proppr.learn.tools.LossData.LOSS;
import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.graph.LearningGraph;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.MuParamVector;
import edu.cmu.ml.proppr.util.ParamVector;
import edu.cmu.ml.proppr.util.SRWOptions;
import gnu.trove.map.TObjectDoubleMap;

public class LocalL1SRW extends L1SRW {
	private static final Logger log = Logger.getLogger(LocalL1SRW.class);
	public LocalL1SRW(SRWOptions params) {
		super(params);
	}
	public LocalL1SRW() { super(); }

	@Override
	public Set<String> localFeatures(ParamVector paramVec, LearningGraph graph) {
		return graph.getFeatureSet();
	}

	@Override
	protected void sgd(ParamVector params, SgdExample ex) {
		((MuParamVector)params).count();
		((MuParamVector)params).setLast(localFeatures(params,ex.g));
		super.sgd(params, ex);
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

	@Override
	public void initializeFeatures(ParamVector params, LearningGraph graph) {
		super.initializeFeatures(params, graph);
	}
	
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
		if (!trainable(f)) return;
		int gap = ((MuParamVector)params).getLast(f);
		if (gap==0) return;

		//L1 with a proximal operator
		//
		//signum(w) * max(0.0, abs(w) - shrinkageVal)

		double shrinkageVal = gap * this.learningRate() * c.mu;
		double weightDecay;
		if((c.mu != 0) && (!Double.isInfinite(shrinkageVal))){
			double value = Dictionary.safeGet(params,f);
			weightDecay = Math.signum(value) * Math.min(Math.abs(value), shrinkageVal);
			apply.adjustValue(f, weightDecay);
			//FIXME: opportunity for out-of-date `value`; may want to convert to a try loop

			if (log.isDebugEnabled()) log.debug("Regularizing "+f+" += "+ -weightDecay);
			this.cumloss.add(LOSS.REGULARIZATION, gap * c.mu * Math.abs(value));
		}
	}
}
