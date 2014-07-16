package edu.cmu.ml.praprolog.learn;

import java.util.Map;

import edu.cmu.ml.praprolog.learn.tools.PosNegRWExample;
import edu.cmu.ml.praprolog.util.ParamVector;

public class AprSRW<T> extends SRW<PosNegRWExample<T>> {
	public AprSRW() {
		//set walk parameters here
	}
	
	@Override
	public Map<String, Double> gradient(ParamVector paramVec, PosNegRWExample<T> example) {
		// startNode maps node->weight
		Map<T,Double> startNode = example.getQueryVec();
		
		// gradient maps feature->gradient with respect to that feature
		Map<String,Double> gradient = null;
		
		// your code here:
		
		return gradient;
	}
}
