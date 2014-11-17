import logging
#symbol table - map strings to/from the range 1..N inclusive.

class SymbolTable(object):
    """A symbol table mapping strings to/from integers in the range
    1..N inclusive."""

    def __init__(self,initSymbols=[]):
        self._symbolList = [None]
        self._nextId = 0
        self._idDict = {}
        for s in initSymbols: 
            self.insert(s)

    def insert(self,symbol):
        """Insert a symbol."""
        #logging.debug( "symtab: %s" % symbol)
        if symbol not in self._idDict:
            self._nextId += 1
            self._idDict[symbol] = self._nextId
            self._symbolList += [symbol]

    def getSymbolList(self):
        """Get an array of all defined symbols."""
        return self._symbolList[1:]

    def getSymbol(self,id):
        return self._symbolList[id]

    def hasId(self,symbol):
        return symbol in self._idDict

    def getId(self,symbol):
        """Get the numeric id, between 1 and N, of a symbol"""
        self.insert(symbol)
        return self._idDict[symbol]

