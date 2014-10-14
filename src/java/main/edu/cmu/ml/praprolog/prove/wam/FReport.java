package edu.cmu.ml.praprolog.prove.wam;

import edu.cmu.ml.praprolog.prove.Feature;
import edu.cmu.ml.praprolog.prove.Goal;
import edu.cmu.ml.praprolog.prove.WamInterpreter;
import edu.cmu.ml.praprolog.prove.v1.LogicProgramException;

public class FReport extends Instruction {

	public FReport(String[] args) {
		super(args);
	}

	@Override
	public void execute(WamInterpreter interp) throws LogicProgramException {
		for (Feature f : interp.getFeatureStack()) {
			Goal g = new Goal(f.functor,f.args);
			interp.reportFeature(g);
		}
		if (interp.getFeatureStack().isEmpty()) interp.reportFeature(new Goal("_no_features_"));
		interp.getState().incrementProgramCounter();
	}

}
