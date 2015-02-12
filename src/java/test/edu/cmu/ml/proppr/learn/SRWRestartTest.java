package edu.cmu.ml.proppr.learn;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.junit.Test;

import edu.cmu.ml.proppr.examples.PairwiseRWExample;
import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.examples.PairwiseRWExample.HiLo;
import edu.cmu.ml.proppr.graph.ArrayLearningGraph;
import edu.cmu.ml.proppr.graph.ArrayLearningGraph.ArrayLearningGraphBuilder;
import edu.cmu.ml.proppr.graph.LearningGraphBuilder;
import edu.cmu.ml.proppr.graph.RWOutlink;
import edu.cmu.ml.proppr.learn.SRW;
import edu.cmu.ml.proppr.learn.tools.LossData;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.ParamVector;
import edu.cmu.ml.proppr.util.SimpleParamVector;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;

/**
 * These tests are on a graph which includes reset links.
 * @author krivard
 *
 */
public class SRWRestartTest extends SRWTest {
	public void initSrw() {
		srw = new L2SRW();
		srw.setAlpha(0.01);
	}
	
	
	@Override
	public void moreSetup(LearningGraphBuilder lgb) {
		super.moreSetup(lgb);
		// add restart links to r0
		for (int u : brGraph.getNodes()) {
			TObjectDoubleMap<String> ff = new TObjectDoubleHashMap<String>();
			ArrayLearningGraph.ArrayLearningGraphBuilder b = ((ArrayLearningGraph.ArrayLearningGraphBuilder) lgb);
			int r0 = nodes.getId("r0");
			RWOutlink outlinkR0 = null;
			if (b.outlinks[u] != null) {
				for (RWOutlink o : b.outlinks[u]) {
					if (o.nodeid == r0) {
						outlinkR0 = o;
						break;
					}
				}
			}
			if (outlinkR0 == null) {
				outlinkR0 = new RWOutlink(new TObjectDoubleHashMap<String>(), r0);
				lgb.addOutlink(brGraph, u, outlinkR0);
			}
			outlinkR0.fd.put("id(restart)",this.srw.getWeightingScheme().defaultWeight());
			lgb.addOutlink(brGraph, u, null); // cheat the label count
		}
		uniformParams.put("id(restart)",this.srw.getWeightingScheme().defaultWeight());
	}
//	@Override
//	public TObjectDoubleMap<String> makeGradient(SRW srw, ParamVector paramVec, TIntDoubleMap query, int[] pos, int[] neg) {
//		List<HiLo> trainingPairs = new ArrayList<HiLo>();
//		for (int p : pos) {
//			for (int n : neg) {
//				trainingPairs.add(new HiLo(p,n));
//			}
//		}
//		return srw.gradient(paramVec, new PairwiseRWExample(brGraph, query, trainingPairs));
//	}
	
//	@Override
//	public double makeLoss(SRW srw, ParamVector paramVec, TIntDoubleMap query, int[] pos, int[] neg) {		
//		List<HiLo> trainingPairs = new ArrayList<HiLo>();
//		for (int p : pos) {
//			for (int n : neg) {
//				trainingPairs.add(new HiLo(p,n));
//			}
//		}
//		return srw.empiricalLoss(paramVec, new PairwiseRWExample(brGraph, query, trainingPairs));
//	}
	
//	@Override
//	public void testUniformRWR() {}
	

	/**
	 * Biasing the params toward blue things should give the blue nodes a higher score.
	 */
	@Test
	public void testBiasedRWR() {
		int maxT = 10;
		
//		Map<String,Double> startVec = new TreeMap<String,Double>();
//		startVec.put("r0",1.0);
//		SRW<PairwiseRWExample> mysrw = new SRW<PairwiseRWExample>(maxT);
//		mysrw.setAlpha(0.01);
		TIntDoubleMap baseLineRwr = myRWR(startVec, brGraph, maxT, new SimpleParamVector<String>(), srw.getWeightingScheme());
		ParamVector biasedParams = makeBiasedVec();
		
		TIntDoubleMap newRwr = myRWR(startVec, brGraph, maxT, biasedParams, srw.getWeightingScheme());
		
		System.err.println("baseline:");
		for (int node : baseLineRwr.keys()) System.err.println(node+"/"+nodes.getSymbol(node)+":"+baseLineRwr.get(node));
		System.err.println("biased:");
		for (int node : newRwr.keys()) System.err.println(node+"/"+nodes.getSymbol(node)+":"+newRwr.get(node));
		
		lowerScores(bluePart(baseLineRwr),bluePart(newRwr));
		lowerScores(redPart(newRwr),redPart(baseLineRwr));
	}
	
	/**
	 * check that learning on red/blue graph works
	 */
	@Test
	public void testLearn1() {
		List<HiLo> trainingPairs = new ArrayList<HiLo>();
		for (String b : blues) {
			for (String r : reds) {
				trainingPairs.add(new HiLo(nodes.getId(b),nodes.getId(r)));
			}
		}
		
		TIntDoubleMap xxFeatures = new TIntDoubleHashMap();
		xxFeatures.put(nodes.getId("r0"), 1.0);
		int[] pos = new int[blues.size()]; { int i=0; for (String k : blues) pos[i++] = nodes.getId(k); }
		int[] neg = new int[reds.size()];  { int i=0; for (String k : reds)  neg[i++] = nodes.getId(k); }
		PosNegRWExample example = new PosNegRWExample(brGraph, xxFeatures, pos, neg);
		
//		ParamVector weightVec = new SimpleParamVector();
//		weightVec.put("fromb",1.01);
//		weightVec.put("tob",1.0);
//		weightVec.put("fromr",1.03);
//		weightVec.put("tor",1.0);
//		weightVec.put("id(restart)",1.02);
		
		ParamVector trainedParams = uniformParams.copy();
		double preLoss = makeLoss(trainedParams, example);
		srw.trainOnExample(trainedParams,example);
		double postLoss = makeLoss(trainedParams, example);
		assertTrue(String.format("preloss %f >=? postloss %f",preLoss,postLoss), 
				preLoss == 0 || preLoss > postLoss);
	}
}
