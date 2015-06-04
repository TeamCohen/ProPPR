package edu.cmu.ml.proppr.prove.wam;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.examples.GroundedExample;
import edu.cmu.ml.proppr.examples.InferenceExample;
import edu.cmu.ml.proppr.graph.InferenceGraph;
import edu.cmu.ml.proppr.graph.LightweightStateGraph;
import edu.cmu.ml.proppr.prove.wam.plugins.WamPlugin;
import edu.cmu.ml.proppr.util.APROptions;
import gnu.trove.strategy.HashingStrategy;

public class StateProofGraph extends ProofGraph {
	private static final Logger log = Logger.getLogger(ProofGraph.class);

	private LightweightStateGraph graph;
	public StateProofGraph(Query query, APROptions apr, WamProgram program, WamPlugin ... plugins) throws LogicProgramException { 
		this(new InferenceExample(query,null,null), apr, program, plugins);
	}
	public StateProofGraph(InferenceExample ex, APROptions apr, WamProgram program, WamPlugin[] plugins) throws LogicProgramException {
		super(ex, apr, program, plugins);
		this.graph = new LightweightStateGraph(new HashingStrategy<State>() {
			@Override
			public int computeHashCode(State s) {
				return s.canonicalHash();
			}

			@Override
			public boolean equals(State s1, State s2) {
				return s1.canonicalHash() == s2.canonicalHash();
			}});
	}
	@Override
	public int getId(State s) {
		return this.graph.getId(s);
	}
	
	public LightweightStateGraph getGraph() {
		return this.graph;
	}
	/**
	 * Return the list of outlinks from the provided state, including a reset outlink back to the query.
	 * @param state
	 * @param trueLoop
	 * @return
	 * @throws LogicProgramException
	 */
	public List<Outlink> pgOutlinks(State state, boolean trueLoop) throws LogicProgramException {
		// wwc: why aren't trueloop, restart objects precomputed and shared?
		if (!this.graph.outlinksDefined(state)) {
			List<Outlink> outlinks = this.computeOutlinks(state,trueLoop);
			if (log.isDebugEnabled()) {
				// check for duplicate hashes
				Set<Integer> canons = new TreeSet<Integer>();
				for (Outlink o : outlinks) {
					if (canons.contains(o.child.canon)) log.warn("Duplicate canonical hash found in outlinks of state "+state);
					canons.add(o.child.canon);
				}
			}
			this.graph.setOutlinks(state,outlinks);
			return outlinks;
		}
		return this.graph.getOutlinks(state);
	}
	/** The number of outlinks for a state, including the reset outlink back to the query. 
	 * @throws LogicProgramException */
	public int pgDegree(State state) throws LogicProgramException {
		return this.pgDegree(state, true);
	}
	
	public int pgDegree(State state, boolean trueLoop) throws LogicProgramException {
		return this.pgOutlinks(state, trueLoop).size();
	}
	@Override
	protected InferenceGraph _getGraph() { return this.graph; }
}
