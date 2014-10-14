package edu.cmu.ml.praprolog.prove;

public abstract class State {
	protected int[] heap;
	public boolean hasConstantAt(int i) { return heap[i]<0; }
	public boolean hasVariableAt(int i) { return heap[i]>=0; }
	public boolean hasFreeAt(int i) { return heap[i]==i; }
	public int getVariableAt(int i) { return heap[i]; }
	public int getIdOfConstantAt(int i) { 
		if (heap[i]>=0) throw new InvalidHeapException();
		return -heap[i];
	}
	public int createConstantCell(int id) {
		if (id<1) throw new IllegalArgumentException();
		return -id;
	}
	public int createVariableCell(int a) { return a; }
	public int copyConstantCell(int i) { return heap[i]; }
	public int dereference(int i) {}
	
	public abstract ImmutableState immutableVersion();
	public abstract MutableState mutableVersion();
	public abstract void collapsePointers(int i, int last);
}
