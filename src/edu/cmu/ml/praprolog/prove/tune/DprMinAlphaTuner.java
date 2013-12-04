package edu.cmu.ml.praprolog.prove.tune;

import edu.cmu.ml.praprolog.prove.Component;
import edu.cmu.ml.praprolog.prove.LogicProgram;
import edu.cmu.ml.praprolog.util.Configuration;

public class DprMinAlphaTuner {

	protected LogicProgram program;

	public DprMinAlphaTuner(String[] programFiles, double alpha) {
		this.program = new LogicProgram(Component.loadComponents(programFiles, alpha));
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Configuration c = new Configuration(args, Configuration.USE_DEFAULTS | Configuration.USE_DATA & ~Configuration.USE_PROVER);
		DprMinAlphaTuner t = new DprMinAlphaTuner(c.programFiles,c.alpha);
	}

}
