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

import edu.cmu.ml.praprolog.examples.PosNegRWExample;
import edu.cmu.ml.praprolog.graph.v1.AnnotatedGraphFactory;
import edu.cmu.ml.praprolog.graph.v1.Feature;
import edu.cmu.ml.praprolog.learn.L2PosNegLossTrainedSRW;
import edu.cmu.ml.praprolog.learn.L2SqLossSRW;
import edu.cmu.ml.praprolog.learn.SRW;
import edu.cmu.ml.praprolog.learn.tools.CookedExampleStreamer;
import edu.cmu.ml.praprolog.learn.tools.SigmoidWeightingScheme;
import edu.cmu.ml.praprolog.v1.Trainer;

public class ToyTrainerTest {
	private static final String COOKED_FILE = "testcases/toy.cooked";

	@Test
	public void test_stringvsint() {
		SRW.seed(0);
		int epochs = 20;
		
		L2PosNegLossTrainedSRW<Integer> srwInt = new L2PosNegLossTrainedSRW<Integer>();
		srwInt.setWeightingScheme(new SigmoidWeightingScheme());
		Trainer<Integer> trainerInt = new Trainer<Integer>(srwInt);
		Map<String,Double> paramVecInt = trainerInt.trainParametersOnCookedIterator(
				new CookedExampleStreamer<Integer>(COOKED_FILE, 
						new AnnotatedGraphFactory<Integer>(AnnotatedGraphFactory.INT)),
				epochs,
				false);//tracelosses

		SRW.seed(0);
		L2PosNegLossTrainedSRW<String> srwStr = new L2PosNegLossTrainedSRW<String>();
		srwStr.setWeightingScheme(new SigmoidWeightingScheme());
		Trainer<String> trainerStr = new Trainer<String>(srwStr);
		Map<String,Double> paramVecStr = trainerStr.trainParametersOnCookedIterator(
				new CookedExampleStreamer<String>(COOKED_FILE, 
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
			assertEquals(f +" ("+ (paramVecInt.get(f)-paramVecStr.get(f))+")",paramVecStr.get(f),paramVecInt.get(f),0.01);
		}
	}
	
	
}
