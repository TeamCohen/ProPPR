package edu.cmu.ml.praprolog.prove.wam;

import edu.cmu.ml.praprolog.prove.WamInterpreter;
import edu.cmu.ml.praprolog.prove.v1.LogicProgramException;

public class FClear extends Instruction {

	public FClear(String[] args) {
		super(args);
	}

	@Override
	public void execute(WamInterpreter interp) throws LogicProgramException {
		interp.getFeatureStack().clear();
		interp.getState().incrementProgramCounter();
	}

}
