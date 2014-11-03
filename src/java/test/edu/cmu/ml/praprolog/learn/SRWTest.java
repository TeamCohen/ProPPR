package edu.cmu.ml.praprolog.learn;

import static org.junit.Assert.*;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;


import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import edu.cmu.ml.praprolog.RedBlueGraph;
import edu.cmu.ml.praprolog.examples.PairwiseRWExample;
import edu.cmu.ml.praprolog.examples.PosNegRWExample;
import edu.cmu.ml.praprolog.examples.RWExample;
import edu.cmu.ml.praprolog.examples.PairwiseRWExample.HiLo;
import edu.cmu.ml.praprolog.graph.v1.AnnotatedGraph;
import edu.cmu.ml.praprolog.graph.v1.AnyKeyGraph;
import edu.cmu.ml.praprolog.learn.L2SqLossSRW;
import edu.cmu.ml.praprolog.learn.SRW;
import edu.cmu.ml.praprolog.learn.tools.ExpWeightingScheme;
import edu.cmu.ml.praprolog.learn.tools.LinearWeightingScheme;
import edu.cmu.ml.praprolog.util.Dictionary;
import edu.cmu.ml.praprolog.util.ParamVector;
import edu.cmu.ml.praprolog.util.SimpleParamVector;
/**
 * These tests are on a graph without reset links in it.
 * @author krivard
 *
 */
public class SRWTest extends RedBlueGraph {
	private static final Logger log = Logger.getLogger(SRWTest.class);
	protected SRW<PairwiseRWExample<String>> srw;
	protected ParamVector uniformParams, startVec;
	
	@Override
	public void setup() {
		super.setup();
		initSrw();
		defaultSrwSettings();
		uniformParams = makeParams(new TreeMap<String,Double>());
		for (String n : new String[] {"fromb","tob","fromr","tor"}) uniformParams.put(n,srw.getWeightingScheme().defaultWeight());
		startVec = makeParams(new TreeMap<String,Double>());
		startVec.put("r0",1.0);
	}
	
	public void defaultSrwSettings() {
		srw.setMu(0);
//		srw.setWeightingScheme(new LinearWeightingScheme());
		srw.setWeightingScheme(new ExpWeightingScheme());
	}
	
	public void initSrw() { 
		srw = new SRW<PairwiseRWExample<String>>(10);
	}
	
	public Map<String,Double> myRWR(Map<String,Double> startVec, AnyKeyGraph<String> g, int maxT) {
		Map<String,Double> vec = startVec;
		Map<String,Double> nextVec = null;
		for (int t=0; t<maxT; t++) {
			nextVec = new TreeMap<String,Double>();
			int k=-1;
			for (String u : vec.keySet()) { k++;
				int z = g.nearNative(u).size();
				for (String v : g.nearNative(u).keySet()) {
					double inc = vec.get(u) / z;
					Dictionary.increment(nextVec, v, inc);
					log.debug("Incremented "+u+", "+v+" by "+inc);
				}
			}
			vec = nextVec;
		}
		return nextVec;
	}
	
	/**
	 * Uniform weights should be the same as the unparameterized basic RWR
	 */
	@Test
	public void testUniformRWR() {
		log.debug("Test logging");
		int maxT = 10;
		
		AnnotatedGraph<String> g = brGraphs.get(0);
		Map<String,Double> baseLineVec = myRWR(startVec,g,maxT);
		uniformParams.put("restart",srw.getWeightingScheme().defaultWeight());
		Map<String,Double> newVec = srw.rwrUsingFeatures(g, startVec, uniformParams);
		equalScores(baseLineVec,newVec);
	}
	
	
	public ParamVector makeParams(Map<String,Double> foo) {
		return new SimpleParamVector(foo);
	}
	
	public ParamVector makeParams() {
		return new SimpleParamVector();
	}
	
	public Map<String,Double> makeGradient(SRW srw, ParamVector paramVec, ParamVector query, Set<String> pos, Set<String> neg) {
		return srw.gradient(paramVec, new PosNegRWExample(brGraphs.get(0), query, pos,neg));
	}
	
	@Test
	public void testGradient() {
		if (this.getClass().equals(SRWTest.class)) return;
		
		ParamVector baseLineVec = makeParams(brGraphs.get(0).rwr(startVec));

		Set<String> pos = bluePart(baseLineVec).keySet();
		Set<String> neg = redPart(baseLineVec).keySet();

		Map<String,Double> gradient = makeGradient(srw, uniformParams, startVec, pos, neg);
		System.err.println(Dictionary.buildString(gradient, new StringBuilder(), "\n").toString());

		assertTrue("gradient @ tob "+gradient.get("tob"),gradient.get("tob") < 0);
		assertTrue("gradient @ tor "+gradient.get("tor"),gradient.get("tor") > 0);
		assertTrue("gradient @ fromb "+gradient.get("fromb"),gradient.get("fromb")<1e-10);
		assertTrue("gradient @ fromr "+gradient.get("fromr"),gradient.get("fromr")>-1e-10);
	}
	
	public double makeLoss(SRW srw, ParamVector paramVec, ParamVector query, Set<String> pos, Set<String> neg) {
		return srw.empiricalLoss(paramVec, new PosNegRWExample(brGraphs.get(0), query, pos,neg));
	}
	
	protected ParamVector makeBiasedVec() {
		return makeBiasedVec(new String[] {"tob"}, new String[] {"tor"});
	}
	protected ParamVector makeBiasedVec(String[] upFeatures, String[] downFeatures) {
		ParamVector biasedWeightVec = makeParams(); biasedWeightVec.putAll(uniformParams);
		if (biasedWeightVec.get(upFeatures[0]).equals(1.0)) {
			for (String f : upFeatures)
				biasedWeightVec.put(f, 10.0);
			for (String f : downFeatures)
				biasedWeightVec.put(f, 0.1);
		} else {			
			for (String f : upFeatures)
				biasedWeightVec.put(f, 1.0);
			for (String f : downFeatures)
				biasedWeightVec.put(f, -1.0);
		}
		return biasedWeightVec;
	}
	
	@Test
	public void testLoss() {
		if (this.getClass().equals(SRWTest.class)) return;
		
		ParamVector baseLineVec = makeParams(brGraphs.get(0).rwr(startVec));
		
		Set<String> pos = bluePart(baseLineVec).keySet();
		Set<String> neg = redPart(baseLineVec).keySet();

		double baselineLoss = makeLoss(this.srw, uniformParams, startVec, pos, neg);

//		ParamVector biasedWeightVec = makeParams(); biasedWeightVec.putAll(uniformWeightVec);
//		if (biasedWeightVec.get("tob").equals(1.0)) {
//			biasedWeightVec.put("tob", 10.0);
//			biasedWeightVec.put("tor", 0.1);
//		} else {
//			biasedWeightVec.put("tob", 1.0);
//			biasedWeightVec.put("tor", -1.0);
//		}
		ParamVector biasedWeightVec = makeBiasedVec();
		double biasedLoss = makeLoss(srw, biasedWeightVec, startVec, pos, neg);

		assertTrue(String.format("baselineLoss %f should be > than biasedLoss %f",baselineLoss,biasedLoss),
				baselineLoss > biasedLoss);
//		assertEquals("baselineLoss\n",6.25056697891,baselineLoss,1e-6);
//		assertEquals("biasedLoss\n",5.6002602,biasedLoss,1e-6);

		double origLoss = makeLoss(srw, uniformParams, startVec, pos, neg);
		
		for (String feature : new String[]{"tob","fromb","tor","fromr"}) {
			ParamVector pert = uniformParams.copy();
			pert.put("fromb", pert.get(feature)+1e-10);
			srw.clearLoss();
			Map<String,Double> epsGrad = makeGradient(srw, pert, startVec, pos, neg);
			double newLoss = srw.cumulativeLoss().total();
			//System.err.println("\n1st-order on "+feature+":"+Dictionary.buildString(epsGrad, new StringBuilder(), "\n").toString());
			
			assertEquals("first order approximation on "+feature,0,newLoss-origLoss,1e-15);
		}
		
		double eps = .0001;
		ParamVector nearlyUniformWeightVec = makeParams(new TreeMap<String,Double>());
		Map<String,Double> gradient = makeGradient(srw, uniformParams, startVec, pos, neg);
		System.err.println("\nbaseline gradient:"+Dictionary.buildString(gradient, new StringBuilder(), "\n").toString());
		for (String f : gradient.keySet()) nearlyUniformWeightVec.put(f,1.0-eps*gradient.get(f));
		System.err.println("\nimproved params:"+Dictionary.buildString(nearlyUniformWeightVec, new StringBuilder(), "\n").toString());
		double improvedBaselineLoss = makeLoss(this.srw, nearlyUniformWeightVec, startVec, pos,neg);
		System.err.println("\nbaselineLoss-improvedBaselineLoss="+(baselineLoss-improvedBaselineLoss));
		assertTrue("baselineLoss "+baselineLoss+" should be > improvedBaselineLoss "+improvedBaselineLoss, baselineLoss > improvedBaselineLoss);
	}
}