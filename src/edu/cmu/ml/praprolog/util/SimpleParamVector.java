package edu.cmu.ml.praprolog.util;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SimpleParamVector extends ParamVector<Double> {
	private Map<String,Double> backingStore;
	public SimpleParamVector() {
		this.backingStore = new ConcurrentHashMap<String,Double>();
	}
	public SimpleParamVector(Map<String,Double> store) {
		this.backingStore = store;
	}
	
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
	
	@Override
	public Set<java.util.Map.Entry<String, Double>> entrySet() {
		return backingStore.entrySet();
	}

}
