package edu.cmu.ml.proppr.prove;


import java.util.Map;

import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.learn.tools.FixedWeightRules;
import edu.cmu.ml.proppr.prove.wam.CachingIdProofGraph;
import edu.cmu.ml.proppr.prove.wam.CallStackFrame;
import edu.cmu.ml.proppr.prove.wam.Goal;
import edu.cmu.ml.proppr.prove.wam.LogicProgramException;
import edu.cmu.ml.proppr.prove.wam.ProofGraph;
import edu.cmu.ml.proppr.prove.wam.State;
import edu.cmu.ml.proppr.prove.wam.StateProofGraph;
import edu.cmu.ml.proppr.util.APROptions;
import edu.cmu.ml.proppr.util.math.LongDense;
import edu.cmu.ml.proppr.util.math.SmoothFunction;

/**
 */
public class PruningIdDprProver extends IdDprProver {
	private CachingIdProofGraph.VisibilityTest test;
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
	public Map<State, Double> prove(CachingIdProofGraph pg) {
		//System.out.println("calling Prunedpredicaterules.prove");
		LongDense.FloatVector p = new LongDense.FloatVector();
		prove(pg,p);
		System.out.println("== before pruning: edges/nodes "+pg.edgeSize()+"/"+pg.nodeSize());
		System.out.println(pg.treeView(weighter));
		pg.prune(params,weighter,test);
		System.out.println("== after pruning:  edges/nodes "+pg.edgeSize()+"/"+pg.nodeSize());
		System.out.println(pg.treeView(weighter));
		return pg.asMap(p);
	}

	public static class PredicatePruner implements CachingIdProofGraph.VisibilityTest {
		private FixedWeightRules rules;
		public PredicatePruner(FixedWeightRules rules) {
			this.rules = rules;
			//System.out.println("pruning rules = "+rules);
		}
		public boolean visible(State state) {
			// test to see if any hidden predicate is on the stack 
			//System.out.println("Testing state "+state);
			String jumpTo = state.getJumpTo();
			if (jumpTo!=null && rules.isFixed(state.getJumpTo())) {
				return false;
			}
			for (CallStackFrame frame: state.getCalls()) {
				jumpTo = frame.getJumpTo();
				if (jumpTo!=null && rules.isFixed(jumpTo)) {
						//System.out.println("== invisible state! from "+jumpTo);
					return false;
				}
			}
			//System.out.println("No rules fired");
			return true;
		}
	}
}
