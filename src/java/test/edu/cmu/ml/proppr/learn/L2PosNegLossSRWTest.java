package edu.cmu.ml.proppr.learn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.junit.Test;

import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.examples.PairwiseRWExample.HiLo;
import edu.cmu.ml.proppr.learn.L2PosNegLossTrainedSRW;
import edu.cmu.ml.proppr.learn.tools.SigmoidWeightingScheme;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.ParamVector;
import edu.cmu.ml.proppr.util.SimpleParamVector;

public class L2PosNegLossSRWTest extends SRWTest {
	public void initSrw() {
		srw = new L2PosNegLossTrainedSRW();
		srw.setMu(0);
		srw.setWeightingScheme(new SigmoidWeightingScheme());
	}
}
