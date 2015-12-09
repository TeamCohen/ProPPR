package edu.cmu.ml.proppr.learn;


import java.util.Map;

import edu.cmu.ml.proppr.util.math.MuParamVector;
import edu.cmu.ml.proppr.util.math.ParamVector;

public class LocalL2PosNegLossSRWTest extends SRWTest {
	@Override
	public void initSrw() {
		srw = new SRW();
		this.srw.setRegularizer(new LocalRegularizationSchedule(this.srw, new RegularizeL2()));
		this.srw.setLossFunction(new PosNegLoss());
	}
//	@Override
//	public ParamVector<String,?> makeParams(Map<String,Double> foo) {
//		return new MuParamVector(foo);
//	}
//	@Override
//	public ParamVector<String,?> makeParams() {
//		return new MuParamVector();
//	}
}
