package edu.cmu.ml.praprolog.prove.wam;

import edu.cmu.ml.praprolog.prove.wam.Argument;

public class Goal implements Comparable<Goal> {
    protected String functor;
    protected Argument[] args;
    protected int arity;
	public Goal(String functor, Argument ... args) {
		this.functor = functor;
		this.args = args;
		this.arity = args.length;
	}
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(functor);
		if (this.arity > 0) {
			sb.append("(");
			boolean first=true;
			for (Argument a : args) {
				if (!first) sb.append(",");
				first=false;
				sb.append(a.getName());
			}
			sb.append(")");
		}
		return sb.toString();
	}
	public String getFunctor() {
		return functor;
	}
	public Argument[] getArgs() {
		return args;
	}
	public Argument getArg(int i) {
		return args[i];
	}
	public int getArity() {
		return this.arity;
	}
	@Override
	public int compareTo(Goal o) {
		int k = this.functor.compareTo(o.functor);
		if (k!=0) return k;
		for (int i=0; i<this.arity; i++) {
			if (i>= o.arity) return -1;
			k = this.args[i].getName().compareTo(o.args[i].getName());
			if (k!=0) return k;
		}
		return 0;
	}
}
