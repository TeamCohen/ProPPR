package edu.cmu.ml.praprolog.prove;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.log4j.Logger;

import edu.cmu.ml.praprolog.prove.Component.Outlink;
import edu.cmu.ml.praprolog.util.Dictionary;
import edu.cmu.ml.praprolog.util.SymbolTable;
import edu.cmu.ml.praprolog.util.tuprolog.TuprologAdapter;

public class ProPPRLogicProgramState extends LogicProgramState {
	private static final Logger log = Logger.getLogger(ProPPRLogicProgramState.class);
	protected Goal[] goals,queryGoals,originalQueryGoals;
	protected VarSketch varSketch;
	protected RenamingSubstitution theta;
	protected int depth;
	protected int hash=0;

	/** primary constructor for programmer use */
	public ProPPRLogicProgramState(Goal ... goals) {
		this.init(
				Arrays.copyOf(goals, goals.length),
				Arrays.copyOf(goals, goals.length),
				Arrays.copyOf(goals, goals.length),
				new RenamingSubstitution(0),
				0); // FIXME
	}
	/** special constructor for internal use only */
	public ProPPRLogicProgramState(Goal[] originalQueryGoals, Goal[] queryGoals, Goal[] goals, RenamingSubstitution theta, int depth) {
		this.init(originalQueryGoals,queryGoals,goals,theta,depth);
	}
	private void init(Goal[] originalQueryGoals, Goal[] queryGoals, Goal[] goals, RenamingSubstitution theta, int depth) {
		this.queryGoals = queryGoals;
		this.goals = goals;
		this.originalQueryGoals = originalQueryGoals;
		this.theta = theta;
		this.depth = depth;
		if (goals.length > 0) this.freeze();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ProPPRLogicProgramState)) return false;
		ProPPRLogicProgramState s = (ProPPRLogicProgramState) o;
		if (this.queryGoals.length != s.queryGoals.length) return false;
		if (this.goals.length != s.goals.length) return false;
		for (int i=0; i<this.queryGoals.length; i++) {
			if ( ! this.queryGoals[i].equals(s.queryGoals[i])) return false;
		}
		for (int i=0; i<this.goals.length; i++) {
			if ( ! this.goals[i].equals(s.goals[i])) return false;
		}
		return true;
	}
	@Override
	public int hashCode() {
		return this.hash;
	}

	protected void freeze() {
		for (Goal g : this.queryGoals) hash = hash ^ g.hashCode();
		for (Goal g : this.goals) hash = hash ^ g.hashCode();
		//count the number of variables appearing in this state
		this.varSketch = new VarSketch();
		this.varSketch.includeAll(this.queryGoals);
		this.varSketch.includeAll(this.goals);
		this.varSketch.include(this.theta);
	}
	public Goal getGoal(int i) {
		if (i<goals.length)
			return goals[i];
		else throw new IllegalArgumentException("Can't get goal "+i+"; only has "+goals.length+" goals");
	}
	@Override
	public String getHeadFunctor() {
		if (this.goals.length==0) return null;
		return this.getGoal(0).getFunctor();
	}
	@Override
	public Argument getHeadArg1() {
		if (this.goals.length==0) return null;
		return this.getGoal(0).getArg(0);
	}
	/**
	 * Return true iff this state is a solution state - ie, a complete refutation.
	 * @return
	 */
	public boolean isSolution() {
		return goals.length == 0;
	}
	/**
	 * Construct a state that restarts the original query.
	 * @return
	 */
	public ProPPRLogicProgramState restart() {
		return new ProPPRLogicProgramState(this.originalQueryGoals);
	}

	private Goal normalizeVariablesInGoal(Goal g, SymbolTable variableSymTab) {
		Argument[] args = g.getArgs();
		Argument[] newArgs = new Argument[args.length];
		for (int a=0; a<args.length; a++) {
			if (args[a].isConstant()) newArgs[a] = args[a];
			else newArgs[a] = variableSymTab.getId(args[a]);
		}
		return new Goal(g.getFunctor(),newArgs);
	}
	/**
	 * Construct a child of this state.  The first goal is
        removed, the additionalGoals are added, and theta is applied
        to the queryGoals and the new goal list.
	 * @param emptyList
	 * @param additionalTheta
	 * @return
	 */
	public LogicProgramState child(Goal[] additionalGoals,
			RenamingSubstitution additionalTheta) {

		if (log.isDebugEnabled()) {
			log.debug("child of "+this);
			log.debug("under "+this.theta);
			log.debug("plus "+Dictionary.buildString(additionalGoals, new StringBuilder(), " "));
			log.debug("under "+additionalTheta);
		}
		// int newDepths = this.depth + 1;


		Goal[] tmpGoals = new Goal[additionalGoals.length + this.goals.length-1];
		Goal[] tmpGoals1 = additionalTheta.applyToGoalList(additionalGoals, RenamingSubstitution.RENAMED),
				tmpGoals2;
		if (this.goals.length>1) tmpGoals2 = additionalTheta.applyToGoalList(
				Arrays.copyOfRange(this.goals, 1, this.goals.length), // drop goal 0
				RenamingSubstitution.NOT_RENAMED);
		else tmpGoals2 = new Goal[0];
		{ int i=0; for(;i<tmpGoals1.length;i++) tmpGoals[i] = tmpGoals1[i];
		for (int j=0;j<tmpGoals2.length;j++) { tmpGoals[i] = tmpGoals2[j]; i++; }}
		//		if (log.isDebugEnabled()) log.debug("tmpGoals:"+Dictionary.buildString(tmpGoals,new StringBuilder()," "));
		Goal[] tmpQueryGoals = additionalTheta.applyToGoalList(this.queryGoals,RenamingSubstitution.NOT_RENAMED);

		Goal[][] allGoals = {tmpQueryGoals, tmpGoals1, tmpGoals2};

		SymbolTable variableSymTab = new SymbolTable();
		for (Goal[] goalList : allGoals) {
			for (Goal g : goalList) {
				for (Argument a : g.getArgs()) {
					if (a.isVariable()) variableSymTab.insert(a); 
				}
			}
		}
		for (int i=0; i<tmpGoals.length; i++) {
			tmpGoals[i] = normalizeVariablesInGoal(tmpGoals[i], variableSymTab);
		}
		for (int i=0; i<tmpQueryGoals.length; i++) {
			tmpQueryGoals[i] = normalizeVariablesInGoal(tmpQueryGoals[i], variableSymTab);
		}

		ProPPRLogicProgramState result = new ProPPRLogicProgramState(
				this.originalQueryGoals, // FIXME - defensive copy?
				tmpQueryGoals, 
				tmpGoals, 
				this.theta.copy(additionalTheta),
				this.depth+1);
		if (log.isDebugEnabled()) {
			log.debug("is "+(result.isSolution() ? "SOLUTION " : "") + result);
			log.debug("with "+result.theta);
		}
		return result;
	}
	public int getVarSketchSize() {
		return this.varSketch.size();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder("lpState: ");
		Dictionary.buildString(this.queryGoals,sb," ");
		sb.append(" ... ");
		Dictionary.buildString(this.goals,sb," ");
		return sb.toString();
	}
	/**
	 * Return a string describing the solution found, relative to the original query.
	 * @return
	 */
	@Override
	public String description() {
		if (!this.isSolution()) return "not a solution";
		if (this.originalQueryGoals.length != this.queryGoals.length) {
			throw new IllegalStateException("Original and grounded goal list must match in length");
		}
		HashMap<Argument,Argument> assignments = new HashMap<Argument,Argument>();
		for (int i=0; i<this.originalQueryGoals.length; i++) {
			if (this.originalQueryGoals[i].getArity() != this.queryGoals[i].getArity()) {
				throw new IllegalStateException("Original and grounded goals must match in arity");
			}
			for (int j=0; j<this.originalQueryGoals[i].getArity(); j++) {
				Argument queryArg = this.originalQueryGoals[i].getArg(j);
				if (queryArg.isConstant()) continue;
				Argument groundArg = this.queryGoals[i].getArg(j);
				if (!assignments.containsKey(queryArg)) {
					assignments.put(queryArg, this.queryGoals[i].getArg(j));
				} else { // then the assignment should be the same
					if (!this.queryGoals[i].getArg(j).equals(assignments.get(queryArg))) {
						throw new IllegalStateException("One variable two assignments :(");
					}
				}
			}
		}
		ArrayList<Argument> sorted = new ArrayList<Argument>();
		sorted.addAll(assignments.keySet());
		Collections.sort(sorted);

		StringBuilder sb = new StringBuilder();
		boolean first=true;
		for (Argument a : sorted) {
			if(first)first=false;
			else sb.append(", ");
			sb.append(a.getName()).append("=").append(assignments.get(a));
		}
		return sb.toString();
	}
	public String oldDescription() {
		HashSet<Argument> queryVars = new HashSet<Argument>();
		for (Goal g : this.originalQueryGoals) {
			for (Argument a : g.getArgs()) {
				if (a.isVariable()) queryVars.add(a);
			}
		}
		StringBuilder sb = new StringBuilder();
		ArrayList<Argument> sorted = new ArrayList<Argument>();
		sorted.addAll(queryVars);
		Collections.sort(sorted);
		boolean first=true;
		for (Argument a : sorted) {
			if(first)first=false;
			else sb.append(", ");
			sb.append(a.getName()).append("=").append(this.theta.valueOf(a));
		}
		return sb.toString();
	}
	public int getDepth() {
		return this.depth;
	}
	public RenamingSubstitution getTheta() {
		return this.theta;
	}
	public int getOffset() {
		return this.theta.offset;
	}
	public Goal[] getQueryGoals() {
		return this.queryGoals;
	}
	public Goal[] getGoals() {
		return this.goals;
	}
	public Goal[] getOriginalGoals() {
		return this.originalQueryGoals;
	}
	@Override
	public Goal getGroundGoal() {
		if (queryGoals.length != 1) throw new IllegalStateException("1 ground goal expected; found "+queryGoals.length);
		return queryGoals[0];
	}

	protected LogicProgramState tuprolog;
	@Override
	public LogicProgramState asTuprolog() {
		if (this.tuprolog == null) this.tuprolog = TuprologAdapter.propprToTuprolog(this);
		return this.tuprolog;
	}
	@Override
	public boolean isHeadEdge() {
		if (this.goals.length==0) return false;
		return this.goals[0].getArity()==2 
				&& this.goals[0].getArg(0).isConstant();
	}
	@Override
	public int getHeadArity() {
		if (this.goals.length == 0) return -1;
		return this.goals[0].getArity();
	}
	@Override
	public LogicProgramState child(RenamingSubstitution bindings) {
		// TODO Auto-generated method stub
		return this.child(new Goal[0], bindings);
	}
	@Override
	public Goal getHeadGoal() {
		if (this.goals.length == 0) return null;
		return this.goals[0];
	}
}
