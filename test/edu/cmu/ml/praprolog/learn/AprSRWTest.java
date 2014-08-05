package edu.cmu.ml.praprolog.learn;

import static org.junit.Assert.*;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.junit.Test;

import edu.cmu.ml.praprolog.learn.tools.ExpWeightingScheme;
import edu.cmu.ml.praprolog.learn.tools.LinearWeightingScheme;
import edu.cmu.ml.praprolog.learn.tools.PosNegRWExample;
import edu.cmu.ml.praprolog.learn.tools.SigmoidWeightingScheme;
import edu.cmu.ml.praprolog.learn.tools.TanhWeightingScheme;
import edu.cmu.ml.praprolog.util.Dictionary;
import edu.cmu.ml.praprolog.util.MuParamVector;
import edu.cmu.ml.praprolog.util.ParamVector;
import edu.cmu.ml.praprolog.util.SimpleParamVector;

public class AprSRWTest extends L2PosNegLossSRWTest {
	@Override
	public void initSrw() {
		srw = new AprSRW();
	}
	
	@Override
	public void defaultSrwSettings() {
		super.defaultSrwSettings();
		srw.setWeightingScheme(new SigmoidWeightingScheme());
	}
}
