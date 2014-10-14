package edu.cmu.ml.praprolog.prove.wam;

import edu.cmu.ml.praprolog.prove.WamInterpreter;

/**
 * Allocate n new variable registers, associated with the
        given variable names.  These will be accessed as
        self.registers[self.rp + a], and will hold indices into
        the heap.
 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
 *
 */
public class Allocate extends Instruction {
	private int n;
	private String[] varNames;
	public Allocate(String[] args) {
		super(args);
		n = Integer.parseInt(args[0]);
		if (n<0) throw new IllegalArgumentException("n must be >= 0");
		varNames = new String[args.length-1];
		for (int i=1;i<args.length; i++) varNames[i-1]=args[i];
	}

	@Override
	public void execute(WamInterpreter interp) {
		interp.getState().addRegisters(n);
		// TODO: debugmode
		interp.getState().incrementProgramCounter();
	}
}
