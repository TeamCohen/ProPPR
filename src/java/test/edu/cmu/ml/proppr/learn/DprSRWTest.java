package edu.cmu.ml.proppr.learn;

import static org.junit.Assert.*;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.junit.Test;

import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.learn.DprSRW;
import edu.cmu.ml.proppr.learn.ExampleFactory.DprExampleFactory;
import edu.cmu.ml.proppr.learn.tools.Exp;
import edu.cmu.ml.proppr.learn.tools.Linear;
import edu.cmu.ml.proppr.learn.tools.Sigmoid;
import edu.cmu.ml.proppr.learn.tools.Tanh;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.math.MuParamVector;
import edu.cmu.ml.proppr.util.math.ParamVector;
import edu.cmu.ml.proppr.util.math.SimpleParamVector;

public class DprSRWTest extends L2PosNegLossSRWTest {
	@Override
	public void initSrw() {
		srw = new DprSRW();
		factory = new DprExampleFactory();
	}
	
	@Override
	public void defaultSrwSettings() {
		super.defaultSrwSettings();
		srw.getOptions().set("apr","epsilon","1e-7");
	}
}
