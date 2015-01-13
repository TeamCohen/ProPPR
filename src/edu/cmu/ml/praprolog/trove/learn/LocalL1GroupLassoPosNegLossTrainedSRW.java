package edu.cmu.ml.praprolog.trove.learn;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.ml.praprolog.trove.learn.tools.PosNegRWExample;
import edu.cmu.ml.praprolog.learn.tools.SRWParameters;
import edu.cmu.ml.praprolog.learn.tools.WeightingScheme;
import edu.cmu.ml.praprolog.learn.tools.LossData.LOSS;
import edu.cmu.ml.praprolog.util.Dictionary;
import edu.cmu.ml.praprolog.util.MuParamVector;
import edu.cmu.ml.praprolog.util.ParamVector;
import gnu.trove.map.hash.TObjectDoubleHashMap;

public class LocalL1GroupLassoPosNegLossTrainedSRW extends L1PosNegLossTrainedSRW {
	public LocalL1GroupLassoPosNegLossTrainedSRW(SRWParameters params) {
		super(params);
	}
	public LocalL1GroupLassoPosNegLossTrainedSRW() { super(); }

	@Override
	public Set<String> localFeatures(ParamVector paramVec, PosNegRWExample example) {
		return example.getGraph().getFeatureSet();
	}	
	@Override
	protected Double derivRegularization(String f, ParamVector paramVec) {
		// NB superclass records regularization loss for this clock cycle
		Double ret = super.derivRegularization(f, paramVec);
		return ret;
	}
	
	@Override
	public TObjectDoubleHashMap<String> gradient(ParamVector paramVec, PosNegRWExample example) {
		TObjectDoubleHashMap<String> ret = super.gradient(paramVec, example);
		((MuParamVector)paramVec).count();
		((MuParamVector)paramVec).setLast(localFeatures(paramVec,example));
		return ret;
	}
	
	@Override
	public ParamVector setupParams(ParamVector paramVec) { return new MuParamVector(paramVec); }
	
	@Override
	public void cleanupParams(ParamVector paramVec) { 
		for(String f : (Set<String>) paramVec.keySet()) {
			// finish catching up the regularization:
			// Bj = Bj - lambda * (Rj)
			prepareFeature(paramVec,f);
		}
		((MuParamVector)paramVec).setLast(paramVec.keySet());
	}
	
	@Override
	public void prepareGradient(ParamVector paramVec, PosNegRWExample example) {
		for (String f : localFeatures(paramVec,example)) {
			prepareFeature(paramVec,f);
		}
	}
	
	private void prepareFeature(ParamVector paramVec, String f) {
		// use last-1 here because superclass will apply regularization for this clock cycle
		// during the gradient() call
		int gap = ((MuParamVector)paramVec).getLast(f);
		if (gap==0) return;
		double value = Dictionary.safeGet(paramVec,f);
		
              
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
		}
 
              //Group Lasso
              double weightDecay = 0;
              if(c.zeta != 0){
                  double grouplasso = 0.5 * Math.pow(sumofsquares,-0.5);
                  if(!Double.isInfinite(grouplasso)){
                      //System.out.println("f: " + f +" group lasso:" + grouplasso);
                      weightDecay = Math.signum(value) * Math.max(0.0, Math.abs(value) - (gap * this.learningRate() * c.zeta * grouplasso));
	      	        Dictionary.reset(paramVec, f, weightDecay);
                  }
              }              
		
		//L1
		//signum(w) * max(0.0, abs(w) - shrinkageVal)
              
              double shrinkageVal = gap * this.learningRate() * c.mu;
              if((c.mu != 0) && (!Double.isInfinite(shrinkageVal))){
 		    weightDecay = Math.signum(value) * Math.max(0.0, Math.abs(value) - shrinkageVal);
		    Dictionary.reset(paramVec, f, weightDecay);
              }
		this.cumloss.add(LOSS.REGULARIZATION, gap * c.mu * value);              		
	}
}
