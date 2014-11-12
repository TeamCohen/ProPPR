package edu.cmu.ml.proppr.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public abstract class ParamVector<F,T> implements Map<F,Double> {
	
	protected abstract Map<F,T> getBackingStore();
	protected abstract Double getWeight(T value);
	protected abstract T newValue(Double value);
	
	@Override
	public Set<F> keySet() {
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
	public abstract Set<java.util.Map.Entry<F, Double>> entrySet();
	
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
		Map<F,T> back = getBackingStore();
		ArrayList<Double> result = new ArrayList<Double>(back.size());
		for (T value : back.values()) result.add(getWeight(value));
		return result;
	}
	
	@Override
	public void clear() {
		getBackingStore().clear();
	}
	@Override
	public Double put(F key, Double value) {
		T nv = this.newValue(value);
		this.getBackingStore().put(key,nv);
		return getWeight(nv);
	}
	@Override
	public synchronized void putAll(Map<? extends F, ? extends Double> m) {
		// synchronized to match the behavior of ConcurrentHashMap.putAll()
		Map<F,T> back = getBackingStore();
		for (Map.Entry<? extends F, ? extends Double> e : m.entrySet()) {
			back.put(e.getKey(), newValue(e.getValue()));
		}
	}
	@Override
	public Double remove(Object key) {
		return getWeight(getBackingStore().remove(key));
	}

	public ParamVector<F,Double> copy () {
        ParamVector<F,Double> copy = new SimpleParamVector<F>();
        copy.putAll(this);
        return copy;
    }
}
