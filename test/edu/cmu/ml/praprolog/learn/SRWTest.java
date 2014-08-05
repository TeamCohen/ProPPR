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
import edu.cmu.ml.praprolog.graph.AnnotatedGraph;
import edu.cmu.ml.praprolog.graph.AnyKeyGraph;
import edu.cmu.ml.praprolog.learn.L2SqLossSRW;
import edu.cmu.ml.praprolog.learn.SRW;
import edu.cmu.ml.praprolog.learn.tools.LinearWeightingScheme;
import edu.cmu.ml.praprolog.learn.tools.PairwiseRWExample;
import edu.cmu.ml.praprolog.learn.tools.PosNegRWExample;
import edu.cmu.ml.praprolog.learn.tools.PairwiseRWExample.HiLo;
import edu.cmu.ml.praprolog.learn.tools.RWExample;
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
	protected ParamVector uniformWeightVec, startVec;
//	@Test @Ignore
//	public void setupTest() {
//		log.debug("\n"+brGraphs.get(0).graphVizDump());
//	}
	
	@Override
	public void setup() {
		super.setup();
		initSrw();
		defaultSrwSettings();
		uniformWeightVec = makeParams(new TreeMap<String,Double>());
		for (String n : new String[] {"fromb","tob","fromr","tor"}) uniformWeightVec.put(n,1.0);
		startVec = makeParams(new TreeMap<String,Double>());
		startVec.put("r0",1.0);
	}
	
	public void defaultSrwSettings() {
		srw.setMu(0);
		srw.setWeightingScheme(new LinearWeightingScheme());
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
		uniformWeightVec.put("restart",1.0);
		Map<String,Double> newVec = srw.rwrUsingFeatures(g, startVec, uniformWeightVec);
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

		Map<String,Double> gradient = makeGradient(srw, uniformWeightVec, startVec, pos, neg);
		System.err.println(Dictionary.buildString(gradient, new StringBuilder(), "\n").toString());

		assertTrue("gradient @ tob "+gradient.get("tob"),gradient.get("tob") < 0);
		assertTrue("gradient @ tor "+gradient.get("tor"),gradient.get("tor") > 0);
		assertTrue("gradient @ fromb "+gradient.get("fromb"),gradient.get("fromb")<1e-10);
		assertTrue("gradient @ fromr "+gradient.get("fromr"),gradient.get("fromr")>-1e-10);
	}
	
	public double makeLoss(SRW srw, ParamVector paramVec, ParamVector query, Set<String> pos, Set<String> neg) {
		return srw.empiricalLoss(paramVec, new PosNegRWExample(brGraphs.get(0), query, pos,neg));
	}
	
	@Test
	public void testLoss() {
		if (this.getClass().equals(SRWTest.class)) return;
		
		ParamVector baseLineVec = makeParams(brGraphs.get(0).rwr(startVec));
		
		Set<String> pos = bluePart(baseLineVec).keySet();
		Set<String> neg = redPart(baseLineVec).keySet();

		double baselineLoss = makeLoss(this.srw, uniformWeightVec, startVec, pos, neg);

		ParamVector biasedWeightVec = makeParams(); biasedWeightVec.putAll(uniformWeightVec);
		biasedWeightVec.put("tob", 10.0);
		biasedWeightVec.put("tor", 0.1);
		double biasedLoss = makeLoss(srw, biasedWeightVec, startVec, pos, neg);

		assertTrue(String.format("baselineLoss %f should be > than biasedLoss %f",baselineLoss,biasedLoss),
				baselineLoss > biasedLoss);
//		assertEquals("baselineLoss\n",6.25056697891,baselineLoss,1e-6);
//		assertEquals("biasedLoss\n",5.6002602,biasedLoss,1e-6);

		double origLoss = makeLoss(srw, uniformWeightVec, startVec, pos, neg);
		
		ParamVector pert = uniformWeightVec.copy();
		pert.put("fromb", pert.get("fromb")+1e-10);
		srw.clearLoss();
		Map<String,Double> epsGrad = makeGradient(srw, pert, startVec, pos, neg);// srw.gradient(pert, new PosNegRWExample(brGraphs.get(0), startVec, pos,neg));
		double newLoss = srw.cumulativeLoss().total();
		System.err.println(Dictionary.buildString(epsGrad, new StringBuilder(), "\n").toString());
		
//		System.err.println("old loss:" +origLoss+" new loss: "+newLoss);
//		System.err.println("difference: "+(newLoss - origLoss));
		assertEquals("first order approximation",0,newLoss-origLoss,1e-15);
		
		double eps = .001;
		ParamVector nearlyUniformWeightVec = makeParams(new TreeMap<String,Double>());
		Map<String,Double> gradient = makeGradient(srw, uniformWeightVec, startVec, pos, neg);
		for (String f : gradient.keySet()) nearlyUniformWeightVec.put(f,1.0-eps*gradient.get(f));
		double improvedBaselineLoss = makeLoss(this.srw, nearlyUniformWeightVec, startVec, pos,neg);
		assertTrue("baselineLoss "+baselineLoss+" should be > improvedBaselineLoss "+improvedBaselineLoss, baselineLoss > improvedBaselineLoss);
	}
}