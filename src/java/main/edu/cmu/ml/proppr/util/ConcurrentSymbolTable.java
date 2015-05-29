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
	protected ConcurrentHashMap<T,Integer> symbol2Id = new ConcurrentHashMap<T,Integer>();
	protected ConcurrentHashMap<Integer,T> id2symbol  = new ConcurrentHashMap<Integer,T>();
	protected int nextId = 0;
	
	/**
	 * Insert a symbol.
	 * @param s
	 */
	public void insert(T symbol) {
		if (!this.symbol2Id.containsKey(symbol)) {
			int newId = -1;
			synchronized(this) {
				newId = ++nextId;
			}
			this.symbol2Id.put(symbol,newId);
			this.id2symbol.put(newId,symbol);
		}
	}
	
	/**
	 * Get the numeric id, between 1 and N, of a symbol, inserting
	 * it if needed.
	 * @param symbol
	 * @return
	 */
	public int getId(T symbol) {
		this.insert(symbol);
		return this.symbol2Id.get(symbol);
	}

	
	/** Test if the symbol has been inserted. 
	 */
	public boolean hasId(T symbol) {
		return this.symbol2Id.containsKey(symbol);
	}

	/** Get the symbol for an id.
	 */
	public T getSymbol(int id) {
		return this.id2symbol.get(id);
	}

	/** Return an iterator over the symbol/id pairs
	 */
	public Iterator<Map.Entry<T,Integer> > getSymbolIterator() {
		return this.symbol2Id.entrySet().iterator();
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

