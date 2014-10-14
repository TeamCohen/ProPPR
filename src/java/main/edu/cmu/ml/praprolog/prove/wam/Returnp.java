package edu.cmu.ml.praprolog.prove.wam;

import edu.cmu.ml.praprolog.prove.CallStackFrame;
import edu.cmu.ml.praprolog.prove.MutableState;
import edu.cmu.ml.praprolog.prove.WamInterpreter;
/**
 * Mark as completed if we're at the top level, and otherwise, pop a
        CallStackFrame and return to that state.
 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
 *
 */
public class Returnp extends Instruction {

	public Returnp(String[] args) {
		super(args);
	}

	@Override
	public void execute(WamInterpreter interp) {
		MutableState state = interp.getState();
		if (state.getCalls().isEmpty()) { 
			state.setCompleted(true);
			return;
		}
		CallStackFrame frame = state.getCalls().remove(state.getCalls().size()-1);
		//TODO: debugmode
		state.truncateHeap(frame.getHeapPointer());
		state.truncateRegisters(frame.getRegisterPointer());
		state.truncateVarNameList(frame.getRegisterPointer());
		state.setProgramCounter(frame.getProgramCounter());
		state.setJumpTo(frame.getJumpTo());
	}

}
