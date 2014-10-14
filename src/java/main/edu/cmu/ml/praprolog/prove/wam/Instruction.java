package edu.cmu.ml.praprolog.prove.wam;

import edu.cmu.ml.praprolog.prove.WamInterpreter;
import edu.cmu.ml.praprolog.prove.v1.LogicProgramException;

public abstract class Instruction {
	public static enum OP {
		comment,
		allocate,
		callp,
		returnp,
		pushconst,
		pushfreevar,
		pushboundvar,
		initfreevar,
		fclear,
		fpushstart,
		fpushconst,
		fpushboundvar,
		freport,
		ffindall
	};
	public Instruction(String[] args) {}
	public static Instruction parseInstruction(String line) {
		String[] parts = line.split("\t",4);
		String[] args = parts[3].split("\t");
		switch(OP.valueOf(parts[2])) {
		case comment: return null;
		case allocate: return new Allocate(args);
		}
		throw new UnsupportedOperationException("No known instruction '"+parts[2]+"'");
	}
	public abstract void execute(WamInterpreter interp) throws LogicProgramException;
}
