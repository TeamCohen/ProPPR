package edu.cmu.ml.praprolog.prove;

import java.util.Arrays;

import org.apache.log4j.Logger;

import edu.cmu.ml.praprolog.util.Dictionary;
import edu.cmu.ml.praprolog.util.SymbolTable;

public class Goal implements Comparable<Goal> {
	private static final Logger log = Logger.getLogger(Goal.class);
	protected String functor;
	protected Argument[] args;
	protected int hashcode;
	protected String name="";
	protected String argString;
	public Goal(String fnctr, String ... args) {
		this.functor = fnctr;
		this.args = new Argument[args.length];
		for (int i=0; i<args.length; i++) {
			this.args[i] = Argument.fromString(args[i]);
		}
		this.freeze();
	}
	public Goal(String fnctr, Argument[] args) {
		this.functor = fnctr;
		this.args = args; // TODO: may need defensive protection
		this.freeze();
	}
	/** (internal) set up hashcode and argstring **/
	protected void freeze() {
		hashcode = functor.hashCode();
		StringBuilder sb = new StringBuilder();
		for (Argument a : args) {
			hashcode += a.hashCode();
			sb.append(",").append(a.toString());
		}
		this.argString = sb.toString();
	}
	public String getFunctor() {
		return functor;
	}
	public int getArity() {
		return args.length;
	}
	public Argument[] getArgs() { // TODO: may need defensive protection
		return this.args;
	}
	public Argument getArg(int i) {
		return this.args[i];
	}
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("goal(").append(this.functor);
		return Dictionary.buildString(this.args, sb, ",",false).append(")").toString();
	}
	
	public String toSaveString() {
		StringBuilder sb = new StringBuilder(this.functor);
		if (this.args.length > 0) {
			sb.append("(");
			for (Argument a : this.args) sb.append(a.getName()).append(",");
			sb.deleteCharAt(sb.length()-1);
			sb.append(")");
		}
		return sb.toString();
	}
	
	private boolean isCompiled=false;
	public void compile(SymbolTable variableSymTab) {
		for (int i=0; i<args.length; i++) {
			String argstring = args[i].getName();
			if (args[i].isConstant() &&
					(argstring.startsWith("_") || argstring.matches("[A-Z].*"))) 
				args[i] = variableSymTab.getId(argstring);
		}
		this.freeze();
		isCompiled=true;
	}
	public boolean isCompiled() { return isCompiled; }
	@Override
	public boolean equals(Object o) { // FIXME probably slow
		if (!(o instanceof Goal)) return false;
		Goal g = (Goal) o;
		if (this.hashCode() != g.hashCode()) return false;
		if (!this.functor.equals(g.functor)) return false;
		if (this.args.length != g.args.length) return false;
		if (!this.argString.equals(g.argString)) return false;
//		for (int i=0; i<this.args.length; i++) 
//			if (! this.args[i].equals(g.args[i])) return false;
		return true;
	}
	@Override
	public int hashCode() { return hashcode; }
	
	/**
	 * Retrieve a Goal object from the compiled string, in format e.g.
	 *   predict,-1,-2
	 * @param string
	 * @return
	 */
	public static Goal decompile(String string) {
		String[] functor_args = string.split(",",2);
		if (functor_args.length == 0) throw new IllegalStateException("Couldn't locate functor in '"+string+"'");
		//functor-only case
		if (functor_args.length < 2) return new Goal(functor_args[0].trim());
		//otherwise parse the arguments
		String[] argstrings = functor_args[1].split(",");
		Argument[] args = new Argument[argstrings.length];
		for (int i=0; i<argstrings.length; i++) {
			argstrings[i] = argstrings[i].trim();
			try { 
				int a = Integer.parseInt(argstrings[i]);
				if (a<0) {
					args[i] = new VariableArgument(a);
					continue;
				}
			} catch (NumberFormatException e) {}
			args[i] = new ConstantArgument(argstrings[i]);
		}
		return new Goal(functor_args[0].trim(),args);
	}
	/**
	 * Create a goal from the string-delimited format
	 * 	   functor arg1 arg2 arg3 ...
	 * @param string
	 * @return
	 */
	public static Goal parseGoal(String string) {
		return parseGoal(string," ");
	}
	@Override
	public int compareTo(Goal arg0) {
		int c = this.functor.compareTo(arg0.functor);
		if (c != 0) return c;
		if (this.args.length != arg0.args.length)
			return this.args.length - arg0.args.length;
		for (int i=0; i<this.args.length; i++) {
			c = this.args[i].compareTo(arg0.args[i]);
			if (c != 0) return c;
		}
		return 0;
	}
	public static Goal parseGoal(String string, String delim) {
		String[] f_a = string.split(delim,2);
		if (f_a.length > 1) return new Goal(f_a[0],f_a[1].split(delim));
		return new Goal(f_a[0]);
	}
}
