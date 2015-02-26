package edu.cmu.ml.proppr.prove.wam;

public class VariableArgument extends Argument {
	private int index;
	private String name;

	public VariableArgument(int i) {
		this.index = i;
		this.name = "X"+String.valueOf(Math.abs(this.index));
	}
	
	@Override
	public boolean isConstant() {
		return false;
	}
	
	@Override
	public int getValue() { return this.index; }
	@Override
	public String getName() { return this.name; }
	
	public String toString() {
		return this.getName();
	}
}
