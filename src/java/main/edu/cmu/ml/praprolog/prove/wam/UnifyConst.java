package edu.cmu.ml.praprolog.prove.wam;

import edu.cmu.ml.praprolog.prove.MutableState;
import edu.cmu.ml.praprolog.prove.WamInterpreter;
import edu.cmu.ml.praprolog.prove.v1.LogicProgramException;
/**
 * Check that constant a is equal to something stored in the heap.  If
        the heap cell is a free variable, then bind it to the
        constant.  If the heap cell is bound to different constant,
        fail.
 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
 *
 */
public class UnifyConst extends Instruction {
	private String a;
	private int relativeHeapIndex;
	public UnifyConst(String[] args) {
		super(args);
		a = args[0];
		relativeHeapIndex = Integer.parseInt(args[1]);
		if (relativeHeapIndex>=0) throw new IllegalStateException("relative heap index must be <0");
	}

	@Override
	public void execute(WamInterpreter interp) throws LogicProgramException {
		MutableState state = interp.getState();
		int i = state.getHeapSize() + relativeHeapIndex;
		int ri = state.dereference(i);
		if (!state.hasConstantAt(ri)) {
			if (!interp.getConstantTable().hasId(a)) {
				state.setFailed(true);
			} else {
				int aid = interp.getConstantTable().getId(a);
				state.setFailed(state.getIdOfConstantAt(ri) != aid);
			}
		} else {
			if (!state.hasFreeAt(ri)) throw new LogicProgramException("ri(="+ri+") is neither free nor constant?");
			state.setHeap(ri, state.createConstantCell(interp.getConstantTable().getId(a)));
			state.collapsePointers(i, ri);
		}
		state.incrementProgramCounter();
	}

}
