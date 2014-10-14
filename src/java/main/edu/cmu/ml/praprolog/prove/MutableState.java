package edu.cmu.ml.praprolog.prove;

import java.util.List;

public class MutableState extends State {
	private int pc;
	private int hn;
	private String jumpTo;
	private boolean completed;
	private boolean failed;
	private int[] registers;
	private int rn;
	
	public MutableState() {
		this.heap = new int[1];
		this.hn=1;
		
		this.registers = new int[1];
		this.rn = 1;
	}

	@Override
	public ImmutableState immutableVersion() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MutableState mutableVersion() {
		return this;
	}

	@Override
	public void collapsePointers(int i, int last) {
		// TODO Auto-generated method stub
		
	}


	public List<CallStackFrame> getCalls() {
		// TODO Auto-generated method stub
		return null;
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


	public void truncateVarNameList(int newsize) {
		// TODO Auto-generated method stub
	}

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

	public int getRegister(int a) {
		return registers[a];
	}
	public void setRegister(int a, int i) {
		registers[a] = i;
	}	
	public void addRegisters(int n) {
		int newrn = rn + n;
		if (newrn >= registers.length) registers = extend(registers,rn,newrn);
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


	private static int[] extend(int[] data, int cursize) {
		return extend(data,cursize,cursize*2);
	}
	private static int[] extend(int[] data, int cursize, int newsize) {
		int[] newdata = new int[newsize];
		for (int i=0; i<cursize; i++) newdata[i] = data[i];
		return newdata;
	}

}
