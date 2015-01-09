package edu.cmu.ml.proppr.prove.wam;

import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.List;

public abstract class WamProgram {
	protected Compiler compiler;
	public WamProgram() {
		compiler = new Compiler();
	}
	public void append(Rule r) {
		compiler.compileRule(r, this);
	}
	
	public abstract void append(Instruction inst);

	public abstract void setInstruction(int placeToPatch,
			Instruction instruction);

	public abstract int size();

	public abstract Instruction getInstruction(int addr);

	public abstract void insertLabel(String label);

	public abstract boolean hasLabel(String jumpTo);

	public abstract List<Integer> getAddresses(String jumpTo);

	public abstract void save();

	public abstract void revert();

	public static WamProgram load(File file) throws IOException {
		return WamBaseProgram.load(file);
	}
	public static WamProgram load(LineNumberReader reader) throws IOException {
		return WamBaseProgram.load(reader);
	}
}