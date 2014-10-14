package edu.cmu.ml.praprolog.prove.wam;

import edu.cmu.ml.praprolog.prove.MutableState;
import edu.cmu.ml.praprolog.prove.WamInterpreter;
/**
 * dd an unbound variable to the heap, and have the a-th
        local variable point to it.
 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
 *
 */
public class PushFreeVar extends Instruction {
	private int a;
	public PushFreeVar(String[] args) {
		super(args);
		a = Integer.parseInt(args[0]);
		if (a>=0) throw new IllegalArgumentException("a must be <0");
	}

	@Override
	public void execute(WamInterpreter interp) {
		MutableState state = interp.getState();
		int i=state.getHeapSize();
		state.appendHeap(state.createVariableCell(i));
		state.setRegister(a,i);
		state.incrementProgramCounter();
	}

}
