package edu.cmu.ml.proppr.prove;


import java.util.Map;

import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.prove.wam.CachingIdProofGraph;
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
	public String toString() { 
		return String.format("p_idpr:%.6g:%g", apr.epsilon, apr.alpha);
	}

	public PruningIdDprProver() { super(false); }

	public PruningIdDprProver(boolean lazyWalk) {
		super(lazyWalk,new APROptions());
	}
	public PruningIdDprProver(APROptions apr) {
		super(false, apr);
	}
	public PruningIdDprProver(boolean lazyWalk, APROptions apr) {
		super( (lazyWalk?STAYPROB_LAZY:STAYPROB_DEFAULT),apr);
	}
	protected PruningIdDprProver(double stayP, APROptions apr) {
		super(stayP,apr);
	}

	public Map<State, Double> prove(CachingIdProofGraph pg) {
		LongDense.FloatVector p = new LongDense.FloatVector();
		prove(pg,p);
		return pg.asMap(p);
	}
}
