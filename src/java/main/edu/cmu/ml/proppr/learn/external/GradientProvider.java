package edu.cmu.ml.proppr.learn.external;

import edu.cmu.ml.proppr.graph.ArrayLearningGraph;
import gnu.trove.map.TIntDoubleMap;

public abstract class GradientProvider {
	/**
	 * Permits arguments to be passed in from the command line / a config file.
	 * @param args
	 */
	abstract public void init(String ... args);
	
	/**
	 * Corrects any externally-derived label weights on the edges where they 
	 * occur, and returns a list of feature IDs which should not be regularized.
	 * @param g
	 * @return
	 */
	abstract public int[] updateGraph(ArrayLearningGraph g);
	/* Graph perusal template:
	 * 	for (int i=0; i<g.label_feature_id.length; i++) {
	 * 		String feature = g.featureLibrary.getSymbol(g.label_feature_id[i]);
	 * 		if (meetsSomeCondition(feature)) {
	 * 			g.label_feature_weight[i] = // adjusted value
	 * 		}
	 * 	}
	 */
	
	/**
	 * Set the gradient of any externally-derived features to zero. Optionally, perform
	 * any internal updates requiring the ProPPR-generated gradient.
	 * 
	 * @param g - necessary for mapping the feature ID in `gradient` to the string version
	 * @param gradient
	 */
	abstract public void updateGradient(ArrayLearningGraph g, TIntDoubleMap gradient);
	/* Gradient perusal template:
	 * 	for (TIntDoubleIterator it = gradient.iterator(); it.hasNext(); ) {
	 * 		it.advance();
	 * 		String feature = g.featureLibrary.getSymbol(it.key());
	 * 		if (meetsSomeCondition(feature)) {
	 * 			it.setValue(0.0);
	 * 		}
	 * 	}
	 */
}
