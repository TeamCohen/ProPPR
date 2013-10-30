package edu.cmu.ml.praprolog.util.tuprolog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;

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
import edu.cmu.ml.praprolog.util.Dictionary;

public class TuprologAdapter {
	private static final Logger log = Logger.getLogger(TuprologAdapter.class);
	private static final String VARSTART = "X";
	/**
	 * Use the prolog expression copy_term to generate a copy of the expression with anonymous variables.
	 * @param t
	 * @return
	 */
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
	/**
	 * Convert ProPPR lpState to the tuProlog equivalent.
	 * @param s
	 * @return
	 */
	public static LogicProgramState propprToTuprolog(ProPPRLogicProgramState s) {
		Struct queryGoals, goals;
		ProPPRLogicProgramState restart = new ProPPRLogicProgramState(s.getOriginalGoals());
		queryGoals = goalArrayToTerm(s.getQueryGoals());
		goals = goalArrayToTerm(s.getGoals());
		return new TuprologLogicProgramState(queryGoals,goals,restart);
	}
	/**
	 * Convert a tuProlog lpState to the ProPPR equivalent, filling an anonymous variable index.
	 * @param state
	 * @return
	 */
	public static LogicProgramState tuprologToProppr(TuprologLogicProgramState state) {
		return tuprologToProppr_index(state,new VarData());
	}
	/** 
	 * Convert a tuProlog lpState to the ProPPR equivalent, filling the specified index.
	 * @param state
	 * @param data
	 * @return
	 */
	public static LogicProgramState tuprologToProppr_index(TuprologLogicProgramState state, VarData data) {
//		Goal[] queryGoals, goals, origGoals;
//		queryGoals = termToGoalArray(tuprologLogicProgramState.getQueryGoals());
//		goals = termToGoalArray(tuprologLogicProgramState.getGoals());
//		origGoals = tuprologLogicProgramState.getOriginalGoals();
		
		RenamingSubstitution theta = new RenamingSubstitution(0);
		log.debug("tu -> proppr");
		log.debug(state.getQueryGoals());
		log.debug(state.getGoals());
		tuprolog_index(state, data);

		log.debug((data.maxVar-1)+" vars");
		log.debug(data.maxArg+" args");
		return tuprologToProppr(state, data);
		//new ProPPRLogicProgramState(origGoals, queryGoals, goals, new RenamingSubstitution(0), 0); // FIXME  ???
	}
	/**
	 * Fill the specified index according to the tuProlog lpState.
	 * @param state
	 * @param data
	 * @return
	 */
	public static VarData tuprolog_index(TuprologLogicProgramState state, VarData data) {
		Struct queryGoals=state.getQueryGoals(), goals=state.getGoals();
		collectVariables(queryGoals, data);
		collectVariables(goals, data);
		return data;
	}
	/**
	 * Convert a tuProlog lpState to the ProPPR equivalent, using a pre-filled index.
	 * @param state
	 * @param data
	 * @return
	 */
	public static LogicProgramState tuprologToProppr(TuprologLogicProgramState state, VarData data) {
		Goal[] queryGoalsArr = termToGoalArray(state.getQueryGoals(), data);
		Goal[] goalsArr = termToGoalArray(state.getGoals(), data);
		ProPPRLogicProgramState ret = new ProPPRLogicProgramState(state.getOriginalGoals(), queryGoalsArr, goalsArr, new RenamingSubstitution(0), -1);
		// FIXME *depth*
		log.debug(state);
		log.debug(ret);
		log.debug(Dictionary.buildString(ret.getOriginalGoals(), new StringBuilder(), ", ").toString());
		return ret;
	}
	protected static void collectVariables(Struct goalList, VarData data) {
		for(Iterator<? extends Term> it = goalList.listIterator(); it.hasNext(); ) {
			Term t = it.next();
			if (!(t instanceof Struct)) throw new IllegalStateException("not a list of structs! "+t.toString());
			Struct g = (Struct) t;
			for (int i=0; i<g.getArity(); i++) {
				Term t_ai = g.getArg(i).getTerm();
				Argument ai = termToArg(t_ai);
				if (ai==null) { Var v = (Var) t_ai; if (!data.index.containsKey(v)) data.index.put(v,data.maxVar++); }
				else if (ai.isVariable()) { data.maxArg = Math.max(data.maxArg, -ai.getValue()); }
			}
		}
	}
	public static class VarData {
		public int maxVar, maxArg;
		public HashMap<Var,Integer> index;
		public VarData(int maxVar, int maxArg) { this.maxVar = maxVar; this.maxArg = maxArg; this.index = new HashMap<Var,Integer>(); }
		public VarData() { this(1,0); }
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
		return new Struct("state",s.getQueryGoals(), s.getGoals(), goalArrayToTerm(s.getOriginalGoals()));
	}
	public static Struct goalArrayToTerm(Goal[] gs) {
		Term[] args = new Term[gs.length];
		for (int i=0; i<gs.length; i++) {
			args[i] = goalToTerm(gs[i]);
		}
		return new Struct(args);
	}
	public static Goal[] termToGoalArray(Term t) { return termToGoalArray(t,null); }
	public static Goal[] termToGoalArray(Term t,VarData data) {
		if (! (t instanceof Struct)) throw new IllegalArgumentException("Term must be a list");
		Struct s = (Struct) t;
		if (!s.isList()) throw new IllegalArgumentException("Struct must be a list");
		Goal[] ret = new Goal[s.listSize()];
		int i=0;
		for (Iterator<? extends Term> it = s.listIterator(); it.hasNext(); i++) {
			Term gi = it.next();
			ret[i] = termToGoal(gi,data);
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
	public static Goal termToGoal(Term term) { return termToGoal(term,null); }
	public static Goal termToGoal(Term term, VarData data) {
		if (! (term instanceof Struct)) throw new IllegalStateException("Term not a Struct: "+term);
		Struct struct = (Struct) term;
		String functor = struct.getName();
		Argument[] args = new Argument[struct.getArity()];
		for (int i=0; i<args.length; i++) {
			Term argTerm = struct.getArg(i);
			args[i] = termToArg(argTerm, data);
			if (args[i] == null) throw new IllegalStateException("Couldn't convert term to goal without bindings: "+term);
		}
		return new Goal(functor,args);
	}
	public static Term argToTerm(Argument a) {
		if (a.isConstant()) return new Struct(a.getName());
		else if (a.isVariable()) return new Var(VARSTART+(-a.getValue()));
		else throw new IllegalArgumentException("Arguments must be constant or variable; is neither: "+a);
	}
	public static Argument termToArg(Term argTerm) { return termToArg(argTerm,null); }
	public static Argument termToArg(Term argTerm, VarData data) {
		if (argTerm instanceof Var) {
			if (argTerm.getTerm() == argTerm) {
				String name = ((Var)argTerm).getName();
				if (name.startsWith(VARSTART)) // X1
					return new VariableArgument(-Integer.parseInt( name.substring(1) ));
				else { // _24897234
					// well, nuts. Need a new variable here.
					if (data == null) return null;
					return new VariableArgument( -(data.maxArg+data.index.get(argTerm)) );
				}
			} else // v[c[]]
				return termToArg(argTerm.getTerm(), data);
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
	public static Outlink termsToOutlink(Term solution, Term features, TuprologLogicProgramState parent) {
		return new Outlink(termToFeatures(features), TuprologLogicProgramState.fromState(solution, parent));
	}
}
