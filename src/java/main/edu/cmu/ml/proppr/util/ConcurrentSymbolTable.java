package edu.cmu.ml.proppr.util;

import java.util.Map;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A "symbol table" mapping arbitrary objects (called 'symbols' in a
 * nod to LISP) to and from 'ids', i.e., integers in the range 1..N
 * (inclusive.)  This is based on ConcurrentHashMap objects so it will
 * hopefully be easy to share among different threads.
 *
 * @author wcohen
 *
 */
public class ConcurrentSymbolTable<T> implements SymbolTable<T>
{
	/** Analogous to a gnu.trove hashing strategy.  Objects will be
	 * assigned distinct id's iff they have different hash codes.
	 **/
	static public interface HashingStrategy<T> 
	{
		public Object computeKey(T symbol);
		public boolean equals(T o1,T o2);
	}
	public class DefaultHashingStrategy<T> implements HashingStrategy<T>
	{
		public Integer computeKey(T symbol) { return symbol.hashCode(); }
		public boolean equals(T o1,T o2) { return o1.equals(o2); }
	}
	public class IdentityHashingStrategy<T> implements HashingStrategy<T>
	{
		public T computeKey(T symbol) { return symbol; }
		public boolean equals(T o1,T o2) { return o1.equals(o2); }
	}
	public static enum HASHING_STRATEGIES {
		hashCode,
		identity
	}


	protected ConcurrentHashMap<Object,Integer> symbol2Id = new ConcurrentHashMap<Object,Integer>();
	protected ConcurrentHashMap<Integer,T> id2symbol  = new ConcurrentHashMap<Integer,T>();
	protected HashingStrategy<T> hashingStrategy;
	protected int nextId = 0;
	
	public ConcurrentSymbolTable(HashingStrategy<T> strategy) {
		this.init(strategy);
	}
	public ConcurrentSymbolTable(HASHING_STRATEGIES h) {
		switch(h) {
		case hashCode: init(new DefaultHashingStrategy<T>()); break;
		case identity: init(new IdentityHashingStrategy<T>()); break;
		}
	}
	public ConcurrentSymbolTable() {
		this(HASHING_STRATEGIES.hashCode);
	}
	private void init(HashingStrategy<T> strategy) {

		this.hashingStrategy = strategy==null? new DefaultHashingStrategy<T>() : strategy;
	}

	/**
	 * Ensure that a 'symbol' is in the table.
	 *
	 * @param symbol
	 */
	public void insert(T symbol) {
		Object h = hashingStrategy.computeKey(symbol);
		if (symbol2Id.containsKey(h)) {
			//check collision
			if (id2symbol.get(symbol2Id.get(h)).equals(symbol)) return;
		}
		synchronized(this) {
			if (!symbol2Id.containsKey(h)) {
				int newId = ++nextId;
				symbol2Id.put(h,newId);
				id2symbol.put(newId,symbol);
			}
		}
	}
	
	/**
	 * Return the numeric id, between 1 and N, of a symbol, inserting it if
	 * needed.
	 *
	 * @param symbol
	 */
	public int getId(T symbol) {
		insert(symbol);
		Object h = hashingStrategy.computeKey(symbol);
		return symbol2Id.get(h);
	}

	
	/** Test if the symbol has been previously inserted.
	 */
	public boolean hasId(T symbol) {
		Object h = hashingStrategy.computeKey(symbol);
		return symbol2Id.containsKey(h);
	}

	/** Get the symbol that corresponds to an id.  Returns null of the
	 * symbol has not yet been inserted.
	 */
	public T getSymbol(int id) {
		return this.id2symbol.get(id);
	}

	/** Return N, the largest id.
	 */
	public int size() {
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
		for (int i=1; i<=stab.size(); i++) {
			System.out.println(i + ":\t" + stab.getSymbol(i));
		}
	}
}

