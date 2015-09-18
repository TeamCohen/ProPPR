package edu.cmu.ml.proppr.prove.wam;

import java.util.ArrayList;
import java.util.List;

public class WamQueryProgram extends WamProgram {
	private WamProgram masterProgram;
	private WamProgram queryProgram;
	public WamQueryProgram(WamProgram master) {
		if (master == null) master = new WamBaseProgram();
		this.masterProgram = master;
	} 
	@Override
	public void append(Instruction inst) {
		this.queryProgram.append(inst);
	}
	@Override
	public void setInstruction(int placeToPatch, Instruction instruction) {
		this.queryProgram.setInstruction(placeToPatch, instruction);
	}
	@Override
	public int size() {
		return masterProgram.size() + queryProgram.size();
	}
	@Override
	public Instruction getInstruction(int addr) {
		if (addr < masterProgram.size()) return masterProgram.getInstruction(addr);
		return queryProgram.getInstruction(addr-masterProgram.size());
	}
	@Override
	public void insertLabel(String label) {
		queryProgram.insertLabel(label);
	}
	@Override
	public boolean hasLabel(String jumpTo) {
		return masterProgram.hasLabel(jumpTo) || queryProgram.hasLabel(jumpTo);
	}
	@Override
	public List<Integer> getAddresses(String jumpTo) {
		List<Integer> master = masterProgram.getAddresses(jumpTo);
		List<Integer> query  = queryProgram.getAddresses(jumpTo);
		if (query==null || query.isEmpty()) return master;
		ArrayList<Integer> ret = new ArrayList<Integer>(master.size() + query.size());
		ret.addAll(master);
		int offset = masterProgram.size();
		for (Integer k : query) ret.add(offset + k);
		return ret;
	}
	@Override
	public void save() {
		// do nothing
	}
	@Override
	public void revert() {
		this.queryProgram = new WamBaseProgram();
	}
}
