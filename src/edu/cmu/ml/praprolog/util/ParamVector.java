package edu.cmu.ml.praprolog.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public abstract class ParamVector<T> implements Map<String,Double> {
	
	protected abstract Map<String,T> getBackingStore();
	protected abstract Double getWeight(T value);
	protected abstract T newValue(Double value);
	
	@Override
	public Set<String> keySet() {
		return getBackingStore().keySet();
	}
	
	@Override
	public boolean containsKey(Object arg0) {
		return getBackingStore().containsKey(arg0);
	}
	@Override
	public boolean containsValue(Object value) {
		return getBackingStore().containsValue(value);
	}
	@Override
	public abstract Set<java.util.Map.Entry<String, Double>> entrySet();
	
	@Override
	public Double get(Object key) {
		return getWeight(getBackingStore().get(key));
	}
	@Override
	public boolean isEmpty() {
		return getBackingStore().isEmpty();
	}
	@Override
	public int size() {
		return getBackingStore().size();
	}
	@Override
	public Collection<Double> values() {
		Map<String,T> back = getBackingStore();
		ArrayList<Double> result = new ArrayList<Double>(back.size());
		for (T value : back.values()) result.add(getWeight(value));
		return result;
	}
	
	@Override
	public void clear() {
		getBackingStore().clear();
	}
	int k=0;
	@Override
	public Double put(String key, Double value) {
		if (key.equals("fixedWeight") && k++ > 0) throw new IllegalStateException("Modifying fixedWeight!");
		T nv = this.newValue(value);
		this.getBackingStore().put(key,nv);
		return getWeight(nv);
	}
	@Override
	public synchronized void putAll(Map<? extends String, ? extends Double> m) {
		// synchronized to match the behavior of ConcurrentHashMap.putAll()
		Map<String,T> back = getBackingStore();
		for (Map.Entry<? extends String, ? extends Double> e : m.entrySet()) {
			back.put(e.getKey(), newValue(e.getValue()));
		}
	}
	@Override
	public Double remove(Object key) {
		return getWeight(getBackingStore().remove(key));
	}

	public ParamVector copy () {
        ParamVector copy = new SimpleParamVector();
        copy.putAll(this);
        return copy;
    }
}
