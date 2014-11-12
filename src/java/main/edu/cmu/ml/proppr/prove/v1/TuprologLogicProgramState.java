package edu.cmu.ml.proppr.prove.v1;

import java.util.Iterator;

import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.tuprolog.TuprologAdapter;
import edu.cmu.ml.proppr.util.tuprolog.TuprologAdapter.VarData;
import alice.tuprolog.Struct;
import alice.tuprolog.Term;
import alice.tuprolog.Var;

public class TuprologLogicProgramState extends LogicProgramState {
	private static final Logger log = Logger.getLogger(TuprologLogicProgramState.class);
	protected Struct goals, queryGoals;
	protected ProPPRLogicProgramState restartState;
	protected int depth=0;
	
	public TuprologLogicProgramState(Goal... goals) {
		this.queryGoals = TuprologAdapter.goalArrayToTerm(goals);
		this.goals = TuprologAdapter.goalArrayToTerm(goals);
		this.restartState = new ProPPRLogicProgramState(goals);
	}
	public TuprologLogicProgramState(Struct qg, Struct g, ProPPRLogicProgramState restart) {
		this.queryGoals = qg;
		this.goals = g;
		this.restartState = restart;
	}
	public static TuprologLogicProgramState fromState(Term stateterm, TuprologLogicProgramState parent) { //ProPPRLogicProgramState restartState) {
		if (! (stateterm instanceof Struct)) throw new IllegalArgumentException("State term not a Struct: "+stateterm);
		Struct state = (Struct) stateterm;
		TuprologLogicProgramState t = new TuprologLogicProgramState();
		t.queryGoals = (Struct) state.getArg(0).getTerm();
		t.goals = (Struct) state.getArg(1).getTerm();
		t.restartState = parent.restartState;
		t.depth = parent.depth+1;
		return t;
	}

	@Override
	public boolean isSolution() {
		return goals.listSize() == 0;
	}

	@Override
	public ProPPRLogicProgramState restart() {
		return new ProPPRLogicProgramState(this.restartState.originalQueryGoals);
	}

	@Override
	public String description() {
		// TODO Auto-generated method stub
		return null;
	}

	public Struct getQueryGoals() {
		return this.queryGoals;
	}
	public Struct getGoals() {
		return this.goals;
	}
	public Goal[] getOriginalGoals() {
		return this.restartState.originalQueryGoals;
	}
	@Override
	public Goal getGroundGoal() {
		if (queryGoals.listSize() != 1) throw new IllegalStateException("1 ground goal expected; found "+queryGoals.listSize());
		return TuprologAdapter.termToGoal(queryGoals.listHead());
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("lpState: ");
		sb.append(this.queryGoals.toString());//,sb," ");
		sb.append(" ... ");
		sb.append(this.goals.toString());//,sb," ");
		return sb.toString();
	}
	
	protected LogicProgramState proppr;
	protected VarData varData;
	protected VarData getVarData() {
		if (this.varData == null) { this.varData = TuprologAdapter.tuprolog_index(this, new VarData()); }
		return this.varData;
	}
	@Override
	public LogicProgramState asProPPR() {
		if (this.proppr == null) {
			this.proppr = TuprologAdapter.tuprologToProppr(this, this.getVarData());
		}
		return this.proppr;
	}
	public Struct asTerm() {
		return new Struct("state",queryGoals, goals, TuprologAdapter.goalArrayToTerm(this.restartState.originalQueryGoals));
	}
	@Override
	public String getHeadFunctor() {
		if (this.goals.listSize()==0) return null;
		return ((Struct) this.goals.listHead()).getName();
	}
	@Override
	public Argument getHeadArg1() {
		if (this.goals.listSize()==0) return null;
		return TuprologAdapter.termToArg(((Struct) this.goals.listHead()).getArg(0));
	}
	@Override
	public boolean isHeadEdge() {
		if (this.goals.listSize()==0) return false;
		Struct head = (Struct) this.goals.listHead();
		return head.getArity() == 2
				&& head.getArg(0).getTerm() instanceof Struct
				&& head.getArg(1).getTerm() instanceof Var;
	}
	@Override
	public int getHeadArity() {
		if (this.goals.listSize()==0) return -1;
		return ((Struct) this.goals.listHead()).getArity();
	}
	@Override
	public LogicProgramState child(RenamingSubstitution bindings) {
//		log.debug("child of "+this.toString()+" with "+bindings);
//		Struct newGoals = this.goals.listTail();
		throw new UnsupportedOperationException("Not yet implemented");
	}
	@Override
	public Goal getHeadGoal() {
		if (this.goals.listSize() == 0) return null;
		return TuprologAdapter.termToGoal(this.goals.listHead(), this.getVarData());
	}
	
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof TuprologLogicProgramState)) return false; // debatable
		TuprologLogicProgramState s = (TuprologLogicProgramState) o;
		if (this.queryGoals.listSize() != s.queryGoals.listSize()) return false;
		if (this.goals.listSize() != s.goals.listSize()) return false;
		for (Iterator<? extends Term> it=this.queryGoals.listIterator(), ito=s.queryGoals.listIterator();
				it.hasNext() && ito.hasNext();) {
			if (!it.next().equals(ito.next())) return false;
		}		
		for (Iterator<? extends Term> it=this.goals.listIterator(), ito=s.goals.listIterator();
				it.hasNext() && ito.hasNext();) {
			if (!it.next().equals(ito.next())) return false;
		}
		return true;
	}
}
