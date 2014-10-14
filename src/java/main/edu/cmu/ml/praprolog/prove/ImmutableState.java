package edu.cmu.ml.praprolog.prove;

public class ImmutableState extends State {
	public ImmutableState(MutableState state) {}

	@Override
	public ImmutableState immutableVersion() {
		return this;
	}

	@Override
	public MutableState mutableVersion() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void collapsePointers(int i, int last) {
		// TODO Auto-generated method stub

	}

}
