package edu.cmu.ml.praprolog.learn;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.junit.Test;

import edu.cmu.ml.praprolog.graph.AnnotatedGraph;
import edu.cmu.ml.praprolog.graph.Feature;
import edu.cmu.ml.praprolog.learn.L2SqLossSRW;
import edu.cmu.ml.praprolog.learn.SRW;
import edu.cmu.ml.praprolog.learn.tools.LossData;
import edu.cmu.ml.praprolog.learn.tools.PairwiseRWExample;
import edu.cmu.ml.praprolog.learn.tools.PairwiseRWExample.HiLo;
import edu.cmu.ml.praprolog.util.Dictionary;
import edu.cmu.ml.praprolog.util.ParamVector;
import edu.cmu.ml.praprolog.util.SimpleParamVector;

/**
 * These tests are on a graph which includes reset links.
 * @author krivard
 *
 */
public class SRWRestartTest extends SRWTest {
	public void initSrw() {
		srw = new L2SqLossSRW<String>();
	}
	public void setup(){
		super.setup();

		// add restart links to r0,r1,r2 each graph
		for (int k=0; k<magicNumber; k++) {
			String ri = "r"+k;
			AnnotatedGraph<String> g = brGraphs.get(k); //brGraph 0 resets to r0, brGraph 1 resets to r1, etc
			for (String u : g.getNodes()) {
				ArrayList<Feature> ff = new ArrayList<Feature>();
				ff.addAll(g.phi(u, ri));
				ff.add(new Feature("restart",this.srw.getWeightingScheme().defaultWeight()));
				g.addDirectedEdge(u, ri, ff);
			}
		}
		uniformParams.put("restart",this.srw.getWeightingScheme().defaultWeight());
	}
	@Override
	public Map<String,Double> makeGradient(SRW srw, ParamVector paramVec, ParamVector query, Set<String> pos, Set<String> neg) {
		List<HiLo> trainingPairs = new ArrayList<HiLo>();
		for (String p : pos) {
			for (String n : neg) {
				trainingPairs.add(new HiLo(p,n));
			}
		}
		return srw.gradient(paramVec, new PairwiseRWExample(brGraphs.get(0), query, trainingPairs));
	}
	
	@Override
	public double makeLoss(SRW srw, ParamVector paramVec, ParamVector query, Set<String> pos, Set<String> neg) {		
		List<HiLo> trainingPairs = new ArrayList<HiLo>();
		for (String p : pos) {
			for (String n : neg) {
				trainingPairs.add(new HiLo(p,n));
			}
		}
		return srw.empiricalLoss(paramVec, new PairwiseRWExample(brGraphs.get(0), query, trainingPairs));
	}
	
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
		ParamVector baseLineRwr = new SimpleParamVector(brGraphs.get(0).rwr(startVec));
		ParamVector biasedParams = makeBiasedVec();
		
		SRW<PairwiseRWExample<String>> mysrw = new SRW<PairwiseRWExample<String>>(maxT);
		Map<String,Double> newRwr = mysrw.rwrUsingFeatures(brGraphs.get(0), startVec, biasedParams);
		
		System.err.println(Dictionary.buildString(baseLineRwr, new StringBuilder(), "\n"));
		System.err.println(Dictionary.buildString(newRwr, new StringBuilder(), "\n"));
		
		lowerScores(bluePart(baseLineRwr),bluePart(newRwr));
		lowerScores(redPart(newRwr),redPart(baseLineRwr));
	}
	
	/**
	 * check that learning on red/blue graph works
	 */
	@Test
	public void testLearn1() {
		List<HiLo<String>> trainingPairs = new ArrayList<HiLo<String>>();
		for (String b : blues) {
			for (String r : reds) {
				trainingPairs.add(new HiLo<String>(b,r));
			}
		}
		
		PairwiseRWExample<String>[] examples = new PairwiseRWExample[magicNumber];
		for (int i=0;i<magicNumber;i++) {
			TreeMap<String,Double> xxFeatures = new TreeMap<String,Double>();
			xxFeatures.put("r"+i, 1.0);
			PairwiseRWExample<String> xx = new PairwiseRWExample<String>(brGraphs.get(i),xxFeatures,trainingPairs);
			examples[i] = xx;
		}
		
		ParamVector weightVec = new SimpleParamVector();
		weightVec.put("fromb",1.01);
		weightVec.put("tob",1.0);
		weightVec.put("fromr",1.03);
		weightVec.put("tor",1.0);
		weightVec.put("restart",1.02);
		
		for (int i=0; i<magicNumber; i++) {
			L2SqLossSRW<String> mysrw = (L2SqLossSRW<String>) this.srw;
			double preLoss = mysrw.empiricalLoss(weightVec, examples[i]);
			mysrw.trainOnExample(weightVec,examples[i]);
			double postLoss = mysrw.empiricalLoss(weightVec, examples[i]);
			assertTrue(String.format("preloss %f postloss %f",preLoss,postLoss), 
					preLoss == 0 || preLoss > postLoss);
		}
	}
}
