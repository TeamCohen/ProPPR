package edu.cmu.ml.proppr.learn;

import java.io.File;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.graph.LearningGraph;
import edu.cmu.ml.proppr.learn.tools.SquashingFunction;
import edu.cmu.ml.proppr.learn.tools.LossData.LOSS;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.SRWOptions;
import edu.cmu.ml.proppr.util.math.MuParamVector;
import edu.cmu.ml.proppr.util.math.ParamVector;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TObjectDoubleMap;

public class LocalL2SRW extends L2SRW {
	private static final Logger log = Logger.getLogger(LocalL2SRW.class);
	public LocalL2SRW(SRWOptions params) {
		super(params);
	}
	public LocalL2SRW() { super(); }

	@Override
	public Set<String> localFeatures(ParamVector<String,?> paramVec, LearningGraph graph) {
		return graph.getFeatureSet();
	}
	
	@Override
	protected TIntDoubleMap gradient(ParamVector<String,?> params, PosNegRWExample ex) {
		TIntDoubleMap ret = super.gradient(params, ex);
		((MuParamVector)params).count();
		((MuParamVector)params).setLast(localFeatures(params,ex.getGraph()));
		return ret;
	}
	
	@Override
	public ParamVector<String,?> setupParams(ParamVector<String,?> paramVec) { return new MuParamVector<String>(paramVec); }
	
	@Override
	public void cleanupParams(ParamVector<String,?> paramVec, ParamVector<String,?> apply) { 
		for(String f : (Set<String>) paramVec.keySet()) {
			// finish catching up the regularization:
			// Bj = Bj - lambda * (Rj)
			prepareFeature(paramVec,f,apply);
		}
		((MuParamVector)paramVec).setLast(paramVec.keySet());
	}
	
	@Override
	public void initializeFeatures(ParamVector<String,?> params, LearningGraph graph) {
		super.initializeFeatures(params, graph);
	}
	
	@Override
	public void prepareForExample(ParamVector<String,?> params, LearningGraph graph, ParamVector<String,?> apply) {
		for (String f : localFeatures(params,graph)) {
			prepareFeature(params,f,apply);
		}
	}
	/**
	 * We want to do g regularization updates simultaneously, as if they had been applied in previous gradients.
	 * 
	 * A single full regularization update is:
	 *   theta_f' = theta_f - learningRate * (2 * mu * theta_f )
	 *            = theta_f * (1 - 2 * mu * learningRate)
	 * 
	 * We can apply multiple updates by subbing theta' for theta, raising the multiplication factor to a power.
	 * In this way, g updates produce:
	 *   theta_f' = theta_f * (1 - 2 * mu * learningRate) ^ g
	 * 
	 * We want to pose the update as an increment, so we can use our threadsafe update method.
	 * 
	 *   theta_f' = theta_f + theta_f * (1 - 2 * mu * learningRate) ^ g - theta_f
	 *            = theta_f + theta_f * [(1 - 2 * mu * learningRate) ^ g - 1]
	 *            
	 * Thus our adjustment value is
	 *   theta_f * [(1 - 2 * mu * learningRate) ^ g - 1]
	 *   
	 * @param paramVec
	 * @param f
	 * @param apply
	 */
	private void prepareFeature(ParamVector<String,?> paramVec, String f, ParamVector<String,?> apply) {
		if (!trainable(f)) return;
		int gap = ((MuParamVector)paramVec).getLast(f);
		if (gap==0) return;
		double value = Dictionary.safeGet(paramVec,f);
		double powerTerm = Math.pow(1 - 2 * c.mu * this.learningRate(), gap);
		double weightDecay = value * (powerTerm - 1);
		//FIXME: opportunity for out-of-date `value`; probably ought to convert to a try loop
		if (log.isDebugEnabled()) log.debug("Regularizing "+f+" += "+ -weightDecay);
		double loss = gap * c.mu * Math.pow(value, 2);
//		if (loss<0 && log.isInfoEnabled()) log.info("gap: "+gap+" mu: "+c.mu+" value: "+value+" value**2: "+Math.pow(value, 2));
		this.cumloss.add(LOSS.REGULARIZATION, loss);
		apply.adjustValue(f, weightDecay);
	}
}
