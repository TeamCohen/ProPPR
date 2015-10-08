package edu.cmu.ml.proppr.learn;

import edu.cmu.ml.proppr.learn.RegularizeL2;

public class L2PosNegLossSRWTest extends SRWTest {
	public void initSrw() {
		srw = new SRW();
		this.srw.setRegularizer(new RegularizationSchedule(this.srw, new RegularizeL2()));
		this.srw.setLossFunction(new PosNegLoss());
	}
}
