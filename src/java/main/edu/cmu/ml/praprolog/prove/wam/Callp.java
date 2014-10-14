package edu.cmu.ml.praprolog.prove.wam;

import edu.cmu.ml.praprolog.prove.CallStackFrame;
import edu.cmu.ml.praprolog.prove.MutableState;
import edu.cmu.ml.praprolog.prove.WamInterpreter;

/**
 * Insert an appropriate CallStackFrame on the call stack for a later
        'returnp' call, and then mark the interpreter's as ready to
        jump to that predicate's definition.
 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
 *
 */
public class Callp extends Instruction {
	private String pred;
	public Callp(String[] args) {
		super(args);
		pred = args[0];
	}

	@Override
	public void execute(WamInterpreter interp) {
		MutableState state = interp.getState();
		state.incrementProgramCounter();
		state.getCalls().add(new CallStackFrame(state));
		state.setJumpTo(pred);
		state.decrementProgramCounter();
	}

}
