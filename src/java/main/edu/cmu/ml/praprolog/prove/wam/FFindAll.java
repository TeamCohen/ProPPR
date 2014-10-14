package edu.cmu.ml.praprolog.prove.wam;

import edu.cmu.ml.praprolog.prove.ImmutableState;
import edu.cmu.ml.praprolog.prove.State;
import edu.cmu.ml.praprolog.prove.WamInterpreter;
import edu.cmu.ml.praprolog.prove.v1.LogicProgramException;

public class FFindAll extends Instruction {
	int addr;
	public FFindAll(String[] args) {
		super(args);
		addr = Integer.parseInt(args[0]);
	}

	@Override
	public void execute(WamInterpreter interp) throws LogicProgramException {
		// backup the state
		ImmutableState savedState = interp.saveState();
		// clear the call stack and branch to addr
		interp.getState().getCalls().clear();
		interp.executeWithoutBranching(addr);
		// do DFS to find all features
		interp.doFeatureFindallDFS(interp.saveState(),0);
		interp.restoreState(savedState);
		interp.getState().incrementProgramCounter();
	}

}
