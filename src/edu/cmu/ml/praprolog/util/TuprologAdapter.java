package edu.cmu.ml.praprolog.util;

import alice.tuprolog.NoSolutionException;
import alice.tuprolog.Prolog;
import alice.tuprolog.SolveInfo;
import alice.tuprolog.Struct;
import alice.tuprolog.Term;
import alice.tuprolog.Var;
import edu.cmu.ml.praprolog.prove.Argument;
import edu.cmu.ml.praprolog.prove.Goal;
import edu.cmu.ml.praprolog.prove.LogicProgramState;

public class TuprologAdapter {
	public static Term lpStateToTerm(LogicProgramState s) {
		Term queryGoals, goals, origGoals;
		queryGoals = goalArrayToTerm(s.getQueryGoals());
		goals = goalArrayToTerm(s.getGoals());
		origGoals = goalArrayToTerm(s.getOriginalGoals());
		Prolog engine = new Prolog();
		SolveInfo info = engine.solve(new Struct("copy_term",origGoals,new Var("S")));
		if (info.isSuccess()) {
			try {
				return new Struct("state",queryGoals,goals,info.getVarValue("S"));
			} catch (NoSolutionException e) {
				e.printStackTrace();
			}
		} 
		throw new IllegalStateException("Couldn't copy goals :(");
	}
	public static Term goalArrayToTerm(Goal[] gs) {
		Term[] args = new Term[gs.length];
		for (int i=0; i<gs.length; i++) {
			args[i] = goalToTerm(gs[i]);
		}
		return new Struct(args);
	}
	public static Term goalToTerm(Goal g) {
		Term[] args = new Term[g.getArgs().length];
		for (int i=0; i<g.getArgs().length; i++) {
			args[i] = argToTerm(g.getArg(i));
		}
		return new Struct(g.getFunctor(),args);
	}
	public static Term argToTerm(Argument a) {
		if (a.isConstant()) return new Struct(a.getName());
		else if (a.isVariable()) return new Var("_"+(-a.getValue()));
		else throw new IllegalArgumentException("Arguments must be constant or variable; is neither: "+a);
	}
}
