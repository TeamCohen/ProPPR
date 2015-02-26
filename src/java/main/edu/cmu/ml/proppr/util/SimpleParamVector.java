package edu.cmu.ml.proppr.util;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SimpleParamVector<F> extends ParamVector<F,Double> {
	private ConcurrentHashMap<F,Double> backingStore;
	public SimpleParamVector() {
		this.backingStore = new ConcurrentHashMap<F,Double>();
	}
	public SimpleParamVector(Map<F, Double> map) {
		
		if (map instanceof ConcurrentHashMap)
			this.backingStore = (ConcurrentHashMap) map;
		else {
			this.backingStore = new ConcurrentHashMap<F,Double>();
			this.backingStore.putAll(map);
		}
	}
	
	@Override
	protected ConcurrentHashMap<F, Double> getBackingStore() {
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
	public Set<java.util.Map.Entry<F, Double>> entrySet() {
		return backingStore.entrySet();
	}

}
