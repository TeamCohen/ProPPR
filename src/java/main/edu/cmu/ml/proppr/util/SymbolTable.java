package edu.cmu.ml.proppr.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cmu.ml.proppr.prove.wam.Argument;
import gnu.trove.map.hash.TObjectIntHashMap;

/**
 * A symbol table mapping strings to/from integers in the range
    1..N inclusive.
 * @author wcohen,krivard
 *
 */
public class SymbolTable<T> {
	protected List<T> symbolList = new ArrayList<T>();
	protected int nextId = 0;
	protected TObjectIntHashMap<T> idDict = new TObjectIntHashMap<T>();
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
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("SymbolTable:");
		for (int i=0; i<this.symbolList.size(); i++) {
			sb.append(" ").append(this.symbolList.get(i)).append(":").append(i+1);
		}
		return sb.toString();
	}
}
