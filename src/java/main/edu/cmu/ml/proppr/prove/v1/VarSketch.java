package edu.cmu.ml.proppr.prove.v1;

/**
 * The set of variables that appear in a bunch of related goals,
    assuming the goals have been normalized to have variables with ids
    in a dense range of 1..N
 * @author krivard
 *
 */
public class VarSketch {

	protected int n = 0;
	public void includeAll(Goal[] goals) {
		for (Goal g : goals) include(g);
	}

	public void include(Goal g) {
		for (Argument a : g.getArgs()) {
			if (a.isVariable()) this.n = Math.max(n,-a.getValue());
		}
	}
	
//	public void include(RenamingSubstitution theta) {
//		this.n = Math.max(n, theta.offset); //hack
//	}

	public int size() {
		return n;
	}
	
	@Override
	public String toString() {
		return String.valueOf(n);
	}
	
/*
 * class varSketch(object):
    """""" 

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
 */
}
