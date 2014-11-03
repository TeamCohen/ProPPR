package edu.cmu.ml.praprolog.graph;

public abstract class Index<K,I> {
	public abstract K asKey(I id);
	public abstract I asId(K key);
}
