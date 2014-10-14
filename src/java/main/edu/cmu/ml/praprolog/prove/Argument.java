package edu.cmu.ml.praprolog.prove;

public abstract class Argument implements Comparable<Argument> {
	public abstract boolean isConstant();
	public boolean isVariable() { return !this.isConstant(); }
	public String getName() { return String.valueOf(this.getValue()); }
	public int getValue() { return 0; }
	public static Argument fromString(String s) {
		return new ConstantArgument(s);
	}
	public int compareTo(Argument a) {
	    return this.getName().compareTo(a.getName());
	}
	@Override
	public boolean equals(Object o) {
	    if (!(o instanceof Argument)) return false;
	    return ((Argument) o).getName().equals(getName());
	}
	@Override
	public int hashCode() {
	    return getName().hashCode();
	}
}
