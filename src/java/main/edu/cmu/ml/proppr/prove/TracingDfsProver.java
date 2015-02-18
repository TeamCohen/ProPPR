package edu.cmu.ml.proppr.prove;

import edu.cmu.ml.proppr.prove.wam.LogicProgramException;
import edu.cmu.ml.proppr.prove.wam.ProofGraph;
import edu.cmu.ml.proppr.prove.wam.State;
import edu.cmu.ml.proppr.util.APROptions;
import edu.cmu.ml.proppr.util.Dictionary;

/**
 * A prover with scores based on simple depth-first-search, which
    additional prints out a detailed trace.
 * @author "William Cohen <wcohen@cs.cmu.edu>"
 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
 *
 */
public class TracingDfsProver extends DfsProver {
	public TracingDfsProver(APROptions apr) {
		super(apr);
	}
	public TracingDfsProver(FeatureDictWeighter w, APROptions apr, boolean trueLoop) {
		super(w,apr,trueLoop);
	}
	@Override
	protected void beforeDfs(State state, ProofGraph pg, int depth) throws LogicProgramException {
		StringBuilder sb = new StringBuilder();
		for (int i=0;i<depth;i++) sb.append("| ");
		if (!state.isCompleted()) {
			Dictionary.buildString(pg.getInterpreter().pendingGoals(state), sb, ", ");
			sb.append(" => ");
		}
		sb.append(pg.fill(state));
		System.out.println(sb.toString());
	}
	@Override
	public Prover copy() {
		return new TracingDfsProver(this.weighter, this.apr, this.trueLoop);
	}
}
