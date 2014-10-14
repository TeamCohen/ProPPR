package edu.cmu.ml.praprolog.trove.learn;

import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cmu.ml.praprolog.trove.learn.tools.PosNegRWExample;
import edu.cmu.ml.praprolog.learn.tools.WeightingScheme;
import edu.cmu.ml.praprolog.learn.tools.LossData.LOSS;
import edu.cmu.ml.praprolog.util.Dictionary;
import edu.cmu.ml.praprolog.util.MuParamVector;
import edu.cmu.ml.praprolog.util.ParamVector;
import gnu.trove.map.hash.TObjectDoubleHashMap;

public class LocalL1GroupLassoPosNegLossTrainedSRW extends L1PosNegLossTrainedSRW {
	public LocalL1GroupLassoPosNegLossTrainedSRW(int maxT, double mu, double eta, WeightingScheme wScheme, double delta, String affgraph, double zeta) {
		super(maxT,mu,eta,wScheme,delta,affgraph,zeta);
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
		if(diagonalDegree.containsKey(target)){
	  	    double positive = this.diagonalDegree.get(target)*value;
		    double negativeSum = 0;
                  sumofsquares = value*value;
		    List<String> sims = this.affinity.get(target);
		    for (String s : sims) {
                         double svalue = Dictionary.safeGet(paramVec,s);
  			    negativeSum -= svalue;
                         sumofsquares = sumofsquares + svalue*svalue;
		    }
		}
 
              //Group Lasso
              double weightDecay = 0;
              if(this.zeta != 0){
                  double grouplasso = 0.5 * Math.pow(sumofsquares,-0.5);
                  if(!Double.isInfinite(grouplasso)){
                      //System.out.println("f: " + f +" group lasso:" + grouplasso);
                      weightDecay = Math.signum(value) * Math.max(0.0, Math.abs(value) - (gap * this.learningRate() * this.zeta * grouplasso));
	      	        Dictionary.reset(paramVec, f, weightDecay);
                  }
              }              
		
		//L1
		//signum(w) * max(0.0, abs(w) - shrinkageVal)
              
              double shrinkageVal = gap * this.learningRate() * this.mu;
              if((this.mu != 0) && (!Double.isInfinite(shrinkageVal))){
 		    weightDecay = Math.signum(value) * Math.max(0.0, Math.abs(value) - shrinkageVal);
		    Dictionary.reset(paramVec, f, weightDecay);
              }
		this.cumloss.add(LOSS.REGULARIZATION, gap * this.mu * value);              		
	}
}
