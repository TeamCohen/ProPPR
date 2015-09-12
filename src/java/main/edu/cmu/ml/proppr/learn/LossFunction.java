package edu.cmu.ml.proppr.learn;

import java.lang.reflect.InvocationTargetException;

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

	@Override
	protected LossFunction clone() throws CloneNotSupportedException {
		Class<? extends LossFunction> clazz = this.getClass();
		try {
			LossFunction copy = clazz.getConstructor().newInstance();
			return copy;
		} catch (InstantiationException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
		}
		throw new CloneNotSupportedException("Programmer error in LossDate subclass "+clazz.getName()
		+": Must provide the standard LossData constructor signature, or else override clone()");
	}
	
}
