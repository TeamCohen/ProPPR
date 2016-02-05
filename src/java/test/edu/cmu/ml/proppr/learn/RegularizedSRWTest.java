package edu.cmu.ml.proppr.learn;

import static org.junit.Assert.*;

import org.junit.Test;

import edu.cmu.ml.proppr.learn.tools.LossData.LOSS;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.StatusLogger;
import edu.cmu.ml.proppr.util.math.SimpleParamVector;

public class RegularizedSRWTest extends SRWTest {
	public void initSrw() {
		srw = new SRW();
		this.srw.setRegularizer(new RegularizationSchedule(this.srw, new RegularizeL2()));
	}
	@Override
	public void defaultSrwSettings() {
		super.defaultSrwSettings();
		srw.setMu(0.002);
	}
	@Test
	public void testRegularizationLoss() {
		int[] pos = new int[blues.size()]; { int i=0; for (String k : blues) pos[i++] = nodes.getId(k); }
		int[] neg = new int[reds.size()];  { int i=0; for (String k : reds)  neg[i++] = nodes.getId(k); }

		srw.clearLoss();
		srw.accumulateGradient(makeBiasedVec(), factory.makeExample("loss",brGraph, startVec, pos,neg), new SimpleParamVector<String>(), new StatusLogger());
		System.out.println(Dictionary.buildString(srw.cumulativeLoss().loss, new StringBuilder(),"\n").toString());
		assertTrue(srw.cumulativeLoss().loss.get(LOSS.REGULARIZATION) > 0);
	}
}
