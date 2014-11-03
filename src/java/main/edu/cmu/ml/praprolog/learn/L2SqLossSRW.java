package edu.cmu.ml.praprolog.learn;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import edu.cmu.ml.praprolog.examples.PairwiseRWExample;
import edu.cmu.ml.praprolog.examples.PairwiseRWExample.HiLo;
import edu.cmu.ml.praprolog.learn.tools.LossData;
import edu.cmu.ml.praprolog.learn.tools.LossData.LOSS;
import edu.cmu.ml.praprolog.util.Dictionary;
import edu.cmu.ml.praprolog.util.ParamVector;

public class L2SqLossSRW<T> extends SRW<PairwiseRWExample<T>> {
	private static final Logger log = Logger.getLogger(L2SqLossSRW.class);
	protected double margin=0.01;
	private LossData cumloss;
	
	public L2SqLossSRW() {
		this.cumloss = new LossData();
	}
	/**
	 * The loss associated with a difference in ranking scores of diff.
	 * @param diff
	 * @return
	 */
	private double loss(double diff) {
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
		return (this.untrainedFeatures.contains(f)) ? 0 : 2*paramVec.get(f)*this.mu;
	}
	
//	/**
//	 * The empirical loss of the current ranking. [This method originally from PairwiseLossTrainedSRW]
//	 * @param weightVec
//	 * @param pairwiseRWExample
//	 */
//	public double empiricalLoss(ParamVector paramVec,
//			PairwiseRWExample<T> example) {
//		Map<T,Double> vec = this.rwrUsingFeatures(example.getGraph(), example.getQueryVec(), paramVec);
//		double loss = 0;
//		    
//		for (HiLo<T> hl : example.getHiLoList()) {
//			// zero loss if lo < hi (===if lo-hi < 0); proportional to sqdifference otherwise
//			double delta = this.loss(Dictionary.safeGet(vec, hl.getLo()) - Dictionary.safeGet(vec, hl.getHi())); 
//			loss += delta;
//		}
//		return loss;
//	}
	
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
	public Map<String,Double> gradient(ParamVector paramVec, PairwiseRWExample<T> example) {
		Map<T,Double> p = this.rwrUsingFeatures(example.getGraph(),example.getQueryVec(),paramVec);
		Map<T,Map<String,Double>> d = this.derivRWRbyParams(example.getGraph(),example.getQueryVec(),paramVec);
		Map<String,Double> derivFparamVec = new TreeMap<String,Double>();
		Set<String> allFeatures = paramVec.keySet();
		for (String f : allFeatures) {
			derivFparamVec.put(f, derivRegularization(f,paramVec));
		}
		
		Set<String> trainableFeatures = trainableFeatures(paramVec);
		for (HiLo<T> hl : example.getHiLoList()) {
			double delta = Dictionary.safeGet(p, hl.getLo()) - Dictionary.safeGet(p,hl.getHi());
			for (String f : trainableFeatures) {
				double del = derivLoss(delta) * (Dictionary.safeGetGet(d, hl.getLo(), f) - Dictionary.safeGetGet(d, hl.getHi(), f));
				Dictionary.increment(derivFparamVec, f, del);
			}
			this.cumloss.add(LOSS.L2, this.loss(delta));
		}
		
		for (String f : trainableFeatures(derivFparamVec)) {
			derivFparamVec.put(f,derivFparamVec.get(f) / example.length());
			if (derivFparamVec.get(f).isInfinite()) {
				throw new IllegalStateException("Asymptote error");
			}
		}
		return derivFparamVec;
	}
	

	public ParamVector train(List<PairwiseRWExample<T>> trainingExamples, ParamVector initialParamVec) {
		this.epoch = 0;
		ParamVector paramVec = initialParamVec;
		for (int i=0; i<NUM_EPOCHS; i++) {
			this.epoch++;
//			int ex=0;
			for (PairwiseRWExample<T> example : trainingExamples) { //ex++;
				if (log.isDebugEnabled()) log.debug(String.format("epoch: %d length: %s train: %s",epoch, example.length(),example.toString()));
				trainOnExample(paramVec, example);
			}
//			double trainLoss = averageLoss(paramVec,trainingExamples);
//			if (log.isInfoEnabled()) log.info(String.format("epoch: %d trainLoss: %f",epoch, trainLoss));
		}
		return paramVec;
	}

	@Override
	public LossData cumulativeLoss() {
		return cumloss.copy();
	}
	@Override
	public void clearLoss() {
		cumloss.clear(); // ?
	}
}
