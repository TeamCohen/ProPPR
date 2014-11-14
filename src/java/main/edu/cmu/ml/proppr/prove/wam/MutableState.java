package edu.cmu.ml.proppr.prove.wam;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.util.Dictionary;

/**
 * An AbstractState that can be modified.  This is the representation
        of interpreter state that is used in execution.
 * @author "William Cohen <wcohen@cs.cmu.edu>"
 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
 *
 */
public class MutableState extends State {
	private static final Logger log = Logger.getLogger(MutableState.class);
	private int hn;
	private int rn;
	
	public MutableState() {
		this.heap = new int[0];
		this.hn = 0;
		
		this.registers = new int[0];
		this.rn = 0;
		
		this.pc = -1;
		
		this.calls = new ArrayList<CallStackFrame>();
	}

	public MutableState(ImmutableState state) {
		this.heap = new int[state.getHeapSize()];
		for (int i=0; i<state.getHeapSize(); i++) this.heap[i] = state.heap[i];
		this.registers = new int[state.getRegisterSize()];
		for (int i=0; i<state.getRegisterSize(); i++) this.registers[i] = state.registers[i];
		this.calls = new ArrayList<CallStackFrame>(); this.calls.addAll(state.calls);
		
		this.hn = this.heap.length;
		this.rn = this.registers.length;
		
		this.pc = state.pc;
		this.jumpTo = state.jumpTo;
		this.completed = state.completed;
		this.failed = state.failed;
	}

	@Override
	public ImmutableState immutableVersion() {
		return new ImmutableState(this);
	}

	@Override
	public MutableState mutableVersion() {
		return this;
	}

	/**
	 * Given index i, the start of a chain of heap variables that ends at
            index finalIndex, make all the variables in that chain
            point directly to finalIndex.
	 * @param i
	 * @param last
	 * @throws LogicProgramException 
	 */
	public void collapsePointers(int i, int last) throws LogicProgramException {
		while( i!= last && this.hasVariableAt(i) ) {
			int nexti=getVariableAt(i);
			if (last >= i) throw new LogicProgramException("Bad collapsePointers from "+i+" to "+last+": "+this.toString());
			this.heap[i] = this.createVariableCell(last);
			i = nexti;
		}
		
	}


	public List<CallStackFrame> getCalls() {
		return this.calls;
	}

	public void setJumpTo(String pred) {
		this.jumpTo = pred;
	}

	public void incrementProgramCounter() {
		pc++;
	}
	public void decrementProgramCounter() {
		pc--;
	}
	public void setProgramCounter(int i) {
		pc = i;
	}


//	public void truncateVarNameList(int newsize) {
//		throw new RuntimeException("Bad programmer! Finish your vegetables");
//	}

	@Override
	public int getHeapSize() {
		return hn;
	}
	public void setHeap(int hi, int cell) {
		if (hi >= hn) throw new InvalidHeapException();
		heap[hi] = cell;
	}
	public void appendHeap(int cell) {
		if (hn >= heap.length) heap = extend(heap,hn);
		heap[hn] = cell;
		hn++;
	}
	public void truncateHeap(int newsize) {
		hn = newsize;
	}

	@Override
	public int getRegisterSize() {
		return rn;
	}
	public int getRegister(int a) {
		return registers[rn + a];
	}
	public void setRegister(int a, int i) {
		registers[rn + a] = i;
	}	
	public void addRegisters(int n) {
		int newrn = rn + n;
		if (newrn >= registers.length) registers = extend(registers,rn,newrn);
		for (int i=rn; i<newrn; i++) registers[i] = -1;
		rn = newrn;
	}
	public void truncateRegisters(int newsize) {
		rn = newsize;
	}
	

	public void setCompleted(boolean b) {
		this.completed = b;
	}
	public void setFailed(boolean b) {
		this.failed = b;
	}
	
	@Override
	protected void buildHeapString(StringBuilder sb) {
		sb.append("h[");
		for (int i=0; i<hn; i++) {
			if (i>0) sb.append(" ");
			sb.append(heap[i]);
		}
		sb.append("]");
	}
	@Override
	protected void buildRegisterString(StringBuilder sb) {
		sb.append("r[");
		for (int i=0; i<rn; i++) {
			if (i>0) sb.append(" ");
			sb.append(registers[i]);
		}
		sb.append("]");
	}

	private static int[] extend(int[] data, int cursize) {
		return extend(data,cursize,Math.max(1,cursize*2));
	}
	private static int[] extend(int[] data, int cursize, int newsize) {
		int[] newdata = new int[newsize];
		for (int i=0; i<cursize; i++) newdata[i] = data[i];
		return newdata;
	}



}
