package edu.cmu.ml.praprolog.prove.wam;

import edu.cmu.ml.praprolog.prove.ConstantArgument;
import edu.cmu.ml.praprolog.prove.MutableState;
import edu.cmu.ml.praprolog.prove.WamInterpreter;
import edu.cmu.ml.praprolog.prove.v1.LogicProgramException;

public class FPushBoundVar extends Instruction {
	private int a;
	public FPushBoundVar(String[] args) {
		super(args);
		a = Integer.parseInt(args[0]);
		if (a >= 0) throw new IllegalArgumentException("a must be <0");
	}

	@Override
	public void execute(WamInterpreter interp) throws LogicProgramException {
		MutableState state = interp.getState();
		int ra = state.dereference(state.getRegister(a));
		if (!state.hasConstantAt(ra)) throw new LogicProgramException("variable in feature not bound to a constant");
		int cid = state.getIdOfConstantAt(ra);
		interp.getFeaturePeek().append(new ConstantArgument(interp.getConstantTable().getSymbol(cid)));
		state.incrementProgramCounter();
	}

}
