package edu.cmu.ml.proppr.learn.tools;

import java.util.Set;
import java.util.TreeSet;

import edu.cmu.ml.proppr.util.math.ParamVector;

public class FixedWeightRules {
	Set<String> exact = new TreeSet<String>();
	Set<String> startsWith = new TreeSet<String>();
	public FixedWeightRules() {}
	public FixedWeightRules(String[] init) {
		for (String r : init) {
			if (r.endsWith("*")) startsWith.add(r.substring(0, r.length()-1));
			else exact.add(r);
		}
	}

	public boolean isFixed(String feature) {
		if (exact.contains(feature)) return true;
		for (String prefix : startsWith) if (feature.startsWith(prefix)) return true;
		return false;
	}

	public void addExact(String feature) {
		exact.add(feature);
	}
	
	public void initializeFixed(ParamVector<String,?> params) {
		/* 
		 * Future work: Could add syntax to fix features at arbitrary values here.
		 */
		for (String f : exact) params.put(f, 1.0);
	}
}
