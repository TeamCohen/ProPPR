package edu.cmu.ml.proppr.prove.v1;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.util.SymbolTable;

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
			return !state.isSolution() && state.isHeadEdge() && this._indexContains(state.getHeadFunctor());
		}
		
		@Override
		public List<Outlink> outlinks(LogicProgramState state) {
			List<Argument> values = this._indexGet(state.getHeadFunctor(), state.getHeadArg1());
			Argument dstVar = state.getHeadGoal().getArg(1);
			List<Outlink> result = new ArrayList<Outlink>();
			if (values != null && values.size() > 0) {
				ProPPRLogicProgramState stateP = (ProPPRLogicProgramState) state.asProPPR();
				double w = 1.0/values.size();
				for (Argument v : values) {
					RenamingSubstitution thnew = new RenamingSubstitution(stateP.getOffset());
					thnew.put(dstVar,v); 
					result.add(new Outlink(this.getFeatureDict(), stateP.child(thnew)));
				}
			}
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
			return this._indexGetDegree(state.getHeadFunctor(), state.getHeadArg1());
		}
		
}
