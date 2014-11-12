package edu.cmu.ml.proppr.prove.v1;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;

import edu.cmu.ml.proppr.prove.v1.Goal;
import edu.cmu.ml.proppr.prove.v1.InnerProductWeighter;

public class InnerProductWeighterTest {

	@Test
	public void test() {
		BasicConfigurator.configure(); Logger.getRootLogger().setLevel(Level.INFO);
		HashMap<Goal,Double> weights = new HashMap<Goal,Double>();
		weights.put(new Goal("feathers"), 0.5);
		weights.put(new Goal("scales"), 0.3);
		weights.put(new Goal("fur"), 0.7);
		InnerProductWeighter w = new InnerProductWeighter(weights);
		Goal ng = new Goal("hair");
		HashMap<Goal,Double> featureDict = new HashMap<Goal,Double>();
		featureDict.put(ng, 0.9);
		featureDict.putAll(weights);
		
		assertFalse("Should start empty!",InnerProductWeighter.unknownFeatures.contains(ng));
		for (Map.Entry<Goal,Double> e : featureDict.entrySet()) {
			e.setValue(e.getValue()-Math.random()/10);
		}
		w.w(featureDict);
		assertTrue("Wasn't added!",InnerProductWeighter.unknownFeatures.contains(ng));
	}

}
