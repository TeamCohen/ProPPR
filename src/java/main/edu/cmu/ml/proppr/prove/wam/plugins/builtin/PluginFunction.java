package edu.cmu.ml.proppr.prove.wam.plugins.builtin;

import edu.cmu.ml.proppr.prove.wam.LogicProgramException;
import edu.cmu.ml.proppr.prove.wam.WamInterpreter;

public interface PluginFunction {
	public boolean run(WamInterpreter wamInterp) throws LogicProgramException;
}