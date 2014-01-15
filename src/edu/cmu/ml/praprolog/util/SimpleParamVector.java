package edu.cmu.ml.praprolog.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SimpleParamVector extends ParamVector<Double> {
	ConcurrentHashMap<String,Double> backingStore = new ConcurrentHashMap<String,Double>();
	
	@Override
	protected Map<String, Double> getBackingStore() {
		return backingStore;
	}

	@Override
	protected Double getWeight(Double value) {
		return value;
	}

	@Override
	protected Double newValue(Double value) {
		return value;
	}

}
