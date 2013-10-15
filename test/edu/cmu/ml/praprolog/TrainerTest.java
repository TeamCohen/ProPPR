package edu.cmu.ml.praprolog;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.Ignore;
import org.junit.Test;

import edu.cmu.ml.praprolog.graph.AnnotatedGraphFactory;
import edu.cmu.ml.praprolog.graph.Feature;
import edu.cmu.ml.praprolog.learn.L2PosNegLossTrainedSRW;
import edu.cmu.ml.praprolog.learn.L2SqLossSRW;
import edu.cmu.ml.praprolog.learn.PosNegRWExample;
import edu.cmu.ml.praprolog.learn.SRW;

public class TrainerTest {
	@Test @Ignore
	public void testImportCookedExamples() {
		String[] properParams = {"alphaBooster",
				"id(defaultRestart)","id(demo/textcat/toylabels.facts)","id(demo/textcat/toywords.graph)",
				"id(trueLoop)","id(trueLoopRestart)",
				"r",
				"w(10,neg)","w(10,pos)","w(7-seater,neg)","w(7-seater,pos)",
				"w(and,neg)","w(and,pos)","w(a,neg)","w(an,neg)",
				"w(an,pos)","w(a,pos)","w(at,neg)","w(at,pos)",
				"w(automatic,neg)","w(automatic,pos)","w(backlog,neg)","w(backlog,pos)",
				"w(barbie,neg)","w(barbie,pos)","w(big,neg)","w(big,pos)",
				"w(bike,neg)","w(bike,pos)","w(bills,neg)","w(bills,pos)",
				"w(car,neg)","w(car,pos)","w(catalogs,neg)","w(catalogs,pos)",
				"w(convertible,neg)","w(convertible,pos)","w(crushing,neg)","w(crushing,pos)",
				"w(doll,neg)","w(doll,pos)","w(due,neg)","w(due,pos)",
				"w(email,neg)","w(email,pos)","w(fire,neg)","w(fire,pos)",
				"w(forms,neg)","w(forms,pos)","w(for,neg)","w(for,pos)",
				"w(house,neg)","w(house,pos)","w(huge,neg)","w(huge,pos)",
				"w(IBM,neg)","w(IBM,pos)","w(in,neg)","w(in,pos)",
				"w(job,neg)","w(job,pos)","w(junk,neg)","w(junk,pos)",
				"w(ken,neg)","w(ken,pos)","w(life,neg)","w(life,pos)",
				"w(little,neg)","w(little,pos)","w(mail,neg)","w(mail,pos)",
				"w(minivan,neg)","w(minivan,pos)","w(mortgage,neg)","w(mortgage,pos)",
				"w(of,neg)","w(of,pos)","w(paperwork,neg)","w(paperwork,pos)",
				"w(pile,neg)","w(pile,pos)","w(porshe,neg)","w(porshe,pos)",
				"w(pricy,neg)","w(pricy,pos)","w(punk,neg)","w(punk,pos)",
				"w(queen,neg)","w(queen,pos)","w(red,neg)","w(red,pos)",
				"w(speed,neg)","w(speed,pos)","w(sports,neg)","w(sports,pos)",
				"w(suburbs,neg)","w(suburbs,pos)","w(tax,neg)","w(tax,pos)",
				"w(the,neg)","w(the,pos)","w(toy,neg)","w(toy,pos)",
				"w(transmission,neg)","w(transmission,pos)","w(trouble,neg)","w(trouble,pos)",
				"w(truck,neg)","w(truck,pos)","w(wagon,neg)","w(wagon,pos)",
				"w(with,neg)","w(with,pos)","w(woe,neg)","w(woe,pos)",
				"w(yellow,neg)","w(yellow,pos)","w(yesterday,neg)","w(yesterday,pos)"};
		
		SRW<PosNegRWExample<String>> learner = new L2PosNegLossTrainedSRW<String>();
		Trainer<String> t = new Trainer<String>(learner);
		Collection<PosNegRWExample<String>> examples = t.importCookedExamples("toy.cooked", new AnnotatedGraphFactory<String>(AnnotatedGraphFactory.STRING));
		
		TreeSet<String> features = new TreeSet<String>();
		for (PosNegRWExample<String> e : examples) {
			for (String f : e.getGraph().getFeatureSet()) {
				features.add(f);
			}
		}
		for (String s : properParams) {
			assertTrue("features "+s,features.contains(s));
		}
		
		Map<String,Double> fauxParamsVec = new HashMap<String,Double>();
		for (PosNegRWExample<String> e : examples) {
			SRW.addDefaultWeights(e.getGraph(),fauxParamsVec);
		}
		for(String s : properParams) {
			assertTrue("params "+s,fauxParamsVec.containsKey(s));
		}
		
	}
	
	@Test @Ignore
	public void testTextcatToyLoss() {
		double[] exampleLosses = {
				//loss 
				3.05647902252,
				//example PosNegRWExample[19/47; ['1'] -> +['11']; -['10']] ...
				//loss 
				3.03595738317,
				//example PosNegRWExample[13/31; ['1'] -> +['9']; -['8']] ...
				//loss 
				3.08298606805,
//				example PosNegRWExample[19/47; ['1'] -> +['11']; -['10']] ...
//				loss 
				3.02843414485,
//				example PosNegRWExample[19/47; ['1'] -> +['11']; -['10']] ...
//				loss 
				3.03763763126,
//				example PosNegRWExample[16/39; ['1'] -> +['10']; -['9']] ...
//				loss 
				3.04500722867,
//				example PosNegRWExample[28/71; ['1'] -> +['13']; -['14']] ...
//				loss 
				3.0069055808,
//				example PosNegRWExample[31/79; ['1'] -> +['14']; -['15']] ...
//				loss 
				3.00066248875,
//				example PosNegRWExample[22/55; ['1'] -> +['11']; -['12']] ...
//				loss 
				3.0238713836,
//				example PosNegRWExample[28/71; ['1'] -> +['13']; -['14']] ...
//				loss 
				3.00627958553,
//				example PosNegRWExample[28/71; ['1'] -> +['13']; -['14']] ...
//				loss 
				3.00629362613
		};
		
		SRW<PosNegRWExample<String>> learner = new L2PosNegLossTrainedSRW<String>();
		Trainer<String> t = new Trainer<String>(learner);
		Map<String,Double> paramVec = new TreeMap<String,Double>();
		
		for (String f : learner.untrainedFeatures()) {
            paramVec.put(f,1.0);
		}
		
		double trainingLoss = 0;
		int numExamples = 0;
		int i=0;
		for (PosNegRWExample<String> x : t.importCookedExamples("toy.cooked", new AnnotatedGraphFactory<String>(AnnotatedGraphFactory.STRING))) {
			SRW.addDefaultWeights(x.getGraph(),paramVec);
			learner.trainOnExample(paramVec, x);
			double tl = learner.empiricalLoss(paramVec,x);
			assertEquals("example "+i+" training loss",exampleLosses[i],tl,1e-2);
			trainingLoss += tl;
			numExamples += x.length();
			i++;
		}
		assertEquals("epoch 1 training loss",1.51502337015,trainingLoss/numExamples,5e-4);
	}

	@Test
	public void test_stringvsint() {
		int epochs = 20;
		
		L2PosNegLossTrainedSRW<Integer> srwInt = new L2PosNegLossTrainedSRW<Integer>();
		Trainer<Integer> trainerInt = new Trainer<Integer>(srwInt);
		Map<String,Double> paramVecInt = trainerInt.trainParametersOnCookedIterator(
				trainerInt.importCookedExamples("toy.cooked", 
						new AnnotatedGraphFactory<Integer>(AnnotatedGraphFactory.INT)),
				epochs,
				false);//tracelosses
		
		L2PosNegLossTrainedSRW<String> srwStr = new L2PosNegLossTrainedSRW<String>();
		Trainer<String> trainerStr = new Trainer<String>(srwStr);
		Map<String,Double> paramVecStr = trainerStr.trainParametersOnCookedIterator(
				trainerStr.importCookedExamples("toy.cooked", 
						new AnnotatedGraphFactory<String>(AnnotatedGraphFactory.STRING)),
				epochs,
				false);//tracelosses
		System.err.println("name                  strvalue  intvalue");
		for (String f : paramVecStr.keySet()) {
			
			assertTrue(f+" not in Int map",paramVecInt.containsKey(f));
			System.err.println(String.format("%20s  %f  %f",f,paramVecStr.get(f),paramVecInt.get(f)));

			assertFalse(f+" str nan",paramVecStr.get(f).isNaN());
			assertFalse(f+" str inf",paramVecStr.get(f).isInfinite());
			assertFalse(f+" int nan",paramVecInt.get(f).isNaN());
			assertFalse(f+" int inf",paramVecInt.get(f).isInfinite());
			// accurate to 1% since most param values ~=1
			assertEquals(f,paramVecStr.get(f),paramVecInt.get(f),0.01);
		}
	}
	
	
}
