package edu.cmu.ml.proppr.util;

import java.util.Map;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A symbol table mapping strings to/from integers in the range
    1..N inclusive.
 *
 * @author wcohen
 *
 */
public class ConcurrentSymbolTable<T> 
{
	static public interface HashingStrategy<T> 
	{
		public int computeHashCode(T symbol);
		public boolean equals(T o1,T o2);
	}
	public class DefaultHashingStrategy<T> implements HashingStrategy<T>
	{
		public int computeHashCode(T symbol) { return symbol.hashCode(); }
		public boolean equals(T o1,T o2) { return o1.equals(o2); }
	}


	protected ConcurrentHashMap<Integer,Integer> symbol2Id = new ConcurrentHashMap<Integer,Integer>();
	protected ConcurrentHashMap<Integer,T> id2symbol  = new ConcurrentHashMap<Integer,T>();
	protected HashingStrategy hashingStrategy;
	protected int nextId = 0;
	
	public ConcurrentSymbolTable(HashingStrategy<T> strategy) {
		this.hashingStrategy = strategy==null? new DefaultHashingStrategy<T>() : strategy;
	}
	public ConcurrentSymbolTable() {
		this(null);
	}

	/**
	 * Insert a symbol.
	 * @param s
	 */
	public void insert(T symbol) {
		int h = hashingStrategy.computeHashCode(symbol);
		if (!symbol2Id.containsKey(h)) {
			int newId = -1;
			synchronized(this) {
				newId = ++nextId;
			}
			symbol2Id.put(h,newId);
			id2symbol.put(newId,symbol);
		}
	}
	
	/**
	 * Get the numeric id, between 1 and N, of a symbol, inserting it if
	 * needed.
	 * @param symbol
	 * @return
	 */
	public int getId(T symbol) {
		insert(symbol);
		int h = hashingStrategy.computeHashCode(symbol);
		return symbol2Id.get(h);
	}

	
	/** Test if the symbol has been inserted. 
	 */
	public boolean hasId(T symbol) {
		int h = hashingStrategy.computeHashCode(symbol);
		return symbol2Id.containsKey(h);
	}

	/** Get the symbol for an id.
	 */
	public T getSymbol(int id) {
		return this.id2symbol.get(id);
	}

	/** Return N, the largest id.
	 */
	public int maxId() {
		return this.symbol2Id.size();
	}

	// simple command-line test 
	static public void main(String[] argv) 
	{
		ConcurrentSymbolTable stab = new ConcurrentSymbolTable<String>();
		for (int i=0; i<argv.length; i++) {
			if (stab.hasId(argv[i])) {
				System.out.println("duplicate: "+argv[i]+" has id "+stab.getId(argv[i]));
			}
			stab.insert(argv[i]);
		}
		for (int i=1; i<=stab.maxId(); i++) {
			System.out.println(i + ":\t" + stab.getSymbol(i));
		}
	}
}

