package edu.cmu.ml.praprolog.prove.wam;

import edu.cmu.ml.praprolog.prove.MutableState;
import edu.cmu.ml.praprolog.prove.WamInterpreter;
/**
 * Add a constant a to the heap.
 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
 *
 */
public class PushConst extends Instruction {
	String a;
	public PushConst(String[] args) {
		super(args);
		a = args[0];
	}

	@Override
	public void execute(WamInterpreter interp) {
		MutableState state = interp.getState();
		int id=interp.getConstantTable().getId(a);
		state.appendHeap(state.createConstantCell(id));
		state.incrementProgramCounter();
	}

}
