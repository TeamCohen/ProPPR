package edu.cmu.ml.praprolog.prove.wam;

import edu.cmu.ml.praprolog.prove.ConstantArgument;
import edu.cmu.ml.praprolog.prove.WamInterpreter;
import edu.cmu.ml.praprolog.prove.v1.LogicProgramException;

public class FPushConst extends Instruction {

	String a;
	public FPushConst(String[] args) {
		super(args);
		a = args[0];
	}

	@Override
	public void execute(WamInterpreter interp) throws LogicProgramException {
		interp.getFeaturePeek().append(new ConstantArgument(a));
		interp.getState().incrementProgramCounter();
	}

}
