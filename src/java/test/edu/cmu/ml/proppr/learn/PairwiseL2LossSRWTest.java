package edu.cmu.ml.proppr.learn;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import edu.cmu.ml.proppr.learn.tools.LossData.LOSS;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.math.SimpleParamVector;

/**
 * Created by kavyasrinet on 10/4/15.
 */
public class PairwiseL2LossSRWTest extends SRWTest {
    public void initSrw() {
        srw = new SRW();
        this.srw.setRegularizer(new RegularizationSchedule(this.srw, new RegularizeL2()));
        this.srw.setLossFunction(new PairwiseL2SqLoss());
    }
    
	@Test
	public void testLogLoss() {
		int[] pos = new int[blues.size()]; { int i=0; for (String k : blues) pos[i++] = nodes.getId(k); }
		int[] neg = new int[reds.size()];  { int i=0; for (String k : reds)  neg[i++] = nodes.getId(k); }

		srw.clearLoss();
		srw.accumulateGradient(uniformParams, factory.makeExample("loss",brGraph, startVec, pos,neg), new SimpleParamVector<String>());
		System.out.println(Dictionary.buildString(srw.cumulativeLoss().loss, new StringBuilder(),"\n").toString());
		assertTrue("loss must be nonzero",srw.cumulativeLoss().total() - srw.cumulativeLoss().loss.get(LOSS.REGULARIZATION) > 0);
	}
    
}
