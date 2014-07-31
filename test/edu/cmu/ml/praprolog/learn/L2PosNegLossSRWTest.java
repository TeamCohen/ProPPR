package edu.cmu.ml.praprolog.learn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.junit.Test;

import edu.cmu.ml.praprolog.learn.tools.PosNegRWExample;
import edu.cmu.ml.praprolog.learn.tools.PairwiseRWExample.HiLo;
import edu.cmu.ml.praprolog.util.Dictionary;
import edu.cmu.ml.praprolog.util.ParamVector;
import edu.cmu.ml.praprolog.util.SimpleParamVector;

public class L2PosNegLossSRWTest extends SRWTest {
	public void initSrw() {
		srw = new L2PosNegLossTrainedSRW();
		srw.setMu(0);
	}
	
	public ParamVector makeParams(Map<String,Double> foo) {
		return new SimpleParamVector(foo);
	}
	public ParamVector makeParams() {
		return new SimpleParamVector();
	}

	@Test
	public void testGradient() {
		ParamVector startVec = makeParams(new TreeMap<String,Double>());
		startVec.put("r0",1.0);

		ParamVector baseLineVec = makeParams(brGraphs.get(0).rwr(startVec));

		Set<String> pos = bluePart(baseLineVec).keySet();
		Set<String> neg = redPart(baseLineVec).keySet();

		ParamVector uniformWeightVec = makeParams(new TreeMap<String,Double>());
		String[] names = {"fromb","tob","fromr","tor"};
		for (String n : names) uniformWeightVec.put(n,1.0);

		L2PosNegLossTrainedSRW srw = (L2PosNegLossTrainedSRW) this.srw;
		double baselineLoss = srw.empiricalLoss(uniformWeightVec, new PosNegRWExample(brGraphs.get(0), startVec, pos,neg));

		ParamVector biasedWeightVec = makeParams(); biasedWeightVec.putAll(uniformWeightVec);
		biasedWeightVec.put("tob", 10.0);
		biasedWeightVec.put("tor", 0.1);
		double biasedLoss = srw.empiricalLoss(biasedWeightVec, new PosNegRWExample(brGraphs.get(0), startVec, pos,neg));

		assertTrue(String.format("baselineLoss %f should be > than biasedLoss %f",baselineLoss,biasedLoss),
				baselineLoss > biasedLoss);
		assertEquals("baselineLoss",6.25056697891,baselineLoss,1e-6);
		assertEquals("biasedLoss",5.6002602,biasedLoss,1e-6);
		//			assertEquals("biasedLoss",3.41579147351,biasedLoss,1e-6); <-- pre-sigmoid value

		Map<String,Double> gradient = srw.gradient(uniformWeightVec, new PosNegRWExample(brGraphs.get(0), startVec, pos,neg));
		System.err.println(Dictionary.buildString(gradient, new StringBuilder(), "\n").toString());

		assertTrue("gradient @ tob "+gradient.get("tob"),gradient.get("tob") < 0);
		assertTrue("gradient @ fromb "+gradient.get("fromb"),gradient.get("fromb") < 0);
		assertTrue("gradient @ tor "+gradient.get("tor"),gradient.get("tor") > 0);
		assertTrue("gradient @ fromr "+gradient.get("fromr"),gradient.get("fromr") > 0);

		double eps = .001;
		ParamVector nearlyUniformWeightVec = makeParams(new TreeMap<String,Double>());
		for (String f : gradient.keySet()) nearlyUniformWeightVec.put(f,1.0-eps*gradient.get(f));
		double improvedBaselineLoss = srw.empiricalLoss(nearlyUniformWeightVec, new PosNegRWExample(brGraphs.get(0), startVec, pos,neg));
		assertTrue("baselineLoss "+baselineLoss+" should be > improvedBaselineLoss "+improvedBaselineLoss, baselineLoss > improvedBaselineLoss);
	}

}
