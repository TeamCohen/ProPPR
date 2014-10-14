package edu.cmu.ml.praprolog.prove.wam;

import edu.cmu.ml.praprolog.prove.MutableState;
import edu.cmu.ml.praprolog.prove.WamInterpreter;
import edu.cmu.ml.praprolog.prove.v1.LogicProgramException;
/**
 * Bind a free variable to this heap position.
 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
 *
 */
public class InitFreeVar extends Instruction {
	private int a;
	private int relativeHeapIndex;
	public InitFreeVar(String[] args) {
		super(args);
		a = Integer.parseInt(args[0]);
		if (a>=0) throw new IllegalArgumentException("a must be <0");
		relativeHeapIndex = Integer.parseInt(args[1]);
		if (relativeHeapIndex >= 0) throw new IllegalArgumentException("relative heap index must be <0");
	}

	@Override
	public void execute(WamInterpreter interp) throws LogicProgramException {
		MutableState state = interp.getState();
		state.setRegister(a, state.getHeapSize() + relativeHeapIndex);
		state.incrementProgramCounter();
	}

}
