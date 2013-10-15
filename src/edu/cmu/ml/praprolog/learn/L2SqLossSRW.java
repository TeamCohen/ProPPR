package edu.cmu.ml.praprolog.learn;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import edu.cmu.ml.praprolog.learn.PairwiseRWExample.HiLo;
import edu.cmu.ml.praprolog.util.Dictionary;

public class L2SqLossSRW<T> extends SRW<PairwiseRWExample<T>> {
	private static final Logger log = Logger.getLogger(L2SqLossSRW.class);
	protected double margin=0.01;
	/**
	 * The loss associated with a difference in ranking scores of diff.
	 * @param diff
	 * @return
	 */
	public double loss(double diff) {
		return (diff+margin)<0 ? 0 : 0.5*diff*diff;
	}
	/**
	 * The derivative of the loss associated with a difference in ranking scores of diff.
	 * @param diff
	 * @return
	 */
	public double derivLoss(double diff) {
		return (diff+margin)<0 ? 0 : diff;
	}
	
	/**
	 * [This method originally from L2RegularizedLearner; Java can't do multiple inheritance]
	 * @param f
	 * @param paramVec
	 * @return
	 */
	public double derivRegularization(String f,Map<String,Double> paramVec) {
		return (this.untrainedFeatures.contains(f)) ? 0 : paramVec.get(f)*this.mu;
	}
	
	/**
	 * The empirical loss of the current ranking. [This method originally from PairwiseLossTrainedSRW]
	 * @param weightVec
	 * @param pairwiseRWExample
	 */
	public double empiricalLoss(Map<String, Double> paramVec,
			PairwiseRWExample<T> example) {
		Map<T,Double> vec = this.rwrUsingFeatures(example.getGraph(), example.getQueryVec(), paramVec);
		double loss = 0;
		    
		for (HiLo<T> hl : example.getHiLoList()) {
			// zero loss if lo < hi (===if lo-hi < 0); proportional to sqdifference otherwise
			double delta = this.loss(Dictionary.safeGet(vec, hl.getLo()) - Dictionary.safeGet(vec, hl.getHi())); 
			loss += delta;
		}
		return loss;
	}
	/**
	 * Compute the local gradient of the parameters, associated
        with a particular start vector and a particular desired
        ranking.  The desired ranking is a list of pairs of the form
        (hi,lo) where hi is a node that should be ranked ahead of -
        i.e. have a higher score than - the corresponding lo.
        @param paramVec Mapping from edge features to values
        @param example
        @return Map from edge features to values
	 */
	public Map<String,Double> gradient(Map<String,Double> paramVec, PairwiseRWExample<T> example) {
		Map<T,Double> p = this.rwrUsingFeatures(example.getGraph(),example.getQueryVec(),paramVec);
		Map<T,Map<String,Double>> d = this.derivRWRbyParams(example.getGraph(),example.getQueryVec(),paramVec);
		Map<String,Double> derivFparamVec = new TreeMap<String,Double>();
		for (String f : paramVec.keySet()) {
			derivFparamVec.put(f, derivRegularization(f,paramVec));
		}
		
		for (HiLo<T> hl : example.getHiLoList()) {
			double delta = Dictionary.safeGet(p, hl.getLo()) - Dictionary.safeGet(p,hl.getHi());
			for (String f : trainableFeatures(paramVec)) {
				double del = derivLoss(delta) * (Dictionary.safeGetGet(d, hl.getLo(), f) - Dictionary.safeGetGet(d, hl.getHi(), f));
				Dictionary.increment(derivFparamVec, f, del);
			}
		}
		
		for (String f : trainableFeatures(derivFparamVec)) {
			derivFparamVec.put(f,derivFparamVec.get(f) / example.length());
			if (derivFparamVec.get(f).isInfinite()) {
				throw new IllegalStateException("Asymptote error");
			}
		}
		return derivFparamVec;
	}
	

	public Map<String, Double> train(List<PairwiseRWExample<T>> trainingExamples, Map<String, Double> initialParamVec) {
		this.epoch = 0;
		Map<String,Double> paramVec = initialParamVec;
		for (int i=0; i<NUM_EPOCHS; i++) {
			this.epoch++;
//			int ex=0;
			for (PairwiseRWExample<T> example : trainingExamples) { //ex++;
				if (log.isDebugEnabled()) log.debug(String.format("epoch: %d length: %s train: %s",epoch, example.length(),example.toString()));
				trainOnExample(paramVec, example);
			}
			double trainLoss = averageLoss(paramVec,trainingExamples);
			if (log.isInfoEnabled()) log.info(String.format("epoch: %d trainLoss: %f",epoch, trainLoss));
		}
		return paramVec;
	}
	
}
