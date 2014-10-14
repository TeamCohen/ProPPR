package edu.cmu.ml.praprolog.prove.v1;

import java.util.HashMap;
import java.util.Map;

import edu.cmu.ml.praprolog.graph.GraphWriter;
import edu.cmu.ml.praprolog.util.Dictionary;
import edu.cmu.ml.praprolog.util.SymbolTable;

public abstract class Prover {
    public static Map<String,Double> filterSolutions(Map<LogicProgramState,Double> vec) {
        Map<String,Double> result = new HashMap<String,Double> ();
        for (Map.Entry<LogicProgramState,Double> s : vec.entrySet()) {
            if (s.getKey().isSolution()) Dictionary.increment(result, s.getKey().description(), s.getValue(),"(elided)");
        }
        return result;
        /*
         *    def filterSolutions(vec):
        filteredVec = collections.defaultdict(float)
        for s,w in vec.items():
            if s.isSolution():
                filteredVec[s.description()] += w
        return filteredVec
         */
    }
    
    
	public static LogicProgramState parseQuery(String goal, String ... args) {
		Argument[] a = new Argument[args.length];
		//compile the goal
		SymbolTable variableSymTab = new SymbolTable();
		for (int i=0; i<args.length; i++) {
			if (args[i].startsWith("_") || args[i].matches("[A-Z].*")) a[i] = new VariableArgument(-variableSymTab.getId(args[i]));
			else a[i] = new ConstantArgument(args[i]);
		}
		Goal g = new Goal(goal, a);
		Goal[] goals = {g};
		LogicProgramState result = new ProPPRLogicProgramState(goals);
		return result;
	}

	/**
	 * Utility - find renormalized vector of solutions to a string query.
	 * @param lp
	 * @param goal
	 * @param args
	 * @return
	 */
	public Map<String,Double> solutionsForQuery(LogicProgram lp, String goal, String ... args) {
	    Map<LogicProgramState,Double> dist = this.proveState(lp, parseQuery(goal,args));
	    Map<String,Double> solutionDist = filterSolutions(dist);
	    return Dictionary.normalize(solutionDist);
	}
	/*
	 *   def solutionsForQueryString(self,lp,com):
        """"""
        dist = self.proveState(lp,self.parseQuery(com))
        #print "Dist:",dist
        solutionDist = self.filterSolutions(dist)
        #print "solutionDist:",solutionDist
        util.normalizeVector(solutionDist)
        return solutionDist
	 */
	public Map<LogicProgramState, Double> proveState(LogicProgram lp, LogicProgramState state0) {
		return this.proveState(lp, state0, null);
	}
	public abstract Map<LogicProgramState, Double> proveState(LogicProgram lp, LogicProgramState state0, GraphWriter w);
	public abstract Prover copy();
}
