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
public class PruningIdPprProver extends IdPprProver {
	private CachingIdProofGraph.VisibilityFilter test;
	private FixedWeightRules prunedPredicateRules;

	public String toString() { 
		return String.format("p_ippr:%.6g:%g", apr.epsilon, apr.alpha);
	}

	public PruningIdPprProver(APROptions apr,FixedWeightRules prunedPredicateRules) {
		super(apr);
		this.test = new CachingIdProofGraph.PredicatePruner(prunedPredicateRules);
		this.prunedPredicateRules = prunedPredicateRules;
	}

	@Override
	public Prover<CachingIdProofGraph> copy() {
		PruningIdPprProver copy = new PruningIdPprProver(this.apr,this.prunedPredicateRules);
		copy.params = this.params;
		return copy;
	}

	@Override
	public Map<State, Double> prove(CachingIdProofGraph pg) {
		//System.out.println("calling Prunedpredicaterules.prove");
		LongDense.FloatVector p = proveVec(pg);
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

}
