package edu.cmu.ml.proppr.util;

public interface SymbolTable<T> {

	/**
	 * Insert a symbol.
	 * @param s
	 */
	public abstract void insert(T symbol);

	/**
	 * Get the numeric id, between 1 and N, of a symbol
	 * @param symbol
	 * @return
	 */
	public abstract int getId(T symbol);

	public abstract boolean hasId(T symbol);

	public abstract T getSymbol(int id);

	public abstract int size();

}