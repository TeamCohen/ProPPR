package edu.cmu.ml.praprolog.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cmu.ml.praprolog.prove.Argument;
import edu.cmu.ml.praprolog.prove.VariableArgument;

/**
 * A symbol table mapping strings to/from integers in the range
    1..N inclusive.
 * @author wcohen,krivard
 *
 */
public class SymbolTable {
	protected List<String> symbolList = new ArrayList<String>();
	protected int nextId = 0;
	protected Map<String,Integer> idDict = new HashMap<String,Integer>();
	public SymbolTable() {
		this(new Argument[0]);
	}
	public SymbolTable(Argument ... initSymbols) {
		for (Argument s : initSymbols) this.insert(s.getName()); // FIXME this might not work
	}
/*

    def __init__(self,initSymbols=[]):
        self._symbolList = [None]
        self._nextId = 0
        self._idDict = {}
        for s in initSymbols: 
            self.insert(s)

    def insert(self,symbol):
        """"""
        if symbol not in self._idDict:
            self._nextId += 1
            self._idDict[symbol] = self._nextId
            self._symbolList += [symbol]

    def getSymbolList(self):
        """Get an array of all defined symbols."""
        return self._symbolList[1:]

    def hasId(self,symbol):
        return symbol in self._idDict

    def getId(self,symbol):
        """Get the numeric id, between 1 and N, of a symbol"""
        self.insert(symbol)
        return self._idDict[symbol]

 */
	/**
	 * Insert a symbol.
	 * @param s
	 */
	public void insert(String symbol) {
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
	public Argument getId(String symbol) {
		this.insert(symbol);
		// FIXME this may be slow
		return new VariableArgument(-this.idDict.get(symbol));
	}
	public void insert(Argument a) {
		this.insert(a.getName()); // FIXME may need to check constant/variable
	}
	public Argument getId(Argument a) {
		return getId(a.getName()); // FIXME may need to check constant/variable
	}
}
