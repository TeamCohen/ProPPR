package edu.cmu.ml.praprolog.prove;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.apache.log4j.Logger;

import edu.cmu.ml.praprolog.prove.VarSketch;
import edu.cmu.ml.praprolog.util.Dictionary;
import edu.cmu.ml.praprolog.util.SymbolTable;

/**
 * Intermediate state of a proof - a conjunction of goals, a
    substitution, and a depth, which is used standardizing apart any
    new rules.  Also contains the original goals and substitution from
    the query, which are used for restarts.
 * @author krivard
 *
 */
public abstract class LogicProgramState extends Component {
	private static final Logger log = Logger.getLogger(LogicProgramState.class);
	
	public LogicProgramState asProPPR() {
		return this;
	}
	public LogicProgramState asTuprolog() {
		return this;
	}

	/**
	 * Return true iff this state is a solution state - ie, a complete refutation.
	 * @return
	 */
	public abstract boolean isSolution();
	/**
	 * Construct a state that restarts the original query.
	 * @return
	 */
	public abstract ProPPRLogicProgramState restart();
	/**
	 * Return a string describing the solution found, relative to the original query.
	 * @return
	 */
	public abstract String description();
	
	public abstract String getHeadFunctor();
	public abstract int getHeadArity();
	public abstract Argument getHeadArg1();
	public abstract boolean isHeadEdge();
	public abstract Goal getGroundGoal();
	public abstract LogicProgramState child(RenamingSubstitution bindings);

	@Override
	public List<Outlink> outlinks(LogicProgramState state) {
		throw new IllegalStateException("Should never be calling outlinks() on a LogicProgramState (even though we are a component)");
	}
	@Override
	public boolean claim(LogicProgramState state) {
		throw new IllegalStateException("Should never be calling claim() on a LogicProgramState (even though we are a component)");
	}

	@Override
	public void compile() {
		// does nothing
	}
	@Override
	public void compile(SymbolTable variableSymTab) {
		// does  nothing		
	}
}
