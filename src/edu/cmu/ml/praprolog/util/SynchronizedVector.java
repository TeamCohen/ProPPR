package edu.cmu.ml.praprolog.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class SynchronizedVector implements Map<String,Double> {
	Map<String,Double> back;
	public SynchronizedVector() {
		back = new TreeMap<String,Double>();
	}
	public SynchronizedVector(Map<String,Double> init) {
		back = init;
	}
	
	/**************************** Read *****************************/

	/**
	 * Returns a defensive copy so that RO-iteration is possible while
	 * other objects access the map.
	 */
	public synchronized Set<String> keySet() { // can't iterate while modifications are taking place?
		TreeSet<String> result = new TreeSet<String>();
		for (String k : back.keySet()) {
			result.add(k);
		}
		return result;//Collections.unmodifiableSet(back.keySet());
	}
	
	public Double get(String key) {
		return back.get(key);
	}
	
	@Override
	public boolean containsKey(Object arg0) {
		return back.containsKey(arg0);
	}
	@Override
	public boolean containsValue(Object value) {
		return back.containsValue(value);
	}
	@Override
	public Set<java.util.Map.Entry<String, Double>> entrySet() {
		// may have write capabilities through Entry?
		throw new UnsupportedOperationException("Not yet implemented!");
	}
	@Override
	public Double get(Object key) {
		return back.get(key);
	}
	@Override
	public boolean isEmpty() {
		return back.isEmpty();
	}
	@Override
	public int size() {
		return back.size();
	}
	@Override
	public Collection<Double> values() {
		return back.values();
	}
	
/************************************** Write **************************************/
	@Override
	public synchronized void clear() {
		back.clear();
	}
	@Override
	public synchronized Double put(String key, Double value) {
		return back.put(key,value);
	}
	@Override
	public synchronized void putAll(Map<? extends String, ? extends Double> m) {
		back.putAll(m);
	}
	@Override
	public synchronized Double remove(Object key) {
		return back.remove(key);
	}
}