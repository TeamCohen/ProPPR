package edu.cmu.ml.proppr.graph;

import static org.junit.Assert.*;

import org.junit.Test;

import edu.cmu.ml.proppr.prove.wam.CachingIdProofGraph;
import edu.cmu.ml.proppr.prove.wam.State;
import edu.cmu.ml.proppr.util.ConcurrentSymbolTable;

public class CachingIdGraphTest extends InferenceGraphTestTemplate {

	@Override
	public InferenceGraph getGraph() {
		return new CachingIdProofGraph(new ConcurrentSymbolTable.HashingStrategy<State>() {
			@Override
			public Object computeKey(State s) {
				return s.canonicalHash();
			}
			@Override
			public boolean equals(State s1, State s2) {
				if (s1.canonicalHash() != s2.canonicalHash()) return false;
				return s1.canonicalForm().equals(s2.canonicalForm());
			}});
	}

}
