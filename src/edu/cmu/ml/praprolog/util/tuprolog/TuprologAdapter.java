package edu.cmu.ml.praprolog.util.tuprolog;

import java.util.HashMap;
import java.util.Map;

import alice.tuprolog.NoSolutionException;
import alice.tuprolog.Prolog;
import alice.tuprolog.SolveInfo;
import alice.tuprolog.Struct;
import alice.tuprolog.Term;
import alice.tuprolog.Var;
import edu.cmu.ml.praprolog.prove.Argument;
import edu.cmu.ml.praprolog.prove.Component.Outlink;
import edu.cmu.ml.praprolog.prove.ConstantArgument;
import edu.cmu.ml.praprolog.prove.Goal;
import edu.cmu.ml.praprolog.prove.LogicProgramState;
import edu.cmu.ml.praprolog.prove.ProPPRLogicProgramState;
import edu.cmu.ml.praprolog.prove.TuprologLogicProgramState;
import edu.cmu.ml.praprolog.prove.VariableArgument;

public class TuprologAdapter {
	public static Term copy(Term t) {
		Prolog engine = new Prolog();
		SolveInfo info = engine.solve(new Struct("copy_term",t,new Var("S")));
		if (info.isSuccess()) {
			try {
				return info.getVarValue("S");
			} catch (NoSolutionException e) {
				e.printStackTrace();
			}
		} 
		return null;
	}
	// gross. can't java be clever about this?
	public static Term lpStateToTerm(LogicProgramState s) {
		if (s instanceof ProPPRLogicProgramState) return lpStateToTerm((ProPPRLogicProgramState) s);
		if (s instanceof TuprologLogicProgramState) return lpStateToTerm((TuprologLogicProgramState) s);
		throw new IllegalArgumentException("Unknown lpState type!");
	}
	public static Term lpStateToTerm(ProPPRLogicProgramState s) {
		Term queryGoals, goals, origGoals;
		queryGoals = goalArrayToTerm(s.getQueryGoals());
		goals = goalArrayToTerm(s.getGoals());
		origGoals = goalArrayToTerm(s.getOriginalGoals());
		Term copy = copy(origGoals);
		if (copy != null) return new Struct("state",queryGoals,goals,copy);
		throw new IllegalStateException("Couldn't copy goals :(");
	}
	public static Term lpStateToTerm(TuprologLogicProgramState s) {
		return new Struct("state",s.getQueryGoals(), s.getGoals(), s.getOriginalGoals());
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
		else if (a.isVariable()) return new Var("X"+(-a.getValue()));
		else throw new IllegalArgumentException("Arguments must be constant or variable; is neither: "+a);
	}
	public static Map<Goal,Double> termToFeatures(Term f) {
//		System.out.println("feature term: "+f.toString());
		HashMap<Goal,Double> ret = new HashMap<Goal,Double>();
		Struct s0 = (Struct) f; // ['='(base,1)]
		Struct s1 = (Struct) s0.getArg(0); // '='(base,1)
		Struct s2 = (Struct) s1.getArg(0).getTerm(); // base
		ret.put(new Goal(s2.getName()),1.0);
		return ret;
	}
	public static Outlink termsToOutlink(Term solution, Term features) {
		return new Outlink(termToFeatures(features), TuprologLogicProgramState.fromState(solution));
	}
	public static Goal termToGoal(Term term) {
		if (! (term instanceof Struct)) throw new IllegalStateException("Head of list not a Struct: "+term);
		Struct struct = (Struct) term;
		String functor = struct.getName();
		Argument[] args = new Argument[struct.getArity()];
		for (int i=0; i<args.length; i++) {
			Term argTerm = struct.getArg(i);
			if (argTerm instanceof Var) {
				args[i] = new VariableArgument(Integer.parseInt( ((Var)argTerm).getName().substring(1) ));
			} else if (argTerm instanceof Struct) {
				args[i] = new ConstantArgument( ((Struct)argTerm).getName() );
			} else throw new IllegalStateException("Argument "+i+" neither Var nor Struct: "+argTerm);
		}
		return new Goal(functor,args);
		
	}
}
