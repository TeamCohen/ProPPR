package edu.cmu.ml.proppr.learn;

/**
 * Created by kavyasrinet on 10/4/15.
 */
public class PairwiseL2LossSRWTest extends SRWTest {
    public void initSrw() {
        srw = new SRW();
        this.srw.setRegularizer(new RegularizationSchedule(this.srw, new RegularizeL2()));
        this.srw.setLossFunction(new PairwiseL2SqLoss());
    }
}
