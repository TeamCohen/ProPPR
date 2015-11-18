package edu.cmu.ml.proppr.learn;

import java.util.Set;

import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.examples.RWExample;
import edu.cmu.ml.proppr.graph.LearningGraph;
import edu.cmu.ml.proppr.learn.tools.LossData;
import edu.cmu.ml.proppr.learn.tools.LossData.LOSS;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.SRWOptions;
import edu.cmu.ml.proppr.util.SymbolTable;
import edu.cmu.ml.proppr.util.math.MuParamVector;
import edu.cmu.ml.proppr.util.math.ParamVector;
import gnu.trove.map.TIntDoubleMap;
import org.apache.log4j.Logger;


public class Regularize {
	private static final Logger log = Logger.getLogger(Regularize.class);
	/**
	 * This is a normal update where each global feature is
	 * regularized at every example. Override to provide a particular
	 * regularization function.
	 */
	protected void synchronousUpdate(SRWOptions c, ParamVector<String,?> params, String f, 
			TIntDoubleMap gradient, LossData loss, SymbolTable<String> featureLibrary) {}
	/**
	 * This is a lazy update, where the features in a particular
	 * example are regularized in a batch, applying the regularization
	 * as many times as since the last time that feature was
	 * updated. Override to proved a particular regularization
	 * function.
	 */
	protected void lazyUpdate(SRWOptions c, MuParamVector<String> params, 
			ParamVector<String,?> apply, String f, LossData loss, double learningRate) {}

	/** Utility function to avoid race conditions that put lazy
	 * regularization in an invalid state.
	 */
	protected int getGap(MuParamVector<String> params, String f) {
		int gap = params.getLast(f);
		int tries = 0;
		while (gap < 0) { // Can't figure out why gap is showing up < 0 :(
			try {
				Thread.sleep(10);
				gap = params.getLast(f);
				tries++;
			} catch(InterruptedException e) {}
		}
		if (tries>1 && log.isInfoEnabled()) { log.info("Took "+(tries+1)+" tries to get a valid gap measure @ "+f); }
		return gap;
	}
}
