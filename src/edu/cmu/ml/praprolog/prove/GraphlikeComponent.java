package edu.cmu.ml.praprolog.prove;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.cmu.ml.praprolog.util.Dictionary;
import edu.cmu.ml.praprolog.util.SymbolTable;

/**
 * A 'extensional database' - restricted to be a labeled directed
    graph, or equivalently, a set of f(+X,-Y) unit predicates.
 * @author wcohen,krivard
 *
 */
public abstract class GraphlikeComponent extends Component {

		
		// NB could pull these out into an index class, but for initialization purposes easier to put variation at the Component level instead...
		protected abstract void _indexAppend(String functor, Argument src, Argument dst);
		protected abstract boolean _indexContains(String functor);
		protected abstract List<Argument> _indexGet(String functor, Argument srcConst);
		protected abstract int _indexGetDegree(String functor, Argument srcConst);

		protected abstract Map<Goal, Double> getFeatureDict();
		/**
		 * An an arc to a graph-based EDB.
		 * @param functor
		 * @param src
		 * @param dst
		 */
		protected void addEdge(String functor, Argument src, Argument dst) {
			this._indexAppend(functor, src, dst);
		}

		@Override
		public boolean claim(LogicProgramState state) {
			return !state.isSolution() && this.contains(state.getGoal(0));
		}

		protected boolean contains(Goal goal) {
			return goal.getArity()==2 && this._indexContains(goal.getFunctor());
		}
		
		@Override
		public List<Outlink> outlinks(LogicProgramState state) {
			Goal g = state.getGoal(0);
			Argument srcConst = convertConst(0,state);
			Argument dstVar   = convertVar(1,state);
			
			List<Argument> values = this._indexGet(g.getFunctor(), srcConst);
			
			List<Outlink> result = new ArrayList<Outlink>();
			if (values.size() > 0) {
				double w = 1.0/values.size();
				for (Argument v : values) {
					RenamingSubstitution thnew = new RenamingSubstitution(state.getTheta().offset);
					thnew.put(dstVar,v); 
					result.add(new Outlink(this.getFeatureDict(), state.child(new Goal[0],thnew)));
				}
			}
			return result;
		}
		
		protected Argument convertConst(int i, LogicProgramState state) {
			Argument result = state.getGoal(0).getArg(i);
			if (!result.isConstant()) throw new IllegalStateException("Argument "+(i+1)+" of "+state.getGoal(0)+" should be bound in theta; was "+result);
			return result;
		}
		
		protected Argument convertVar(int i, LogicProgramState state) {
			Argument result = state.getGoal(0).getArg(i);
			if (!result.isVariable()) 
				throw new IllegalStateException("Argument "+(i+1)+" of "+state.getGoal(0)+" should be unbound in theta; was "+result);
			return result;
		}

		@Override
		public void compile() { 
			// pass
		}

		@Override
		public void compile(SymbolTable variableSymTab) {
			// pass
		}

		@Override
		public int degree(LogicProgramState state) {
			if (state.isSolution()) return 0;
			Goal g = state.getGoal(0);
			Argument srcConst = convertConst(0,state);
			Argument dstVar   = convertVar(1,state); // unused but necessary to check binding
			return this._indexGetDegree(g.getFunctor(), srcConst);
		}
		
}
