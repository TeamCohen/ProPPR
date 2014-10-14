package edu.cmu.ml.praprolog.prove.wam;

import edu.cmu.ml.praprolog.prove.MutableState;
import edu.cmu.ml.praprolog.prove.WamInterpreter;
import edu.cmu.ml.praprolog.prove.v1.LogicProgramException;
/**
 * Unify a variable to a heap position.
 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
 *
 */
public class UnifyBoundVar extends Instruction {
	private int a;
	private int relativeHeapIndex;
	public UnifyBoundVar(String[] args) {
		super(args);
		a = Integer.parseInt(args[0]);
		if (a>=0) throw new IllegalArgumentException("a must be <0");
		relativeHeapIndex = Integer.parseInt(args[1]);
		if (relativeHeapIndex >= 0) throw new IllegalArgumentException("relative heap index must be <0");
	}

	@Override
	public void execute(WamInterpreter interp) throws LogicProgramException {
		MutableState state = interp.getState();
		// convert to absolute heap indices
		int i = state.getHeapSize() + relativeHeapIndex;
		int j = state.getRegister(a);
		// follow pointer chains
		int ri = state.dereference(i);
		int rj = state.dereference(j);
		// cases for unification
		if (ri == rj) {
			//fine
		} else if (state.hasConstantAt(ri) && state.hasConstantAt(rj)) {
			state.setFailed(state.getIdOfConstantAt(ri) != state.getIdOfConstantAt(rj));
		} else if (state.hasConstantAt(ri)) {
			if (!state.hasFreeAt(rj)) throw new LogicProgramException("ri constant; rj not free");
			rj = ri;
		} else if (state.hasConstantAt(rj)) {
			if (!state.hasFreeAt(ri)) throw new LogicProgramException("rj constant; ri not free");
			state.setHeap(ri, state.copyConstantCell(rj));
		} else if (rj > ri) {
			if (!state.hasFreeAt(ri) || !state.hasFreeAt(ri)) throw new LogicProgramException("ri,rj not both free");
			state.setHeap(rj, state.createVariableCell(ri)); // bind larger to smaller
			rj = ri;
		} else {
			if ( !(ri > rj) || !state.hasFreeAt(ri) || !state.hasFreeAt(rj)) throw new LogicProgramException("ri,rj not ordered and free");
			state.setHeap(ri, state.createVariableCell(rj)); // bind larger to smaller
			ri = rj;
		}
		state.collapsePointers(i,ri);
		state.collapsePointers(j, rj);
		state.incrementProgramCounter();
	}

}
