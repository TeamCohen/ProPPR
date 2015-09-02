package edu.cmu.ml.proppr.learn;

import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.learn.tools.LossData;
import edu.cmu.ml.proppr.util.SRWOptions;
import edu.cmu.ml.proppr.util.math.ParamVector;
import gnu.trove.map.TIntDoubleMap;

public abstract class LossFunction {
	protected static final double BOUND = 1.0e-15; //Prevent infinite log loss.
	protected static final Logger log = Logger.getLogger(SRW.class);

	public double clip(double prob)
	{
		if(prob <= 0) return BOUND;
		return prob;
	}
	
	public abstract int computeLossGradient(ParamVector params, PosNegRWExample example, TIntDoubleMap gradient, LossData lossdata, SRWOptions c);

}
