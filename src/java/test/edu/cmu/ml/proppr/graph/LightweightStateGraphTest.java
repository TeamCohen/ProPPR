package edu.cmu.ml.proppr.graph;

import static org.junit.Assert.*;

import org.junit.Test;

import edu.cmu.ml.proppr.prove.wam.State;
import gnu.trove.strategy.HashingStrategy;

/**
 * NB this uses a different hash than StateProofGraph does :(
 * @author krivard
 *
 */
public class LightweightStateGraphTest extends InferenceGraphTestTemplate {

	@Override
	public InferenceGraph getGraph() {
		return new LightweightStateGraph(new HashingStrategy<State>() {
			@Override
			public int computeHashCode(State s) {
				return s.canonicalHash();
			}

			@Override
			public boolean equals(State s1, State s2) {
				if (s1.canonicalHash() != s2.canonicalHash()) return false;
				return s1.canonicalForm().equals(s2.canonicalForm());
			}});
	}

}
