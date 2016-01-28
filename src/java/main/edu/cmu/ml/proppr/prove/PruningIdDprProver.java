package edu.cmu.ml.proppr.prove;


import java.util.Map;

import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.learn.tools.FixedWeightRules;
import edu.cmu.ml.proppr.prove.wam.CachingIdProofGraph;
import edu.cmu.ml.proppr.prove.wam.CachingIdProofGraph.VisibilityFilter;
import edu.cmu.ml.proppr.prove.wam.CallStackFrame;
import edu.cmu.ml.proppr.prove.wam.Goal;
import edu.cmu.ml.proppr.prove.wam.LogicProgramException;
import edu.cmu.ml.proppr.prove.wam.ProofGraph;
import edu.cmu.ml.proppr.prove.wam.State;
import edu.cmu.ml.proppr.prove.wam.StateProofGraph;
import edu.cmu.ml.proppr.util.APROptions;
import edu.cmu.ml.proppr.util.StatusLogger;
import edu.cmu.ml.proppr.util.math.LongDense;
import edu.cmu.ml.proppr.util.math.SmoothFunction;

/**
 */
public class PruningIdDprProver extends IdDprProver {
	private CachingIdProofGraph.VisibilityFilter test;
	private FixedWeightRules prunedPredicateRules;

	public String toString() { 
		return String.format("p_idpr:%.6g:%g", apr.epsilon, apr.alpha);
	}

	public PruningIdDprProver(APROptions apr,FixedWeightRules prunedPredicateRules) {
		super(false, apr);
		this.test = new PredicatePruner(prunedPredicateRules);
		this.prunedPredicateRules = prunedPredicateRules;
	}

	@Override
	public Prover<CachingIdProofGraph> copy() {
		PruningIdDprProver copy = new PruningIdDprProver(this.apr,this.prunedPredicateRules);
		copy.params = this.params;
		if (this.parent != null) copy.parent = this.parent;
		else copy.parent = this;
		return copy;
	}

	@Override
	public Map<State, Double> prove(CachingIdProofGraph pg, StatusLogger status) {
		//System.out.println("calling Prunedpredicaterules.prove");
		LongDense.FloatVector p = new LongDense.FloatVector();
		prove(pg,p,status);
		if (apr.traceDepth!=0) {
			System.out.println("== before pruning:  edges/nodes "+pg.edgeSize()+"/"+pg.nodeSize());
			System.out.println(pg.treeView(apr.traceDepth,apr.traceRoot,weighter,p));
		}
		LongDense.FloatVector prunedP = pg.prune(params,weighter,test,p);
		//System.out.println("== after pruning:  edges/nodes "+pg.edgeSize()+"/"+pg.nodeSize());
		//System.out.println(pg.treeView(weighter,prunedP));
		if (apr.traceDepth!=0) {
			System.out.println("== after pruning:  edges/nodes "+pg.edgeSize()+"/"+pg.nodeSize());
			System.out.println(pg.treeView(apr.traceDepth,apr.traceRoot,weighter,prunedP));
		}
		return pg.asMap(prunedP);
	}

	public static class PredicatePruner implements CachingIdProofGraph.VisibilityFilter {
		private FixedWeightRules rules;
		public PredicatePruner(FixedWeightRules rules) {
			this.rules = rules;
		}
		public boolean visible(State state) {
			if (rules==null) return true;
			// test to see if any 'pruned' predicate is on the stack 
			String jumpTo = state.getJumpTo();
			if (jumpTo!=null && rules.isFixed(state.getJumpTo())) {
				return false;
			}
			for (CallStackFrame frame: state.getCalls()) {
				jumpTo = frame.getJumpTo();
				if (jumpTo!=null && rules.isFixed(jumpTo)) {
					return false;
				}
			}
			return true;
		}
	}
}
