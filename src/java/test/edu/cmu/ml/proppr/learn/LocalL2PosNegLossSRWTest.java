package edu.cmu.ml.proppr.learn;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Test;

import edu.cmu.ml.proppr.learn.LocalL2SRW;
import edu.cmu.ml.proppr.util.math.MuParamVector;
import edu.cmu.ml.proppr.util.math.ParamVector;
import edu.cmu.ml.proppr.util.math.SimpleParamVector;

public class LocalL2PosNegLossSRWTest extends SRWTest {
	@Override
	public void initSrw() {
		srw = new LocalL2SRW();
	}
	@Override
	public ParamVector<String,?> makeParams(Map<String,Double> foo) {
		return new MuParamVector(foo);
	}
	@Override
	public ParamVector<String,?> makeParams() {
		return new MuParamVector();
	}
}
