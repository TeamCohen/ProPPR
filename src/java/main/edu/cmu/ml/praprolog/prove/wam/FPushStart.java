package edu.cmu.ml.praprolog.prove.wam;

import edu.cmu.ml.praprolog.prove.Feature;
import edu.cmu.ml.praprolog.prove.WamInterpreter;
import edu.cmu.ml.praprolog.prove.v1.LogicProgramException;

public class FPushStart extends Instruction {
	private String functor;
	private int arity;
	public FPushStart(String[] args) {
		super(args);
		functor = args[0];
		arity = Integer.parseInt(args[1]);
	}

	@Override
	public void execute(WamInterpreter interp) throws LogicProgramException {
		interp.getFeatureStack().add(new Feature(functor,arity));
		interp.getState().incrementProgramCounter();
	}

}
