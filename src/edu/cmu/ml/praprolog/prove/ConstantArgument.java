package edu.cmu.ml.praprolog.prove;

public class ConstantArgument extends Argument {
	private String name;
	public ConstantArgument(String s) {
		this.name = s;
	}
	@Override
	public boolean isConstant() {
		return true;
	}

	@Override
	public String getName() {
		return name;
	}

	public String toString() {
		return "c["+this.name+"]";//String.format("c[%s]",this.name);
	}
}
