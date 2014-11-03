package edu.cmu.ml.praprolog.graph;

import edu.cmu.ml.praprolog.prove.wam.State;

public class IdentityIndex extends Index<State,State> {

	@Override
	public State asKey(State id) {
		return id;
	}

	@Override
	public State asId(State key) {
		return key;
	}

}
