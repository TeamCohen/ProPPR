package edu.cmu.ml.proppr.learn;

import static org.junit.Assert.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.junit.Test;

import edu.cmu.ml.proppr.RedBlueGraph;
import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.graph.ArrayLearningGraph;
import edu.cmu.ml.proppr.graph.LearningGraphBuilder;
import edu.cmu.ml.proppr.learn.SRW;
import edu.cmu.ml.proppr.learn.tools.ExpWeightingScheme;
import edu.cmu.ml.proppr.learn.tools.ReLUWeightingScheme;
import edu.cmu.ml.proppr.learn.tools.WeightingScheme;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.ParamVector;
import edu.cmu.ml.proppr.util.SimpleParamVector;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
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
	public void moreSetup(LearningGraphBuilder lgb) {
		initSrw();
		defaultSrwSettings();
		uniformParams = makeParams(new ConcurrentHashMap<String,Double>());
		for (String n : new String[] {"fromb","tob","fromr","tor"}) uniformParams.put(n,srw.getWeightingScheme().defaultWeight());
		startVec = new TIntDoubleHashMap();
		startVec.put(nodes.getId("r0"),1.0);
	}
	
	public void defaultSrwSettings() {
		srw.setMu(0);
		srw.getOptions().set("apr","alpha","0.1");
//		srw.setWeightingScheme(new LinearWeightingScheme());
//		srw.setWeightingScheme(new ReLUWeightingScheme());
		srw.setWeightingScheme(new ExpWeightingScheme());
	}
	
	public void initSrw() { 
		srw = new SRW(10);
	}
	
	public TIntDoubleMap myRWR(TIntDoubleMap startVec, ArrayLearningGraph g, int maxT) {
		TIntDoubleMap vec = startVec;
		TIntDoubleMap nextVec = null;
		for (int t=0; t<maxT; t++) {
			nextVec = new TIntDoubleHashMap();
			int k=-1;
			for (int u : vec.keys()) { k++;
				int z = g.node_near_hi[u] - g.node_near_lo[u];// near(u).size();
				for (int eid = g.node_near_lo[u]; eid<g.node_near_hi[u]; eid++) { //TIntIterator it = g.near(u).iterator(); it.hasNext(); ) {
					int v = g.edge_dest[eid];
					double inc = vec.get(u) / z;
					Dictionary.increment(nextVec, v, inc);
					log.debug("Incremented "+u+", "+v+" by "+inc);
				}
			}
			vec = nextVec;
		}
		return nextVec;
	}
	
	public TIntDoubleMap myRWR(TIntDoubleMap startVec, ArrayLearningGraph g, int maxT, ParamVector params, WeightingScheme scheme) {
		TIntDoubleMap vec = startVec;
		TIntDoubleMap nextVec = null;
		for (int t=0; t<maxT; t++) {
			nextVec = new TIntDoubleHashMap();
			int k=-1;
			for (int u : vec.keys()) { k++;
				// compute total edge weight:
				double z = 0.0;
				for (int eid = g.node_near_lo[u]; eid<g.node_near_hi[u]; eid++) {
					int v = g.edge_dest[eid];
					double suv = 0.0;
					for (int fid = g.edge_labels_lo[eid]; fid<g.edge_labels_hi[eid]; fid++) {
						suv += Dictionary.safeGet(params, (g.featureLibrary.getSymbol(g.label_feature_id[fid])), scheme.defaultWeight()) * g.label_feature_weight[fid];
					}
					double ew = scheme.edgeWeight(suv);
					z+=ew;
				}

				for (int eid = g.node_near_lo[u]; eid<g.node_near_hi[u]; eid++) {
					int v = g.edge_dest[eid];
					double suv = 0.0;
					for (int fid = g.edge_labels_lo[eid]; fid<g.edge_labels_hi[eid]; fid++) {
						suv += Dictionary.safeGet(params, (g.featureLibrary.getSymbol(g.label_feature_id[fid])), scheme.defaultWeight()) * g.label_feature_weight[fid];
					}
					double ew = scheme.edgeWeight(suv);
					double inc = vec.get(u) * ew / z;
					Dictionary.increment(nextVec, v, inc);
					log.debug("Incremented "+u+", "+v+" by "+inc);
				}
			}
			vec = nextVec;
		}
		return nextVec;
	}
	
//	 Test removed: We no longer compute rwr in SRW
//	
//	/**
//	 * Uniform weights should be the same as the unparameterized basic RWR
//	 */
//	@Test
//	public void testUniformRWR() {
//		log.debug("Test logging");
//		int maxT = 10;
//		
//		TIntDoubleMap baseLineVec = myRWR(startVec,brGraph,maxT);
//		uniformParams.put("id(restart)",srw.getWeightingScheme().defaultWeight());
//		TIntDoubleMap newVec = srw.rwrUsingFeatures(brGraph, startVec, uniformParams);
//		equalScores(baseLineVec,newVec);
//	}
	
	
	public ParamVector makeParams(Map<String,Double> foo) {
		return new SimpleParamVector(foo);
	}
	
	public ParamVector makeParams() {
		return new SimpleParamVector();
	}
	
	public TObjectDoubleMap<String> makeGradient(SRW srw, ParamVector paramVec, TIntDoubleMap query, int[] pos, int[] neg) {
		TObjectDoubleMap<String> grad = new TObjectDoubleHashMap<String>();
		srw.accumulateGradient(paramVec, new PosNegRWExample(brGraph, query, pos,neg), grad);
		return grad;
	}
	
	@Test
	public void testGradient() {
//		if (this.getClass().equals(SRWTest.class)) return;
		
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
		srw.clearLoss();
		srw.accumulateGradient(paramVec, new PosNegRWExample(brGraph, query, pos,neg), new TObjectDoubleHashMap<String>());
		return srw.cumulativeLoss().total();
	}
	public double makeLoss(ParamVector paramVec, PosNegRWExample example) {
		srw.clearLoss();
		srw.accumulateGradient(paramVec, example, new TObjectDoubleHashMap<String>());
		return srw.cumulativeLoss().total();
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
		TObjectDoubleMap<String> baselineGrad = makeGradient(srw, uniformParams, startVec, pos, neg);

		ParamVector biasedWeightVec = makeBiasedVec();
		double biasedLoss = makeLoss(srw, biasedWeightVec, startVec, pos, neg);

		assertTrue(String.format("baselineLoss %f should be > than biasedLoss %f",baselineLoss,biasedLoss),
				baselineLoss > biasedLoss);

		double perturb_epsilon = 1e-10;
		for (String feature : new String[]{"tob","fromb","tor","fromr"}) {
			
			ParamVector pert = uniformParams.copy();
			pert.put(feature, pert.get(feature)+perturb_epsilon);
			
			srw.clearLoss();
			TObjectDoubleMap<String> epsGrad = makeGradient(srw, pert, startVec, pos, neg);
			double newLoss = srw.cumulativeLoss().total();
			

			double truediff = (newLoss-baselineLoss);
			double approxdiff = (perturb_epsilon*baselineGrad.get(feature));
			double percdiff = (truediff) != 0 ? Math.abs(((approxdiff) / (truediff))-1)*100 : 0;
			System.err.println(String.format("%5s  true: %+1.8e  approx: %+1.8e  %%diff: %3.2f%%",
					feature,
					truediff,
					approxdiff, 
					percdiff));
			
			assertEquals("first order approximation on "+feature,
					0,
					percdiff,
					10);
		}
		
		double eps = .0001;
		ParamVector nearlyUniformWeightVec = uniformParams.copy();
//		TObjectDoubleMap<String> baselineGrad = makeGradient(srw, uniformParams, startVec, pos, neg);
		System.err.println("\nbaseline gradient:"+Dictionary.buildString(baselineGrad, new StringBuilder(), "\n").toString());
		for (String f : baselineGrad.keySet()) Dictionary.increment(nearlyUniformWeightVec,f,-eps*baselineGrad.get(f));
		System.err.println("\nimproved params:"+Dictionary.buildString(nearlyUniformWeightVec, new StringBuilder(), "\n").toString());
		srw.clearLoss();
		double improvedBaselineLoss = makeLoss(this.srw, nearlyUniformWeightVec, startVec, pos,neg);
		System.err.println("\nbaselineLoss-improvedBaselineLoss="+(baselineLoss-improvedBaselineLoss));
		assertTrue("baselineLoss "+baselineLoss+" should be > improvedBaselineLoss "+improvedBaselineLoss, baselineLoss > improvedBaselineLoss);
	}
	
	
	/**
	 * check that learning on red/blue graph works
	 */
	@Test
	public void testLearn1() {
		
		TIntDoubleMap query = new TIntDoubleHashMap();
		query.put(nodes.getId("r0"), 1.0);
		int[] pos = new int[blues.size()]; { int i=0; for (String k : blues) pos[i++] = nodes.getId(k); }
		int[] neg = new int[reds.size()];  { int i=0; for (String k : reds)  neg[i++] = nodes.getId(k); }
		PosNegRWExample example = new PosNegRWExample(brGraph, query, pos, neg);
		
//		ParamVector weightVec = new SimpleParamVector();
//		weightVec.put("fromb",1.01);
//		weightVec.put("tob",1.0);
//		weightVec.put("fromr",1.03);
//		weightVec.put("tor",1.0);
//		weightVec.put("id(restart)",1.02);
		
		ParamVector trainedParams = uniformParams.copy();
		double preLoss = makeLoss(trainedParams, example);
		srw.clearLoss();
		srw.trainOnExample(trainedParams,example);
		double postLoss = makeLoss(trainedParams, example);
		assertTrue(String.format("preloss %f >=? postloss %f",preLoss,postLoss), 
				preLoss == 0 || preLoss > postLoss);
	}
	
}