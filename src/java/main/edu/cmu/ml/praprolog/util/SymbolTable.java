package edu.cmu.ml.praprolog.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cmu.ml.praprolog.prove.wam.Argument;

/**
 * A symbol table mapping strings to/from integers in the range
    1..N inclusive.
 * @author wcohen,krivard
 *
 */
public class SymbolTable<T> {
	protected List<T> symbolList = new ArrayList<T>();
	protected int nextId = 0;
	protected Map<T,Integer> idDict = new HashMap<T,Integer>();
//	public SymbolTable() {
//		this(new Argument[0]);
//	}
//	public SymbolTable(Argument ... initSymbols) {
//		for (Argument s : initSymbols) this.insert(s.getName()); // FIXME this might not work
//	}
	/**
	 * Insert a symbol.
	 * @param s
	 */
	public void insert(T symbol) {
		if (!this.idDict.containsKey(symbol)) {
			this.nextId += 1;
			this.idDict.put(symbol,this.nextId);
			this.symbolList.add(symbol);
		}
	}
	/**
	 * Get the numeric id, between 1 and N, of a symbol
	 * @param symbol
	 * @return
	 */
	public int getId(T symbol) {
		this.insert(symbol);
		// FIXME this may be slow
		return this.idDict.get(symbol);
	}
	public boolean hasId(T symbol) {
		return this.idDict.containsKey(symbol);
	}
	public T getSymbol(int id) {
		return this.symbolList.get(id-1);
	}
	public List<T> getSymbolList() {
		return this.symbolList;
	}
	public int size() {
		return this.symbolList.size();
	}
}
