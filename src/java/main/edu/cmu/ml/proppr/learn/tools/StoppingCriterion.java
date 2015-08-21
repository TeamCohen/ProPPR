package edu.cmu.ml.proppr.learn.tools;

import org.apache.log4j.Logger;

public class StoppingCriterion {
	public static final int DEFAULT_MIN_STABLE_EPOCHS = 3;
	public static final double DEFAULT_MAX_PCT_IMPROVEMENT = 1.0;
	private static final Logger log = Logger.getLogger(StoppingCriterion.class);
	/** Stop when the percentage improvement in loss has been no more
	 * than maxPctImprovementInLoss for minStableEpochs in a row, or
	 * when at least maxEpochs have occurred.
	 */
	public double maxPctImprovementInLoss;
	public int minStableEpochs;
	public int numConseqStableEpochs;
	public int maxEpochs;
	public int numEpochs;

	public StoppingCriterion(int maxEpochs,double maxPctImprovementInLoss, int minStableEpochs) {
		this.maxPctImprovementInLoss = maxPctImprovementInLoss;
		this.minStableEpochs = minStableEpochs;
		this.numConseqStableEpochs = 0;
		this.maxEpochs = maxEpochs;
		this.numEpochs = 0;
	}
	public StoppingCriterion(int maxEpochs) {
		this(maxEpochs, DEFAULT_MAX_PCT_IMPROVEMENT, DEFAULT_MIN_STABLE_EPOCHS);
	}
	public void recordEpoch() {
		numEpochs++;
	}

	public void recordConsecutiveLosses(LossData lossThisEpoch,LossData lossLastEpoch) {
		LossData diff = lossLastEpoch.diff(lossThisEpoch);
		double percentImprovement = 100 * diff.total()/lossThisEpoch.total();
		if (percentImprovement > maxPctImprovementInLoss) {
			numConseqStableEpochs = 0;				
		} else {
			numConseqStableEpochs++;
		}
	}
	public boolean satisified() {
		boolean converged = numConseqStableEpochs >= minStableEpochs;
		return converged || (numEpochs>maxEpochs);
	}
}
