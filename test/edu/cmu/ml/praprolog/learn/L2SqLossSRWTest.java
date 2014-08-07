package edu.cmu.ml.praprolog.learn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.junit.Test;

import edu.cmu.ml.praprolog.graph.AnnotatedGraph;
import edu.cmu.ml.praprolog.learn.tools.LinearWeightingScheme;
import edu.cmu.ml.praprolog.learn.tools.PairwiseRWExample;
import edu.cmu.ml.praprolog.learn.tools.PosNegRWExample;
import edu.cmu.ml.praprolog.learn.tools.TanhWeightingScheme;
import edu.cmu.ml.praprolog.learn.tools.PairwiseRWExample.HiLo;
import edu.cmu.ml.praprolog.learn.tools.ReLUWeightingScheme;
import edu.cmu.ml.praprolog.learn.tools.SigmoidWeightingScheme;
import edu.cmu.ml.praprolog.util.Dictionary;
import edu.cmu.ml.praprolog.util.ParamVector;
import edu.cmu.ml.praprolog.util.SimpleParamVector;

public class L2SqLossSRWTest extends SRWTest {
	public void initSrw() {
		srw = new L2SqLossSRW();
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

	@Test
	public void testLearn2() {

		List<HiLo> trainingPairs = new ArrayList<HiLo>();
		for (String b : blues) {
			for (String r : reds) {
				trainingPairs.add(new HiLo(r,b));
			}
		}
		
		AnnotatedGraph g = brGraphs.get(0);
		
		List<PairwiseRWExample> trainGen = new ArrayList<PairwiseRWExample>();
		TreeMap<String,Double> query = new TreeMap<String,Double>();
		query.put("b0", 1.0); 
		trainGen.add(new PairwiseRWExample(g, query, trainingPairs));
		query = new TreeMap<String,Double>();
		query.put("b1",1.0);
		trainGen.add(new PairwiseRWExample(g, query, trainingPairs));
		
		L2SqLossSRW brSRW = ((L2SqLossSRW) srw);
		double originalLoss = brSRW.averageLoss(uniformWeightVec, trainGen);
		
		System.err.println("originalLoss "+originalLoss);
		
		double setpoint = brSRW.getWeightingScheme().defaultWeight();
		
		ParamVector learnedWeightVec = brSRW.train(trainGen,uniformWeightVec);
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
