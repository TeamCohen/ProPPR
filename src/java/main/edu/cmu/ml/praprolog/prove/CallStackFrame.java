package edu.cmu.ml.praprolog.prove;

/**
 * Enough information to 'return' to a prior state.
 * @author "William Cohen <wcohen@cs.cmu.edu>"
 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
 *
 */
public class CallStackFrame {
	private int heapPointer;
	private int registerPointer;
	private int programCounter;
	private String jumpTo;
	public CallStackFrame(State state) {}

	public int getHeapPointer() {
		return heapPointer;
	}

	public int getRegisterPointer() {
		return registerPointer;
	}

	public int getProgramCounter() {
		return programCounter;
	}

	public String getJumpTo() {
		return jumpTo;
	}
	
}
