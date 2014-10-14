package edu.cmu.ml.praprolog.prove.wam;

import edu.cmu.ml.praprolog.prove.MutableState;
import edu.cmu.ml.praprolog.prove.WamInterpreter;
/**
 * Push the value of the a-th local variable onto the heap.
 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
 *
 */
public class PushBoundVar extends Instruction {
	private int a;
	public PushBoundVar(String[] args) {
		super(args);
		a = Integer.parseInt(args[0]);
		if (a>=0) throw new IllegalArgumentException("a must be <0");
	}
	@Override
	public void execute(WamInterpreter interp) {
		MutableState state = interp.getState();
		int valueOfA = state.dereference(state.getRegister(a));
		state.appendHeap(state.createVariableCell(valueOfA));
		state.incrementProgramCounter();
	}

}
