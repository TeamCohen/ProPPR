package edu.cmu.ml.proppr.learn;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Test;

import edu.cmu.ml.proppr.learn.LocalL2SRW;
import edu.cmu.ml.proppr.util.MuParamVector;
import edu.cmu.ml.proppr.util.ParamVector;
import edu.cmu.ml.proppr.util.SimpleParamVector;

public class LocalL2PosNegLossSRWTest extends SRWTest {
	@Override
	public void initSrw() {
		srw = new LocalL2SRW();
	}
	@Override
	public ParamVector makeParams(Map<String,Double> foo) {
		return new MuParamVector(foo);
	}
	@Override
	public ParamVector makeParams() {
		return new MuParamVector();
	}
}
