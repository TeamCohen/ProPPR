package edu.cmu.ml.proppr.prove.wam;

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.util.Dictionary;

/**
 * State of the interpreter.  States are stored and retrieved to allow
        for backtracking, and to build a proof graph.  You can call

        savedState = wamInterp.state.save()
        
        to save an interpreter state, and 

        wamInterp.state = State.restore(savedState)

        to restore one. Saved states are immutable and hashable.
 * @author "William Cohen <wcohen@cs.cmu.edu>"
 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
 *
 */
public abstract class State {
	protected int[] heap;
	protected int[] registers;
	protected int pc;
	protected String jumpTo;
	protected boolean completed;
	protected boolean failed;
	protected List<CallStackFrame> calls;
	/** True iff there is a constant at heap position i. */
	public boolean hasConstantAt(int i) { return heap[i]<0; }
	/** True iff there is a variable at heap position i. */
	public boolean hasVariableAt(int i) { return heap[i]>=0; }
	/** True iff there is an unbound variable at heap position i. */
	public boolean hasFreeAt(int i) { return heap[i]==i; }
	/** Get the value of the variable stored at this heap position. */
	public int getVariableAt(int i) { return heap[i]; }
	/**
	 * Get the id, in the Interpreter's constantTable, of the constant
            stored in this heap position.
	 * @param i
	 * @return
	 */
	public int getIdOfConstantAt(int i) { 
		if (heap[i]>=0) throw new InvalidHeapException();
		return -heap[i];
	}
	/** Create a heap cell that stores a constant with the given id */
	public int createConstantCell(int id) {
		if (id<1) throw new IllegalArgumentException();
		return -id;
	}
	/** Create a heap cell that stores a variable bound to heap position a */
	public int createVariableCell(int a) { return a; }
	/** Create a copy of the constant cell at position i. */
	public int copyConstantCell(int i) { return heap[i]; }
	/**
	 * Dereference a variable, ie, follow pointers till you reach an
            unbound variable or a constant.
	 * @param i
	 * @return
	 */
	public int dereference(int heapIndex) {
		while( !hasConstantAt(heapIndex) && !hasFreeAt(heapIndex)) {
			heapIndex = getVariableAt(heapIndex);
		}
		return heapIndex;
	}
	
	/** Immutable, hashable version of this state. */
	public abstract ImmutableState immutableVersion();
	/** Restore from a copy produced by save(). */
	public abstract MutableState mutableVersion();
	/** */
	public boolean isCompleted() {
		return this.completed;
	}
	public boolean isFailed() {
		return this.failed;
	}	
	public int getHeapSize() {
		return heap.length;
	}

	
	public int getRegisterSize() {
		return registers.length;
	}

	
	public int getProgramCounter() {
		return pc;
	}

	
	public String getJumpTo() {
		return jumpTo;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("state<");
		buildHeapString(sb);
		sb.append(" ");
		buildRegisterString(sb);
		sb.append(" ");
		buildCallStackString(sb);
		sb.append(" ");
		sb.append(pc).append(" ").append(jumpTo);
		sb.append(">");
		if (completed) sb.append("*");
		if (failed) sb.append("!");
		return sb.toString();
	}
	
	protected void buildHeapString(StringBuilder sb) {
		sb.append("h[");
		Dictionary.buildString(heap, sb, " ");
		sb.append("]");
	}
	protected void buildCallStackString(StringBuilder sb) {
		sb.append("c[?]");
	}
	protected void buildRegisterString(StringBuilder sb) {
		sb.append("r[");
		Dictionary.buildString(registers, sb, " ");
		sb.append("]");
	}
	public int[] getRegisters() {
		return this.registers;
	}
	
	@Override
	public boolean equals(Object o) {
		if (! (o instanceof State)) return false;
		State s = (State) o;
		if (this.getHeapSize() != s.getHeapSize() ||
				this.getRegisterSize() != s.getRegisterSize() ||
				this.calls.size() != s.calls.size() ||
				this.pc != s.pc ||
				this.completed != s.completed ||
				this.failed != s.failed)
			return false;
		for (int i=0; i<this.getHeapSize(); i++) {
			if (heap[i] != s.heap[i]) return false;
		}
		for (int i=0; i<this.getRegisterSize(); i++) {
			if (registers[i] != s.registers[i]) return false;
		}
		Iterator<CallStackFrame> it = this.calls.iterator(),
				sit = s.calls.iterator();
		while(it.hasNext()) {
			CallStackFrame me = it.next();
			CallStackFrame them = sit.next();
			if (!me.equals(them)) return false;
		}
		return true;
	}
}
