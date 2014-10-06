import collections
import sys
import string

import util
import symtab as syt
#import components as lpc

##############################################################################
## a parser
##############################################################################

from pyparsing import Word, CharsNotIn, alphas, alphanums, delimitedList, nestedExpr, Optional, Group, QuotedString

atomNT = Word( alphanums+"_" ) |  QuotedString(quoteChar="'",escChar="\\")
simpleGoalNT = atomNT + Optional("(" + delimitedList(atomNT) + ")")
goalNT = simpleGoalNT | "[" + simpleGoalNT.setResultsName("nested") + "]"
goalListNT = Optional(delimitedList(Group(goalNT)))
featureNT = simpleGoalNT
featureListNT = delimitedList(Group(featureNT))
ruleNT = simpleGoalNT("lhs") + ":-" + goalListNT("rhs") + Optional("#" + featureListNT("features")) + "."

class parser(object):

	"""Parser for this BNF:

	atom --> QuotedString | [a-zA-Z0-9_]+
	delimitedListOfAtoms --> atom (',' atom)*
	simpleGoal --> atom [ '(' delimitedListOfAtoms ')' ]
	goal --> simpleGoal | '[' simpleGoal ']'
	delimitedListOfGoals --> ( goal (',' goal)* )?
	feature --> simpleGoal
	delimitedListOfFeatures --> feature (',' feature)*
	rule --> simpleGoal ':-' delimitedListOfGoals ('#' delimitedListOfFeatures)? '.'

	Right now, I'm not using the [p(X,Y)] notation for anything - they
	are mapped to plain old p(X,Y) goals. It might be used later for
	hard constraints or hints.
	"""

	@staticmethod
	def isVariable(atom):
		"""Tests if an atom is a variable, where variables start with
		uppercase letters or underscores."""
		return atom[0].isupper() or atom[0]=='_'

	@staticmethod
	def isConstant(atom):
		"""Tests if an atom is a constant, where variables start with
		uppercase letters or underscores."""
		return not parser.isVariable(atom)

	@staticmethod
	def _convertGoal(ptree):
		isHard = False
		# ptree could be a list like: [ '[' 'foo' '(' 'X' 'Y' ')' ']' ]
		# which indicates a hard goal....
		if ptree[0]=='[':
			isHard =True
			ptree = ptree[1:-1]
		return goalBuffer(ptree[0], ptree[2:-1], isHard=isHard)

	@staticmethod
	def _convertRule(ptree,rid):
		if 'rhs' in ptree: tmpRhs = map(parser._convertGoal, ptree['rhs'].asList())
		else: tmpRhs = [ ]
		if 'features' in ptree: tmpFeatures = map(parser._convertGoal,ptree['features'].asList())
		else: tmpFeatures = [ goalBuffer('id',[rid],False) ]
		return ruleBuffer(parser._convertGoal(ptree['lhs']), tmpRhs,  tmpFeatures, rid) 

	@staticmethod
	def parseGoal(s):
		"""Convert a string to a goal."""
		return parser._convertGoal(goalNT.parseString(s))
	
	@staticmethod
	def parseGoalList(s):
		"""Convert a string to a goal list."""
		return map(parser._convertGoal, goalListNT.parseString(s).asList())
		
	@staticmethod
	def parseRule(s,rid=None):
		"""Convert a string to a rule."""
		if not rid: rid=s
		return parser._convertRule(ruleNT.parseString(s),rid)

	@staticmethod
	def parseFile(file):	
		"""Extract a series of rules from a file."""
		print 'parsing',file
		buf = ""
		for line in open(file,'r'):
			if not line[0]=='#':
				buf += line
		programBuffer = []
		try:
			k = 0
			for (ptree,lo,hi) in ruleNT.scanString(buf):
				k += 1
				r = parser._convertRule(ptree, "'%s_%02d'" % (file,k))
				programBuffer += [r]
			return programBuffer
		except KeyError:
			print 'error near ',lo,'in',file

class varSketch(object):
    """The set of variables that appear in a bunch of related goals,
    assuming the goals have been normalized to have variables with ids
    in a dense range of 1..N""" 

    def __init__(self,initSize=0): 
        self.n = initSize

    def size(self): 
        return self.n 

    def include(self,goal):
        for a in goal.args:
            if isVariable(a): 
                self.n = max(self.n,-a)

    def includeAll(self,goals):
        for g in goals:
            self.include(g)

class goalBuffer(object):

	def __init__(self,functor,args,isHard):
		self.isHard = isHard
		self.orig_args = args
		#super(goalBuffer,self).__init__(functor,args)
		self.functor = functor
		self._setArgs(args)
		#print "new goal",self
	
	def _setArgs(self,args):
		self.args = args
		self.arity = len(args)
		self._tuple = tuple([self.functor]+args)
	
	def compile(self,variableSymtab=syt.SymbolTable()):
		self.variableSymtab = variableSymtab
		self.origArgs = self.args
		self._setArgs(compileArgs(self.args,variableSymtab))
		return variableSymtab
	
	def __repr__(self):
		return 'goalBuffer(' + repr(self.functor) + ',' + repr(self.args) + 'hardOrSoft=' + self.isHard + ')'

class ruleBuffer(object):
	
	def __init__(self,lhs,rhs,features,ruleId,variableList=string.ascii_uppercase):
		self.ruleId = ruleId
		#self.variableSymtab = syt.SymbolTable()
		#compileGoalToIds(lhs,self.variableSymtab)
		#for g in rhs:
		#	compileGoalToIds(g,self.variableSymtab)
		#for f in features:
		#	compileGoalToIds(f,self.variableSymtab)
		#super(ruleBuffer,self).__init__(lhs,rhs,features)#,self.variableSymtab.getSymbolList())
		#self.varSketch.include(lhs)
		#self.varSketch.includeAll(rhs)
		#self.varSketch.includeAll(features)
		#self.lhs = lhs
		#self.rhs = rhs
		#self.features = features
		self.lhs = lhs
		self.rhs = rhs
		self.features = features # a list of goals, which might contain variables
		self.variableList=variableList
		self.varSketch = varSketch(len(self.variableList))
	def compile(self,variableSymtab = syt.SymbolTable()):
		self.variableSymtab = variableSymtab
		self.lhs.compile(variableSymtab)
		for g in self.rhs:
			g.compile(variableSymtab)
		for f in self.features:
			f.compile(variableSymtab)
	
	def __repr__(self):
		return 'ruleBuffer(' + repr(self.lhs) + ',' + repr(self.rhs) + ',' + repr(self.features) + ',' + repr(self.ruleId)

def compileLPToFile(inputFile,outputFile):
	"""Convert a file containing a logic program to a simple, integer-based
	encoding of that logic program - an encoding that doesn't really need to
	be parsed.

	format is as follows. Each line is a rule in three #-separated
	parts.  Part 1 is the rule itself, as a conjunction of goals - the
	first goal is the LHS/consequent.  Part 2 are the features, as a
	conjunction of goals.  Part 3 are the variables used in the rule,
	in a comma-sep list.  The i-th variable in this list (starting
	from 1) is assigned index -i in Parts 2 and 3.
	
	A conjunction of goals (Parts 2-3 above) is a &-separated list of
	items, each of which encodes a goal.  Each goal is a comma-sep
	list of integers: the first encodes the functor/arity, the
	remainder the arguments of the goal.

	Example, where the decoded rule is after the --> token: 
	
	"""
	lp = parser.parseFile(inputFile)
	fp = open(outputFile,'w')
	for r in lp:
		variableSymtab = syt.SymbolTable()
		fp.write(",".join([str(c) for c in compileGoalToIds(r.lhs,variableSymtab)]))
		for g in r.rhs:
			fp.write(" & " + ",".join([str(c) for c in compileGoalToIds(g,variableSymtab)]))
		fp.write(" # ")			
		separator = ""
		for f in r.features:
			fp.write(separator + ",".join([str(c) for c in compileGoalToIds(f,variableSymtab)]))
			separator = " & "
		fp.write(" # ")			
		fp.write(",".join(variableSymtab.getSymbolList()))
		fp.write("\n")
			
def compileGoalToIds(g,variableSymtab):
	hardIndicator = ''
	if g.isHard: hardIndicator = '+'
	parts = [g.functor + hardIndicator]
	parts.extend(compileArgs(g.args,variableSymtab))
	return parts

def compileArgs(args,variableSymtab):
	parts = []
	for a in args:
		k = a if parser.isConstant(a) else -variableSymtab.getId(a)
		parts += [k] #was: str(k)
	return parts

def compileState(state,variableSymtab = None):
	if not variableSymtab: variableSymtab = syt.SymbolTable()
	for g in state.queryGoals:
		g.compile(variableSymtab)
	if state.theta.size() > 0:
		state.theta.compile(variableSymtab)
	state._freeze()
	return variableSymtab

def compileRule(rule,variableSymtab = None):
	if not variableSymtab: variableSymtab = syt.SymbolTable()
	rule.lhs.compile(variableSymtab)
	for g in rule.rhs:
		g.compile(variableSymtab)
	for g in rule.features:
		g.compile(variableSymtab)
	return variableSymtab
	
def compileComponent(component,variableSymtab = None):
	if not variableSymtab: variableSymtab = syt.SymbolTable()
	for goal in component.goals():
		goal.compile(variableSymtab)
	component._freeze()
	return variableSymtab


if __name__ == "__main__":
	if len(sys.argv)<3:
		print "usage:"
		print "\t",sys.argv[0],"foo.rules foo.crules"
	else:
		print "compiling",sys.argv[1],'to',sys.argv[2]
		compileLPToFile(sys.argv[1],sys.argv[2])
