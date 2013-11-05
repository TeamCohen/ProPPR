package edu.cmu.ml.praprolog.learn;

import static org.junit.Assert.*;

import java.util.Map;
import java.util.TreeMap;


import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;

import edu.cmu.ml.praprolog.RedBlueGraph;
import edu.cmu.ml.praprolog.graph.AnnotatedGraph;
import edu.cmu.ml.praprolog.graph.AnyKeyGraph;
import edu.cmu.ml.praprolog.learn.L2SqLossSRW;
import edu.cmu.ml.praprolog.learn.PairwiseRWExample;
import edu.cmu.ml.praprolog.learn.SRW;
import edu.cmu.ml.praprolog.learn.PairwiseRWExample.HiLo;
import edu.cmu.ml.praprolog.util.Dictionary;
/**
 * These tests are on a graph without reset links in it.
 * @author krivard
 *
 */
public class SRWTest extends RedBlueGraph {
	private static final Logger log = Logger.getLogger(SRWTest.class);
	protected SRW<PairwiseRWExample<String>> srw;
//	@Test @Ignore
//	public void setupTest() {
//		log.debug("\n"+brGraphs.get(0).graphVizDump());
//	}
	
	@Override
	public void setup() {
		super.setup();
		initSrw();
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
		
		TreeMap<String,Double> startVec = new TreeMap<String,Double>();
		startVec.put("r0",1.0);
		Map<String,Double> baseLineVec = myRWR(startVec,g,maxT);
		TreeMap<String,Double>uniformWeightVec = new TreeMap<String,Double>();
		uniformWeightVec.put("fromb",1.0);
		uniformWeightVec.put("tob",1.0);
		uniformWeightVec.put("fromr",1.0);
		uniformWeightVec.put("tor",1.0);
		uniformWeightVec.put("restart",1.0);
		Map<String,Double> newVec = srw.rwrUsingFeatures(g, startVec, uniformWeightVec);
		equalScores(baseLineVec,newVec);
	}
}