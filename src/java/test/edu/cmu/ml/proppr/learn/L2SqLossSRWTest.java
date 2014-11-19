package edu.cmu.ml.proppr.learn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.junit.Test;

import edu.cmu.ml.proppr.examples.PairwiseRWExample;
import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.examples.PairwiseRWExample.HiLo;
import edu.cmu.ml.proppr.graph.LearningGraph;
import edu.cmu.ml.proppr.learn.L2SqLossSRW;
import edu.cmu.ml.proppr.learn.SRW;
import edu.cmu.ml.proppr.learn.tools.LinearWeightingScheme;
import edu.cmu.ml.proppr.learn.tools.ReLUWeightingScheme;
import edu.cmu.ml.proppr.learn.tools.SigmoidWeightingScheme;
import edu.cmu.ml.proppr.learn.tools.TanhWeightingScheme;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.ParamVector;
import edu.cmu.ml.proppr.util.SimpleParamVector;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;

public class L2SqLossSRWTest extends SRWTest {
	public void initSrw() {
		srw = new L2SqLossSRW();
	}
	
	@Override
	public TObjectDoubleMap<String> makeGradient(SRW srw, ParamVector paramVec, TIntDoubleMap query, int[] pos, int[] neg) {
		List<HiLo> trainingPairs = new ArrayList<HiLo>();
		for (int p : pos) {
			for (int n : neg) {
				trainingPairs.add(new HiLo(p,n));
			}
		}
		return srw.gradient(paramVec, new PairwiseRWExample(brGraph, query, trainingPairs));
	}
	
	@Override
	public double makeLoss(SRW srw, ParamVector paramVec, TIntDoubleMap query, int[] pos, int[] neg) {		
		List<HiLo> trainingPairs = new ArrayList<HiLo>();
		for (int p : pos) {
			for (int n : neg) {
				trainingPairs.add(new HiLo(p,n));
			}
		}
		return srw.empiricalLoss(paramVec, new PairwiseRWExample(brGraph, query, trainingPairs));
	}

	@Test
	public void testLearn2() {

		List<HiLo> trainingPairs = new ArrayList<HiLo>();
		for (String b : blues) {
			for (String r : reds) {
				trainingPairs.add(new HiLo(nodes.getId(r),nodes.getId(b)));
			}
		}
		
		List<PairwiseRWExample> trainGen = new ArrayList<PairwiseRWExample>();
		TIntDoubleMap query = new TIntDoubleHashMap();
		query.put(nodes.getId("b0"), 1.0); 
		trainGen.add(new PairwiseRWExample(brGraph, query, trainingPairs));
		query = new TIntDoubleHashMap();
		query.put(nodes.getId("b1"),1.0);
		trainGen.add(new PairwiseRWExample(brGraph, query, trainingPairs));
		
		L2SqLossSRW brSRW = ((L2SqLossSRW) srw);
		double originalLoss = brSRW.averageLoss(uniformParams, trainGen);
		
		System.err.println("originalLoss "+originalLoss);
		
		double setpoint = brSRW.getWeightingScheme().defaultWeight();
		
		ParamVector learnedWeightVec = brSRW.train(trainGen,uniformParams);
		double learnedLoss = brSRW.averageLoss(learnedWeightVec,trainGen);
		assertTrue(String.format("learnedLoss %f !< originalLoss %f",learnedLoss,originalLoss),learnedLoss < originalLoss);
		Set<String> features = learnedWeightVec.keySet(); 
		System.err.println(Dictionary.buildString(learnedWeightVec, new StringBuilder(), "\n").toString());
		for (String f : features) {
			if (f.equals("tor")) 
				assertTrue(
						String.format("Feature %s %f !> %f",f,learnedWeightVec.get(f),setpoint),
						learnedWeightVec.get(f) > setpoint);
			else
				assertTrue(
						String.format("Feature %s %f !< %f",f,learnedWeightVec.get(f),setpoint),
						learnedWeightVec.get(f) < setpoint); 
		}
		/*
		 *     def notestLearn2(self):
        trainingPairs = [(r,b) for b in self.blues for r in self.reds]
        def trainGen():
            yield srw.PairwiseRWExample({'b0':1.0},trainingPairs)        
            yield srw.PairwiseRWExample({'b1':1.0},trainingPairs)
        def testGen():
            yield srw.PairwiseRWExample({'b2':1.0},trainingPairs)
        uniformWeightVec = {'fromb':1.0,'tob':1.0,'fromr':1.0,'tor':1.0}
        originalLoss = self.brSRW.averageLoss(uniformWeightVec,trainGen)
        learnedWeightVec = self.brSRW.train(trainGen,initialParamVec=uniformWeightVec)
        learnedLoss = self.brSRW.averageLoss(learnedWeightVec,trainGen)
        #print 'learned loss', learnedLoss, 'original loss',originalLoss,'learned weights',learnedWeightVec
        self.assertTrue(learnedLoss < originalLoss)
        for f in learnedWeightVec:
            if f.endswith("b"): self.assertTrue(learnedWeightVec[f] < 1.0)
            else: self.assertTrue(learnedWeightVec[f] > 1.0)
		 */
	}
}
