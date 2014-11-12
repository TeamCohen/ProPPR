package edu.cmu.ml.proppr.learn;

import static org.junit.Assert.*;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.junit.Test;

import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.learn.AprSRW;
import edu.cmu.ml.proppr.learn.tools.ExpWeightingScheme;
import edu.cmu.ml.proppr.learn.tools.LinearWeightingScheme;
import edu.cmu.ml.proppr.learn.tools.SigmoidWeightingScheme;
import edu.cmu.ml.proppr.learn.tools.TanhWeightingScheme;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.MuParamVector;
import edu.cmu.ml.proppr.util.ParamVector;
import edu.cmu.ml.proppr.util.SimpleParamVector;

public class AprSRWTest extends L2PosNegLossSRWTest {
	@Override
	public void initSrw() {
		srw = new AprSRW(0.1,1e-5,0);
	}
	
	@Override
	public void defaultSrwSettings() {
		super.defaultSrwSettings();
		srw.setWeightingScheme(new ExpWeightingScheme());
	}
}
