package edu.cmu.ml.proppr.prove.v1;

import java.util.ArrayDeque;

import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.util.Dictionary;

public class Backtrace<T> {
	private Logger log;
	public Backtrace(Logger parent) { this.log = parent; }
	private ArrayDeque<T> backtrace;
	public void start() {
		this.backtrace = new ArrayDeque<T>();
	}
	public void push(T state) {
		this.backtrace.push(state);
	}
	public void pop(T state) {
		T p = this.backtrace.pop();
		if (!p.equals(state)) log.error("popped unexpected state\nexpected "+state+"\ngot"+p);
	}
	public void print(LogicProgramException e) {
		StringBuilder sb = new StringBuilder(e.getMessage()+"\nLogic program backtrace:\n");
		Dictionary.buildString(this.backtrace, sb, "\n");
		throw new IllegalStateException(sb.toString(),e);
	}
}
