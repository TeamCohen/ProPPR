package edu.cmu.ml.praprolog.util.tuprolog;

import java.util.HashMap;
import java.util.Iterator;
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
import edu.cmu.ml.praprolog.prove.RenamingSubstitution;
import edu.cmu.ml.praprolog.prove.TuprologLogicProgramState;
import edu.cmu.ml.praprolog.prove.VariableArgument;

public class TuprologAdapter {
	private static final String VARSTART = "X";
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
	public static LogicProgramState propprToTuprolog(ProPPRLogicProgramState s) {
		Struct queryGoals, goals, origGoals;
		queryGoals = goalArrayToTerm(s.getQueryGoals());
		goals = goalArrayToTerm(s.getGoals());
		origGoals = goalArrayToTerm(s.getOriginalGoals());
		Struct copy = (Struct) copy(origGoals);
		if (copy != null) return new TuprologLogicProgramState(queryGoals,goals,copy);
		throw new IllegalStateException("Couldn't copy goals :(");
	}
	public static LogicProgramState tuprologToProppr(TuprologLogicProgramState tuprologLogicProgramState) {
		Goal[] queryGoals, goals, origGoals;
		queryGoals = termToGoalArray(tuprologLogicProgramState.getQueryGoals());
		goals = termToGoalArray(tuprologLogicProgramState.getGoals());
		origGoals = termToGoalArray(tuprologLogicProgramState.getOriginalGoals());
		
		return new ProPPRLogicProgramState(origGoals, queryGoals, goals, new RenamingSubstitution(0), 0); // FIXME  ???
	}
	// gross. can't java be clever about this?
	public static Term lpStateToTerm(LogicProgramState s) {
		if (s instanceof ProPPRLogicProgramState) return lpStateToTerm((ProPPRLogicProgramState) s);
		if (s instanceof TuprologLogicProgramState) return lpStateToTerm((TuprologLogicProgramState) s);
		throw new IllegalArgumentException("Unknown lpState type!");
	}
	public static Struct lpStateToTerm(ProPPRLogicProgramState s) {
		Term queryGoals, goals, origGoals;
		queryGoals = goalArrayToTerm(s.getQueryGoals());
		goals = goalArrayToTerm(s.getGoals());
		origGoals = goalArrayToTerm(s.getOriginalGoals());
		Term copy = copy(origGoals);
		if (copy != null) return new Struct("state",queryGoals,goals,copy);
		throw new IllegalStateException("Couldn't copy goals :(");
	}
	public static Struct lpStateToTerm(TuprologLogicProgramState s) {
		return new Struct("state",s.getQueryGoals(), s.getGoals(), s.getOriginalGoals());
	}
	public static Struct goalArrayToTerm(Goal[] gs) {
		Term[] args = new Term[gs.length];
		for (int i=0; i<gs.length; i++) {
			args[i] = goalToTerm(gs[i]);
		}
		return new Struct(args);
	}
	public static Goal[] termToGoalArray(Term t) {
		if (! (t instanceof Struct)) throw new IllegalArgumentException("Term must be a list");
		Struct s = (Struct) t;
		if (!s.isList()) throw new IllegalArgumentException("Struct must be a list");
		Goal[] ret = new Goal[s.listSize()];
		int i=0;
		for (Iterator<? extends Term> it = s.listIterator(); it.hasNext(); i++) {
			Term gi = it.next();
			ret[i] = termToGoal(gi);
		}
		return ret;
	}
	public static Struct goalToTerm(Goal g) {
		Term[] args = new Term[g.getArgs().length];
		for (int i=0; i<g.getArgs().length; i++) {
			args[i] = argToTerm(g.getArg(i));
		}
		return new Struct(g.getFunctor(),args);
	}
	public static Goal termToGoal(Term term) {
		if (! (term instanceof Struct)) throw new IllegalStateException("Term not a Struct: "+term);
		Struct struct = (Struct) term;
		String functor = struct.getName();
		Argument[] args = new Argument[struct.getArity()];
		for (int i=0; i<args.length; i++) {
			Term argTerm = struct.getArg(i);
			args[i] = termToArg(argTerm);
		}
		return new Goal(functor,args);
	}
	public static Term argToTerm(Argument a) {
		if (a.isConstant()) return new Struct(a.getName());
		else if (a.isVariable()) return new Var(VARSTART+(-a.getValue()));
		else throw new IllegalArgumentException("Arguments must be constant or variable; is neither: "+a);
	}
	public static Argument termToArg(Term argTerm) {
		if (argTerm instanceof Var) {
			if (argTerm.getTerm() == argTerm) {
				String name = ((Var)argTerm).getName();
				if (name.startsWith(VARSTART)) // X1
					return new VariableArgument(Integer.parseInt( name.substring(1) ));
				else { // _24897234
					// well, nuts. Need a new variable here.
					return new VariableArgument( 1000 ); //FIXME
				}
			} else // v[c[]]
				return termToArg(argTerm.getTerm());
		} else if (argTerm instanceof Struct) {
			return new ConstantArgument( ((Struct)argTerm).getName() );
		} else throw new IllegalStateException("Argument neither Var nor Struct: "+argTerm);
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
}
