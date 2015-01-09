package edu.cmu.ml.proppr.prove.wam;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * An immutable, hashable version of an interpreter state.
 * @author "William Cohen <wcohen@cs.cmu.edu>"
 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
 *
 */
public class ImmutableState extends State {
	public ImmutableState(MutableState state) {
		this.heap = Arrays.copyOf(state.heap,state.getHeapSize());//new int[state.getHeapSize()];
//		for (int i=0; i<state.getHeapSize(); i++) this.heap[i] = state.heap[i];
		this.registers = Arrays.copyOf(state.registers, state.getRegisterSize()); //new int[state.getRegisterSize()];
//		for (int i=0; i<state.getRegisterSize(); i++) this.registers[i] = state.registers[i];
		this.calls = new ArrayList<CallStackFrame>(state.calls.size()); this.calls.addAll(state.calls);
		// TODO: varNameList
		
		this.pc = state.getProgramCounter();
		this.jumpTo = state.getJumpTo();
		this.completed = state.isCompleted();
		this.failed = state.isFailed();
	}
	
	@Override
	public int hashCode() {
		return ((Arrays.hashCode(heap) ^ Arrays.hashCode(registers) ^ pc ^ (jumpTo!=null ? jumpTo.hashCode() : 0)) << 2) ^ (completed?1:0) ^ (failed?2:0);
	}

	@Override
	public ImmutableState immutableVersion() {
		return this;
	}

	@Override
	public MutableState mutableVersion() {
		MutableState results = new MutableState(this);
		return results;
	}
}
