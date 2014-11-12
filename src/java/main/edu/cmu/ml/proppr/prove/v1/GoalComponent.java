package edu.cmu.ml.proppr.prove.v1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.prove.v1.Argument;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.ParsedFile;
import edu.cmu.ml.proppr.util.SymbolTable;

public class GoalComponent extends Component {
	private static final Logger log = Logger.getLogger(GoalComponent.class);
	public static final String FILE_EXTENSION=".cfacts";
	public static final boolean DEFAULT_INDICES=false;
	protected Map<Goal,Double> featureDict = new HashMap<Goal,Double>();
	protected Map<FunctorArityKey,List<Goal>> indexF = new HashMap<FunctorArityKey,List<Goal>>();
	protected Map<FunctorArityArgKey,List<Goal>> indexFA1 = new HashMap<FunctorArityArgKey,List<Goal>>();
	protected Map<FunctorArityArgKey,List<Goal>> indexFA2 = new HashMap<FunctorArityArgKey,List<Goal>>();
	protected Map<FunctorArityArg1Arg2Key,List<Goal>> indexFA1A2 = new HashMap<FunctorArityArg1Arg2Key,List<Goal>>();
	// collected stats on how various indexes are used....
	int numUsesGoalsMatching = 0;
	int numUsesIndexF = 0;
	int numUsesIndexFA1 = 0;
	int numUsesIndexFA2 = 0;
	int numUsesIndexFA1A2 = 0;
	boolean useTernaryIndex;
	protected String label;
	public GoalComponent() {
		this("goalComponent");
	}
	public GoalComponent(String label) {
		this(label,DEFAULT_INDICES);
	}
	public GoalComponent(String label, boolean useTernaryIndex) {
		label = Component.cleanLabel(label);
		featureDict.put(new Goal("id",label), 1.0);
		this.label = label;
		this.useTernaryIndex = useTernaryIndex;
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
		if (useTernaryIndex && goal.getArity() > 2) {
			FunctorArityArgKey keyfa2 = new FunctorArityArgKey(goal.getFunctor(), goal.getArity(), goal.getArg(1));
			Dictionary.safeAppend(this.indexFA2, keyfa2, goal);
			FunctorArityArg1Arg2Key keyfa1a2 = new FunctorArityArg1Arg2Key(goal.getFunctor(), goal.getArity(), goal.getArg(0), goal.getArg(1));
			Dictionary.safeAppend(this.indexFA1A2, keyfa1a2, goal);
		}
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

	public static class FunctorArityArg1Arg2Key extends FunctorArityArgKey {
		public final Argument arg2;
		public FunctorArityArg1Arg2Key(String functor, int arity, Argument arg, Argument arg2) {
			super(functor, arity, arg);
			this.arg2 = arg2;
		}

		public int hashCode() {
			return super.hashCode() ^ arg2.hashCode();
		}
		public boolean equals(Object o) {
			return super.equals(o) && ((FunctorArityArg1Arg2Key)o).arg2.equals(this.arg2);
		}
	}

	long lastPrint = System.currentTimeMillis();

	@Override
	public List<Outlink> outlinks(LogicProgramState state) {
		// maybe print stats
		long now = System.currentTimeMillis();
		if (useTernaryIndex && now-lastPrint > 5000) {
			log.info("index usage for F / F,A1 / F,A2 / F,A1,A2: "
					+ numUsesIndexF+"/"+numUsesIndexFA1+"/"+numUsesIndexFA2+"/"+numUsesIndexFA1A2 
					+ " of "+numUsesGoalsMatching);
			lastPrint = now;
		}

		List<RenamingSubstitution> matches = new ArrayList<RenamingSubstitution>();
		for (Goal g : this.goalsMatching(state.getHeadGoal())) {
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
		numUsesGoalsMatching++;
		// figure out what index to use
		if (goal.getArity()==1 && goal.getArg(0).isConstant()) {
			numUsesIndexFA1++;
			return Dictionary.safeGet(this.indexFA1, 
					new FunctorArityArgKey(goal.getFunctor(), goal.getArity(), goal.getArg(0)),
					Collections.<Goal> emptyList());
		}
		if (useTernaryIndex && goal.getArity()>2) {
			if (goal.getArg(0).isConstant() && goal.getArg(1).isConstant()) {
				numUsesIndexFA1A2++;
				return Dictionary.safeGet(this.indexFA1A2, 
						new FunctorArityArg1Arg2Key(goal.getFunctor(), goal.getArity(), goal.getArg(0), goal.getArg(1)),
						Collections.<Goal> emptyList());
			} else if (goal.getArg(0).isConstant()) {
				numUsesIndexFA1++;
				return Dictionary.safeGet(this.indexFA1, 
						new FunctorArityArgKey(goal.getFunctor(), goal.getArity(), goal.getArg(0)),
						Collections.<Goal> emptyList());
			} else if (goal.getArg(1).isConstant()) {
				numUsesIndexFA2++;
				return Dictionary.safeGet(this.indexFA2, 
						new FunctorArityArgKey(goal.getFunctor(), goal.getArity(), goal.getArg(1)),
						Collections.<Goal> emptyList());
			}
		}
		// no argument-specific indices available so use the basic one
		numUsesIndexF++;
		return Dictionary.safeGet(this.indexF,
				new FunctorArityKey(goal.getFunctor(), goal.getArity()),
				Collections.<Goal> emptyList());
	}
	
	/** Utility method for complex features
	 * 
	 * @param functor
	 * @param arity
	 * @param arg1
	 * @return
	 */
    public Iterable<Goal> goalsMatching(String functor, int arity, Argument arg1) {
    	numUsesGoalsMatching++;
		// figure out what index to use
		if (arity==1 && arg1.isConstant()) {
			numUsesIndexFA1++;
			return Dictionary.safeGet(this.indexFA1, 
					new FunctorArityArgKey(functor, arity, arg1),
					Collections.<Goal> emptyList());
		}
		if (useTernaryIndex && arity>2) {
			if (arg1.isConstant()) {
				numUsesIndexFA1++;
				return Dictionary.safeGet(this.indexFA1, 
						new FunctorArityArgKey(functor, arity, arg1),
						Collections.<Goal> emptyList());
			}
		}
		// no argument-specific indices available so use the basic one
		numUsesIndexF++;
		return Dictionary.safeGet(this.indexF,
				new FunctorArityKey(functor, arity),
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

	public static GoalComponent loadCompiled(List<String> files) {
		return loadCompiled(files, DEFAULT_INDICES);
	}
	public static GoalComponent loadCompiled(List<String> files, boolean useTernaryIndex) {
		GoalComponent result = new GoalComponent(files.get(0)+ (files.size() > 1 ? "+"+(files.size()-1)+"others" : ""), useTernaryIndex);
		for (String filename : files) loadInto(result,filename);
		return result;
	}
	private static void loadInto(GoalComponent result, String filename) {
		ParsedFile file = new ParsedFile(filename);
		for (String line : file) {
			result.addFact(Goal.parseGoal(line,"\t"));//new Goal(functor_args[0],functor_args[1].split("\t")));
		}
	}
	/**
	 * Returns a goal component loaded from a file, where each line contains
        a single ground goal, stored as functor <TAB> arg1 <TAB> .....
	 * @param filename
	 */
	public static GoalComponent loadCompiled(String filename) {
		return loadCompiled(filename,DEFAULT_INDICES);
	}
	/**
	 * Returns a goal component loaded from a file, where each line contains
        a single ground goal, stored as functor <TAB> arg1 <TAB> .....
	 * @param filename
	 * @param useTernaryIndex
	 */
	public static GoalComponent loadCompiled(String filename, boolean useTernaryIndex) {
		return loadCompiled(Collections.singletonList(filename), useTernaryIndex);
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
