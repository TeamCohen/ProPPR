package edu.cmu.ml.proppr.prove;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;

import edu.cmu.ml.proppr.prove.InnerProductWeighter;
import edu.cmu.ml.proppr.prove.wam.Feature;
import edu.cmu.ml.proppr.prove.wam.Goal;

public class InnerProductWeighterTest {

	@Test
	public void test() {
		BasicConfigurator.configure(); Logger.getRootLogger().setLevel(Level.INFO);
		HashMap<Feature,Double> weights = new HashMap<Feature,Double>();
		weights.put(new Feature("feathers"), 0.5);
		weights.put(new Feature("scales"), 0.3);
		weights.put(new Feature("fur"), 0.7);
		InnerProductWeighter w = new InnerProductWeighter(weights);
		Feature ng = new Feature("hair");
		HashMap<Feature,Double> featureDict = new HashMap<Feature,Double>();
		featureDict.put(ng, 0.9);
		featureDict.putAll(weights);
		
		assertFalse("Should start empty!",w.unknownFeatures.contains(ng));
		for (Map.Entry<Feature,Double> e : featureDict.entrySet()) {
			e.setValue(e.getValue()-Math.random()/10);
		}
		w.w(featureDict);
		assertTrue("Wasn't added!",w.unknownFeatures.contains(ng));
	}

}
