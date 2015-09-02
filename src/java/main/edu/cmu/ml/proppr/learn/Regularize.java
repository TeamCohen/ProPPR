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
import edu.cmu.ml.proppr.util.math.ParamVector;
import gnu.trove.map.TIntDoubleMap;

public abstract class Regularize {
	protected abstract void synchronousUpdate(SRWOptions c, ParamVector params, String f, TIntDoubleMap gradient, LossData loss, SymbolTable<String> featureLibrary);
	protected abstract void lazyUpdate(SRWOptions c, ParamVector params, ParamVector apply, String f, LossData loss, double learningRate);
}
