package edu.cmu.ml.proppr.learn;

import edu.cmu.ml.proppr.learn.RegularizeL2;

public class L2PosNegLossSRWTest extends SRWTest {
	public void initSrw() {
		srw = new RegularizeL2();
	}
}
