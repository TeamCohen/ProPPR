package edu.cmu.ml.proppr.learn;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.List;

import edu.cmu.ml.proppr.learn.tools.LossData.LOSS;
import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.learn.tools.WeightingScheme;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.MuParamVector;
import edu.cmu.ml.proppr.util.ParamVector;
import gnu.trove.map.TObjectDoubleMap;

public class LocalL1LaplacianPosNegLossTrainedSRW extends L1PosNegLossTrainedSRW {
	public LocalL1LaplacianPosNegLossTrainedSRW(int maxT, double mu, double eta, WeightingScheme<String> wScheme, double delta, File affgraph, double zeta) {
		super(maxT,mu,eta,wScheme,delta,affgraph,zeta);
	}
	public LocalL1LaplacianPosNegLossTrainedSRW() { super(); }

	@Override
	public Set<String> localFeatures(ParamVector paramVec, PosNegRWExample example) {
		return example.getGraph().getFeatureSet();
	}
	
	@Override
	protected double derivRegularization(String f, ParamVector paramVec) {
		// NB superclass records regularization loss for this clock cycle
		Double ret = super.derivRegularization(f, paramVec);
		return ret;
	}
	
	@Override
	public TObjectDoubleMap<String> gradient(ParamVector paramVec, PosNegRWExample example) {
		TObjectDoubleMap<String> ret = super.gradient(paramVec, example);
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

		double laplacian = 0;
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
		    laplacian = positive + negativeSum;
                  //System.out.println("f: " + f +" laplacian:" + laplacian);
		}
               
              //Laplacian
		double powerTerm = Math.pow(1 - 2 * this.zeta * this.learningRate() * laplacian, gap);
		double weightDecay = laplacian * (powerTerm - 1);
		Dictionary.increment(paramVec, f, weightDecay);
		this.cumloss.add(LOSS.REGULARIZATION, gap * this.zeta * Math.pow(value, 2));
              
		//L1 with a proximal operator              
		//signum(w) * max(0.0, abs(w) - shrinkageVal)
              
              double shrinkageVal = gap * this.learningRate() * this.mu;
              if((this.mu != 0) && (!Double.isInfinite(shrinkageVal))){
 		    weightDecay = Math.signum(value) * Math.max(0.0, Math.abs(value) - shrinkageVal);
		    Dictionary.reset(paramVec, f, weightDecay);
              }
		this.cumloss.add(LOSS.REGULARIZATION, gap * this.mu);             		
	}
}
