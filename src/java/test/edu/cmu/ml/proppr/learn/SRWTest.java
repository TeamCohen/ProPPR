package edu.cmu.ml.proppr.learn;

import static org.junit.Assert.*;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;


import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import edu.cmu.ml.proppr.RedBlueGraph;
import edu.cmu.ml.proppr.examples.PairwiseRWExample;
import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.examples.RWExample;
import edu.cmu.ml.proppr.examples.PairwiseRWExample.HiLo;
import edu.cmu.ml.proppr.graph.LearningGraph;
import edu.cmu.ml.proppr.learn.L2SqLossSRW;
import edu.cmu.ml.proppr.learn.SRW;
import edu.cmu.ml.proppr.learn.tools.ExpWeightingScheme;
import edu.cmu.ml.proppr.learn.tools.LinearWeightingScheme;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.ParamVector;
import edu.cmu.ml.proppr.util.SimpleParamVector;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
/**
 * These tests are on a graph without reset links in it.
 * @author krivard
 *
 */
public class SRWTest extends RedBlueGraph {
	private static final Logger log = Logger.getLogger(SRWTest.class);
	protected SRW srw;
	protected ParamVector uniformParams;
	protected TIntDoubleMap startVec;
	
	@Override
	public void setup() {
		super.setup();
		initSrw();
		defaultSrwSettings();
		uniformParams = makeParams(new TreeMap<String,Double>());
		for (String n : new String[] {"fromb","tob","fromr","tor"}) uniformParams.put(n,srw.getWeightingScheme().defaultWeight());
		startVec = new TIntDoubleHashMap();
		startVec.put(nodes.getId("r0"),1.0);
	}
	
	public void defaultSrwSettings() {
		srw.setMu(0);
//		srw.setWeightingScheme(new LinearWeightingScheme());
		srw.setWeightingScheme(new ExpWeightingScheme());
	}
	
	public void initSrw() { 
		srw = new SRW<PairwiseRWExample>(10);
	}
	
	public TIntDoubleMap myRWR(TIntDoubleMap startVec, LearningGraph g, int maxT) {
		TIntDoubleMap vec = startVec;
		TIntDoubleMap nextVec = null;
		for (int t=0; t<maxT; t++) {
			nextVec = new TIntDoubleHashMap();
			int k=-1;
			for (int u : vec.keys()) { k++;
				int z = g.near(u).size();
				for (TIntIterator it = g.near(u).iterator(); it.hasNext(); ) {
					int v = it.next();
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
		
		TIntDoubleMap baseLineVec = myRWR(startVec,brGraph,maxT);
		uniformParams.put("id(restart)",srw.getWeightingScheme().defaultWeight());
		TIntDoubleMap newVec = srw.rwrUsingFeatures(brGraph, startVec, uniformParams);
		equalScores(baseLineVec,newVec);
	}
	
	
	public ParamVector makeParams(Map<String,Double> foo) {
		return new SimpleParamVector(foo);
	}
	
	public ParamVector makeParams() {
		return new SimpleParamVector();
	}
	
	public TObjectDoubleMap<String> makeGradient(SRW srw, ParamVector paramVec, TIntDoubleMap query, int[] pos, int[] neg) {
		return srw.gradient(paramVec, new PosNegRWExample(brGraph, query, pos,neg));
	}
	
	@Test
	public void testGradient() {
		if (this.getClass().equals(SRWTest.class)) return;
		
		int[] pos = new int[blues.size()]; { int i=0; for (String k : blues) pos[i++] = nodes.getId(k); }
		int[] neg = new int[reds.size()];  { int i=0; for (String k : reds)  neg[i++] = nodes.getId(k); }

		TObjectDoubleMap<String> gradient = makeGradient(srw, uniformParams, startVec, pos, neg);
		System.err.println(Dictionary.buildString(gradient, new StringBuilder(), "\n").toString());

		// to favor blue (positive label) nodes,
		// we want the gradient to go downhill (negative) 
		// toward blue nodes (edges labeled 'tob')
		assertTrue("gradient @ tob "+gradient.get("tob"),gradient.get("tob") < 0);
		assertTrue("gradient @ tor "+gradient.get("tor"),gradient.get("tor") > 0);
		// we don't really care what happens on edges coming *from* blue nodes though:
//		assertTrue("gradient @ fromb "+gradient.get("fromb"),gradient.get("fromb")<1e-10);
//		assertTrue("gradient @ fromr "+gradient.get("fromr"),gradient.get("fromr")>-1e-10);
	}
	
	public double makeLoss(SRW srw, ParamVector paramVec, TIntDoubleMap query, int[] pos, int[] neg) {
		return srw.empiricalLoss(paramVec, new PosNegRWExample(brGraph, query, pos,neg));
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

		int[] pos = new int[blues.size()]; { int i=0; for (String k : blues) pos[i++] = nodes.getId(k); }
		int[] neg = new int[reds.size()];  { int i=0; for (String k : reds)  neg[i++] = nodes.getId(k); }

		double baselineLoss = makeLoss(this.srw, uniformParams, startVec, pos, neg);

		ParamVector biasedWeightVec = makeBiasedVec();
		double biasedLoss = makeLoss(srw, biasedWeightVec, startVec, pos, neg);

		assertTrue(String.format("baselineLoss %f should be > than biasedLoss %f",baselineLoss,biasedLoss),
				baselineLoss > biasedLoss);

		double origLoss = makeLoss(srw, uniformParams, startVec, pos, neg);
		
		for (String feature : new String[]{"tob","fromb","tor","fromr"}) {
			ParamVector pert = uniformParams.copy();
			pert.put("fromb", pert.get(feature)+1e-10);
			srw.clearLoss();
			TObjectDoubleMap<String> epsGrad = makeGradient(srw, pert, startVec, pos, neg);
			double newLoss = srw.cumulativeLoss().total();
			System.err.println("\n1st-order on "+feature+":"+Dictionary.buildString(epsGrad, new StringBuilder(), "\n").toString());
			
			assertEquals("first order approximation on "+feature,0,newLoss-origLoss,1e-15);
		}
		
		double eps = .0001;
		ParamVector nearlyUniformWeightVec = makeParams(new TreeMap<String,Double>());
		TObjectDoubleMap<String> gradient = makeGradient(srw, uniformParams, startVec, pos, neg);
		System.err.println("\nbaseline gradient:"+Dictionary.buildString(gradient, new StringBuilder(), "\n").toString());
		for (String f : gradient.keySet()) nearlyUniformWeightVec.put(f,1.0-eps*gradient.get(f));
		System.err.println("\nimproved params:"+Dictionary.buildString(nearlyUniformWeightVec, new StringBuilder(), "\n").toString());
		double improvedBaselineLoss = makeLoss(this.srw, nearlyUniformWeightVec, startVec, pos,neg);
		System.err.println("\nbaselineLoss-improvedBaselineLoss="+(baselineLoss-improvedBaselineLoss));
		assertTrue("baselineLoss "+baselineLoss+" should be > improvedBaselineLoss "+improvedBaselineLoss, baselineLoss > improvedBaselineLoss);
	}
}