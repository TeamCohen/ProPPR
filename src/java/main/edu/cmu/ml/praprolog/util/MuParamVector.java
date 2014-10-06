package edu.cmu.ml.praprolog.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A version of the parameter vector which also tracks the last update time of each key
 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
 *
 */
public class MuParamVector extends ParamVector<TimestampedWeight> {
	private ConcurrentHashMap<String,TimestampedWeight> backingStore;
	private int count=0;
	
	public MuParamVector() {
		this.backingStore = new ConcurrentHashMap<String,TimestampedWeight>();
	}
	public MuParamVector(Map<String,Double> store) {
		this();
		for(Map.Entry<String, Double> e : store.entrySet()) {
			this.backingStore.put(e.getKey(), newValue(e.getValue()));
		}
	}
	
	@Override
	protected Map<String, TimestampedWeight> getBackingStore() {
		return this.backingStore;
	}

	@Override
	protected TimestampedWeight newValue(Double value) {
		return new TimestampedWeight(value,this.count);
	}

	@Override
	protected Double getWeight(TimestampedWeight value) {
		if (value==null) {
			throw new IllegalStateException("null?");
		}
		return value.wt;
	}
	
	@Override
	public Set<java.util.Map.Entry<String, Double>> entrySet() {
		return new MuSet(this.backingStore.entrySet());
	}

	public int getLast(String key) {
		if (!this.backingStore.containsKey(key)) 
			return 0;
		return this.count - this.backingStore.get(key).k;
	}
	
	public void setLast(Set<String> keys) {
		// not synchronized for now...
		for(String s : keys) {
			this.backingStore.get(s).k = this.count;
		}
	}
	
	public void count() {
		this.count++;
	}
	
	@Override
	public ParamVector copy () {
		MuParamVector copy = new MuParamVector();
        copy.putAll(this);
        return copy;
    }
	
	/** Utility class so we can fake entry iteration over <String,Double>
	 * 
	 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
	 *
	 */
	public static class MuSet implements Set<java.util.Map.Entry<String,Double>>, Iterator<java.util.Map.Entry<String, Double>> {
		private Iterator<Entry<String, TimestampedWeight>> entries;

		private MuSet(Set<java.util.Map.Entry<String,TimestampedWeight>> entries) {
			this.entries = entries.iterator();
		}

		@Override
		public Iterator<java.util.Map.Entry<String, Double>> iterator() {
			return this;
		}

		@Override
		public boolean hasNext() {
			return this.entries.hasNext();
		}

		@Override
		public java.util.Map.Entry<String, Double> next() {
			java.util.Map.Entry<String, TimestampedWeight> next = this.entries.next();
			return new MuEntry(next);
		}
		
		// ***************** Dummy methods, never used:
		
		@Override
		public boolean add(java.util.Map.Entry<String, Double> arg0) {
			throw new UnsupportedOperationException("Not yet implemented!");
		}

		@Override
		public boolean addAll(
				Collection<? extends java.util.Map.Entry<String, Double>> arg0) {
			throw new UnsupportedOperationException("Not yet implemented!");
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException("Not yet implemented!");
		}

		@Override
		public boolean contains(Object arg0) {
			throw new UnsupportedOperationException("Not yet implemented!");
		}

		@Override
		public boolean containsAll(Collection<?> arg0) {
			throw new UnsupportedOperationException("Not yet implemented!");
		}

		@Override
		public boolean isEmpty() {
			throw new UnsupportedOperationException("Not yet implemented!");
		}

		@Override
		public boolean remove(Object arg0) {
			throw new UnsupportedOperationException("Not yet implemented!");
		}

		@Override
		public boolean removeAll(Collection<?> arg0) {
			throw new UnsupportedOperationException("Not yet implemented!");
		}

		@Override
		public boolean retainAll(Collection<?> arg0) {
			throw new UnsupportedOperationException("Not yet implemented!");
		}

		@Override
		public int size() {
			throw new UnsupportedOperationException("Not yet implemented!");
		}

		@Override
		public Object[] toArray() {
			throw new UnsupportedOperationException("Not yet implemented!");
		}

		@Override
		public <T> T[] toArray(T[] arg0) {
			throw new UnsupportedOperationException("Not yet implemented!");
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("Not yet implemented!");
			
		}
		public static class MuEntry implements java.util.Map.Entry<String,Double> {
			java.util.Map.Entry<String, TimestampedWeight> source;
			private MuEntry(java.util.Map.Entry<String, TimestampedWeight> src) {
				this.source = src;
			}
			@Override
			public String getKey() {
				return this.source.getKey();
			}

			@Override
			public Double getValue() {
				return this.source.getValue().wt;
			}

			@Override
			public Double setValue(Double value) {
				this.source.getValue().wt = value;
				return value;
			}
			
		}
	}
}
