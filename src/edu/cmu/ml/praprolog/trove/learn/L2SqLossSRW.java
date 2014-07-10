package edu.cmu.ml.praprolog.trove.learn;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import edu.cmu.ml.praprolog.learn.tools.LossData;
import edu.cmu.ml.praprolog.learn.tools.LossData.LOSS;
import edu.cmu.ml.praprolog.trove.learn.tools.PairwiseRWExample;
import edu.cmu.ml.praprolog.trove.learn.tools.PairwiseRWExample.HiLo;
import edu.cmu.ml.praprolog.util.Dictionary;
import edu.cmu.ml.praprolog.util.ParamVector;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;

public class L2SqLossSRW extends SRW<PairwiseRWExample> {
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
	
//	/**
//	 * The empirical loss of the current ranking. [This method originally from PairwiseLossTrainedSRW]
//	 * @param weightVec
//	 * @param pairwiseRWExample
//	 */
//	@Override
//	public double empiricalLoss(ParamVector paramVec,
//			PairwiseRWExample example) {
//		TIntDoubleMap vec = this.rwrUsingFeatures(example.getGraph(), example.getQueryVec(), paramVec);
//		double loss = 0;
//		    
//		for (HiLo hl : example.getHiLoList()) {
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
	@Override
	public TObjectDoubleHashMap<String> gradient(ParamVector paramVec, PairwiseRWExample example) {
		TIntDoubleMap p = this.rwrUsingFeatures(example.getGraph(),example.getQueryVec(),paramVec);
		TIntObjectMap<TObjectDoubleHashMap<String>> d = this.derivRWRbyParams(example.getGraph(),example.getQueryVec(),paramVec);
		TObjectDoubleHashMap<String> derivFparamVec = new TObjectDoubleHashMap<String>();
		Set<String> features = paramVec.keySet();
		for (String f : features) {
			derivFparamVec.put(f, derivRegularization(f,paramVec));
		}
		
		for (HiLo hl : example.getHiLoList()) {
			double delta = Dictionary.safeGet(p, hl.getLo()) - Dictionary.safeGet(p,hl.getHi());
			for (String f : trainableFeatures(paramVec)) {
				double del = derivLoss(delta) * (Dictionary.safeGet(d, hl.getLo(), f) - Dictionary.safeGet(d, hl.getHi(), f));
				Dictionary.increment(derivFparamVec, f, del);
			}
			this.addLoss(LOSS.L2, this.loss(delta));
		}
		
		for (String f : trainableFeatures(derivFparamVec.keySet())) {
			derivFparamVec.put(f,derivFparamVec.get(f) / example.length());
			if (Double.isInfinite(derivFparamVec.get(f))) {
				throw new IllegalStateException("Asymptote error");
			}
		}
		return derivFparamVec;
	}
	

	public Map<String, Double> train(List<PairwiseRWExample> trainingExamples, ParamVector initialParamVec) {
		this.epoch = 0;
		ParamVector paramVec = initialParamVec;
		for (int i=0; i<NUM_EPOCHS; i++) {
			this.epoch++;
//			int ex=0;
			for (PairwiseRWExample example : trainingExamples) { //ex++;
				if (log.isDebugEnabled()) log.debug(String.format("epoch: %d length: %s train: %s",epoch, example.length(),example.toString()));
				trainOnExample(paramVec, example);
			}
			double trainLoss = averageLoss(paramVec,trainingExamples);
			if (log.isInfoEnabled()) log.info(String.format("epoch: %d trainLoss: %f",epoch, trainLoss));
		}
		return paramVec;
	}
	
	protected void addLoss(LOSS type, double loss) {
		Dictionary.increment(this.cumloss.loss, type, loss);
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
