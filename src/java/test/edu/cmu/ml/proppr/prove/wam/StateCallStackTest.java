package edu.cmu.ml.proppr.prove.wam;

import static org.junit.Assert.*;

import java.util.Iterator;

import org.junit.Test;

public class StateCallStackTest {

	@Test
	public void test() {
		MutableState state = new MutableState();
		MutableState[] stack = new MutableState[5];
		for (int i=0; i<5; i++) {
			stack[i] = new MutableState();
			stack[i].setJumpTo(String.valueOf(i));
			stack[i].setProgramCounter(i);
		}
		for (int i=5; i>0; i--) {
			state.getCalls().push(new CallStackFrame(stack[i-1]));
		}
		assertTrue("sanity check", state.calls.peekFirst().getProgramCounter() != state.calls.peekLast().getProgramCounter());
		
		ImmutableState test = new ImmutableState(state);
		
		assertEquals("Peek", state.calls.peek(), test.calls.peek());
		
		Iterator<CallStackFrame> si = state.getCalls().iterator();
		Iterator<CallStackFrame> ti = test.calls.iterator();
		
		while(si.hasNext()) {
			assertTrue("Immutable version shorter than source", ti.hasNext());
			CallStackFrame sc = si.next();
			CallStackFrame tc = ti.next();
			assertEquals("Program counter", sc.getProgramCounter(), tc.getProgramCounter());
		}
		
	}

}
