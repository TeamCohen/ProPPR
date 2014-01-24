package edu.cmu.ml.praprolog.prove;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.cmu.ml.praprolog.prove.Argument;
import edu.cmu.ml.praprolog.util.Dictionary;
import edu.cmu.ml.praprolog.util.ParsedFile;
import edu.cmu.ml.praprolog.util.SymbolTable;

public class GoalComponent extends Component {
	private static final Logger log = Logger.getLogger(GoalComponent.class);
	public static final String FILE_EXTENSION=".cfacts";
	protected Map<Goal,Double> featureDict = new HashMap<Goal,Double>();
	protected Map<FunctorArityKey,List<Goal>> indexF = new HashMap<FunctorArityKey,List<Goal>>();
	protected Map<FunctorArityArgKey,List<Goal>> indexFA1 = new HashMap<FunctorArityArgKey,List<Goal>>();
	protected String label;
	public GoalComponent() {
		this("goalComponent");
	}
	public GoalComponent(String label) {
		label = Component.cleanLabel(label);
		featureDict.put(new Goal("id",label), 1.0);
		this.label = label;
	}
	/**
	 * Add a fact to this EDB.
	 * @param goal
	 */
	public void addFact(Goal goal) {
		if (goal.getArity() < 1) throw new IllegalArgumentException("Goal arity must be >= 1 for goal "+goal.toString());
		for (Argument a : goal.getArgs()) {
			if (!a.isConstant()) throw new IllegalArgumentException("Goal arguments must all be constant for goal "+goal.toString());
		}

		FunctorArityKey keyf = new FunctorArityKey(goal.getFunctor(), goal.getArity());
		FunctorArityArgKey keyfa1 = new FunctorArityArgKey(goal.getFunctor(), goal.getArity(), goal.getArg(0));

		Dictionary.safeAppend(this.indexF, keyf, goal);
		Dictionary.safeAppend(this.indexFA1, keyfa1, goal);
	}

	public static class FunctorArityKey {
		public final String functor;
		public final int arity;
		public FunctorArityKey(String functor, int arity) {
			this.functor = functor;
			this.arity = arity;
		}
		public int hashCode() {
			return functor.hashCode() << 16 + arity;
		}
		public boolean equals(Object o) {
			if (!(o instanceof FunctorArityKey)) return false;
			FunctorArityKey k = (FunctorArityKey) o;
			return this.functor.equals(k.functor) && this.arity == k.arity;
		}
	}

	public static class FunctorArityArgKey extends FunctorArityKey {
		public final Argument arg;
		public FunctorArityArgKey(String functor, int arity, Argument arg) {
			super(functor, arity);
			this.arg = arg;
		}

		public int hashCode() {
			return super.hashCode() ^ arg.hashCode();
		}
		public boolean equals(Object o) {
			return super.equals(o) && ((FunctorArityArgKey)o).arg.equals(this.arg);
		}
	}

	@Override
	public List<Outlink> outlinks(LogicProgramState state) {
		List<RenamingSubstitution> matches = new ArrayList<RenamingSubstitution>();
		for (Goal g : this.goalsMatching(state.getHeadFunctor(), state.getHeadArity(), state.getHeadArg1())) {
			if (log.isDebugEnabled()) log.debug("trying goal "+g+" on "+state.getHeadGoal()+" with offset "+0);
			RenamingSubstitution theta1 = 
					RenamingSubstitution.unify(g, 
							state.getHeadGoal(), 
							0, 
							RenamingSubstitution.NOT_RENAMED, 
							RenamingSubstitution.NOT_RENAMED);
			if (theta1 != null) {
				if (log.isDebugEnabled()) log.debug("succeeds "+theta1);
				matches.add(theta1);
			}
		}
		List<Outlink> result = new ArrayList<Outlink>();
		if (matches.isEmpty()) return result;
		ProPPRLogicProgramState stateP = (ProPPRLogicProgramState) state.asProPPR();
		for (RenamingSubstitution theta : matches) {
			result.add(new Outlink(this.featureDict,stateP.child(theta)));
		}
		return result;
	}
	private Iterable<Goal> goalsMatching(Goal goal) {
		if (goal.getArity() == 0 || goal.getArg(0).isVariable()) 
			return Dictionary.safeGet(this.indexF,
					new FunctorArityKey(goal.getFunctor(), goal.getArity()),
					Collections.<Goal> emptyList());
		return Dictionary.safeGet(this.indexFA1, 
				new FunctorArityArgKey(goal.getFunctor(), goal.getArity(), goal.getArg(0)),
				Collections.<Goal> emptyList());
	}
	private Iterable<Goal> goalsMatching(String functor, int arity, Argument arg1) {
		if (arity == 0 || arg1.isVariable()) 
			return Dictionary.safeGet(this.indexF,
					new FunctorArityKey(functor, arity),
					Collections.<Goal> emptyList());
		return Dictionary.safeGet(this.indexFA1, 
				new FunctorArityArgKey(functor, arity, arg1),
				Collections.<Goal> emptyList());
	}
	@Override
	public boolean claim(LogicProgramState state) {
		// FIXME -- only works b/c we know isSolution is true iff #goals == 0
		return !state.isSolution() && this.contains(state.getHeadFunctor(), state.getHeadArity());
	}
	protected boolean contains(Goal goal) {
		// TODO: this is likely slow
		return this.indexF.containsKey(new FunctorArityKey(goal.getFunctor(),goal.getArity()));
	}
	protected boolean contains(String functor, int arity) {
		// TODO: this is likely slow
		return this.indexF.containsKey(new FunctorArityKey(functor,arity));
	}

	public void compile() {
		this.compile(new SymbolTable());
	}
	public void compile(SymbolTable variableSymTab) {
		for (List<Goal> el : this.indexF.values()) {
			for (Goal g : el) {
				g.compile(variableSymTab);
			}
		}
	}
	public String toString() {
		StringBuilder sb = new StringBuilder("goalComponent:");
		//		for (List<Goal> el : this.indexF.values()) {
		//			for (Goal g : el) {
		//				sb.append("\n\t").append(g);
		//			}
		//		}

		for (FunctorArityArgKey k : this.indexFA1.keySet()) {
			//			for (Goal g : G) {
			//				sb.append("\n").append(g);
			//			}
			sb.append("\n\t").append(k.functor).append("/").append(k.arity).append(":").append(k.arg);
		}
		return sb.toString();
	}

	/**
	 * Returns a goal component loaded from a file, where each line contains
        a single ground goal, stored as functor <TAB> arg1 <TAB> .....
	 * @param filename
	 */
	public static GoalComponent loadCompiled(String filename) {
		GoalComponent result = new GoalComponent(filename);
		ParsedFile file = new ParsedFile(filename);
		for (String line : file) {
			result.addFact(Goal.parseGoal(line,"\t"));//new Goal(functor_args[0],functor_args[1].split("\t")));
		}
		return result;
		/*
		 *     def loadCompiled(inputFile):
        """"""
        result = goalComponent(label=inputFile)
        for line in util.linesOf(inputFile):
            line = line.strip()
            if not line.startswith("#") and line:
                parts = line.split("\t")
                functor = parts[0]
                args = parts[1:]
                result.addFact(goal(functor,args))
        return result
		 */
	}

	@Override
	public String listing() {
		StringBuilder sb = new StringBuilder("%% from goalComponent ").append(this.label).append(":");
		for (List<Goal> G : this.indexF.values()) {
			for (Goal g : G) {
				sb.append("\n").append(g);
			}
			//			sb.append("\n").append(k.functor).append("/").append(k.arity).append(":").append(k.arg);
		}
		return sb.toString();
	}
}
