package edu.cmu.ml.praprolog.learn;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Test;

import edu.cmu.ml.praprolog.util.MuParamVector;
import edu.cmu.ml.praprolog.util.ParamVector;
import edu.cmu.ml.praprolog.util.SimpleParamVector;

public class AprSRWTest extends L2PosNegLossSRWTest {
	@Override
	public void initSrw() {
		srw = new AprSRW();
	}
	@Override
	public ParamVector makeParams(Map<String,Double> foo) {
		return new SimpleParamVector(foo);
	}
	@Override
	public ParamVector makeParams() {
		return new SimpleParamVector();
	}

}
