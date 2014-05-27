package edu.cmu.ml.praprolog.learn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Test;

import edu.cmu.ml.praprolog.graph.AnnotatedGraph;
import edu.cmu.ml.praprolog.learn.PairwiseRWExample.HiLo;

public class L2SqLossSRWTest extends SRWTest {
	public void initSrw() {
		srw = new L2SqLossSRW();
	}
	
	@Test
	public void testGradient() {
		
		TreeMap<String,Double> startVec = new TreeMap<String,Double>();
		startVec.put("r0",1.0);
		Map<String,Double> baseLineVec = brGraphs.get(0).rwr(startVec);
		
		Map<String,Double> blues = bluePart(baseLineVec);
		Map<String,Double> reds = redPart(baseLineVec);
		List<HiLo> trainingPairs = new ArrayList<HiLo>();
		for (String b : blues.keySet()) {
			for (String r : reds.keySet()) {
				trainingPairs.add(new HiLo(b,r));
			}
		}
		
		TreeMap<String,Double>uniformWeightVec = new TreeMap<String,Double>();
		String[] names = {"fromb","tob","fromr","tor"};
		for (String n : names) uniformWeightVec.put(n,1.0);
		
		L2SqLossSRW srw = (L2SqLossSRW) this.srw;
		double baselineLoss = srw.empiricalLoss(uniformWeightVec, new PairwiseRWExample(brGraphs.get(0), startVec, trainingPairs));
		
		Map<String,Double> biasedWeightVec = (Map<String, Double>) uniformWeightVec.clone();
		biasedWeightVec.put("tob", 10.0);
		biasedWeightVec.put("tor", 0.1);
		double biasedLoss = srw.empiricalLoss(biasedWeightVec, new PairwiseRWExample(brGraphs.get(0), startVec, trainingPairs));
		
		assertTrue(String.format("baselineLoss %f should be > than biasedLoss %f",baselineLoss,biasedLoss),
				baselineLoss > biasedLoss);
		assertEquals("baselineLoss",0.010638133294,baselineLoss,1e-4);
		assertEquals("biasedLoss",0.00129850,biasedLoss,1e-5);
//		assertEquals("biasedLoss",0,biasedLoss,1e-4); <-- pre-sigmoid value
		
		
		Map<String,Double> gradient = srw.gradient(uniformWeightVec, new PairwiseRWExample(brGraphs.get(0), startVec, trainingPairs));
		
		assertTrue("gradient @ tob "+gradient.get("tob"),gradient.get("tob") < 0);
		assertTrue("gradient @ fromb "+gradient.get("fromb"),gradient.get("fromb") < 0);
		assertTrue("gradient @ tor "+gradient.get("tor"),gradient.get("tor") > 0);
		assertTrue("gradient @ fromr "+gradient.get("fromr"),gradient.get("fromr") > 0);
		
		double eps = .001;
		TreeMap<String,Double> nearlyUniformWeightVec = new TreeMap<String,Double>();
		for (String f : gradient.keySet()) nearlyUniformWeightVec.put(f,1.0-eps*gradient.get(f));
		double improvedBaselineLoss = srw.empiricalLoss(nearlyUniformWeightVec, new PairwiseRWExample(brGraphs.get(0), startVec, trainingPairs));
		assertTrue("baselineLoss "+baselineLoss+" should be > improvedBaselineLoss "+improvedBaselineLoss, baselineLoss > improvedBaselineLoss);
				/*

        
        #the gradient should be negative for the blue features, positive for the red features
        gradient = self.brSRWs[0].gradient(uniformWeightVec,srw.PairwiseRWExample(self.brGraphs[0],startVec,trainingPairs))
        #print 'gradient',gradient
        self.assertTrue(gradient['tob'] < 0)
        self.assertTrue(gradient['fromb'] < 0)
        self.assertTrue(gradient['tor'] > 0)
        self.assertTrue(gradient['fromr'] > 0)

        #subtracting epsilon times the gradient should help
        eps = 0.001
        nearlyUniformWeightVec = {}
        for f in gradient:
            nearlyUniformWeightVec[f] = 1.0 - eps*gradient[f]
        improvedBaselineLoss = self.brSRWs[0].empiricalLoss(nearlyUniformWeightVec,srw.PairwiseRWExample(self.brGraphs[0],startVec,trainingPairs))
        #print 'baselineLoss',baselineLoss,'>','improved baseline',improvedBaselineLoss
        self.assertTrue(baselineLoss > improvedBaselineLoss)
        
		 */
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
		
//		List<PairwiseRWExample> testGen = new ArrayList<PairwiseRWExample>();
//		query = new TreeMap<String,Double>();
//		query.put("b2", 1.0); 
//		testGen.add(new PairwiseRWExample(g, query, trainingPairs));
		
		Map<String,Double> uniformWeightVec = new TreeMap<String,Double>();
		uniformWeightVec.put("fromb", 1.0);
		uniformWeightVec.put("tob", 1.0);
		uniformWeightVec.put("fromr", 1.0);
		uniformWeightVec.put("tor", 1.0);
		
		L2SqLossSRW brSRW = ((L2SqLossSRW) srw);
		double originalLoss = brSRW.averageLoss(uniformWeightVec, trainGen);
		
		System.err.println("originalLoss "+originalLoss);
//		if(true)return;
		
		Map<String,Double> learnedWeightVec = brSRW.train(trainGen,uniformWeightVec);
		double learnedLoss = brSRW.averageLoss(learnedWeightVec,trainGen);
		assertTrue(String.format("learnedLoss %f !< originalLoss %f",learnedLoss,originalLoss),learnedLoss < originalLoss);
		for (String f : learnedWeightVec.keySet()) {
			if (f.endsWith("b")) assertTrue(String.format("Feature %s %f !< 1.0",f,learnedWeightVec.get(f)),learnedWeightVec.get(f) < 1.0);
			else assertTrue(String.format("Feature %s %f !> 1.0",f,learnedWeightVec.get(f)),learnedWeightVec.get(f) > 1.0);
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
