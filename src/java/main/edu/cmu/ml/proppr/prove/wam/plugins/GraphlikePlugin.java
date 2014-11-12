package edu.cmu.ml.proppr.prove.wam.plugins;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.cmu.ml.proppr.prove.wam.Goal;
import edu.cmu.ml.proppr.prove.wam.LogicProgramException;
import edu.cmu.ml.proppr.prove.wam.Outlink;
import edu.cmu.ml.proppr.prove.wam.State;
import edu.cmu.ml.proppr.prove.wam.WamInterpreter;

public abstract class GraphlikePlugin extends WamPlugin {
	protected abstract boolean indexContains(String label);
	protected abstract List<String> indexGet(String label, String src);
	protected abstract Collection<String> indexGet(String label);
	protected abstract void indexAdd(String label, String src, String dst);
	protected abstract Map<Goal,Double> getFD();
	
	public void addEdge(String functor, String src, String dst) {
		indexAdd(functor+"/2",src,dst);
	}
	
	@Override
	public boolean claim(String jumpto) {
		return indexContains(jumpto);
	}

	@Override
	public void restartFD(State state, WamInterpreter wamInterp) {
		throw new RuntimeException("Not yet implemented");
	}

	@Override
	public List<Outlink> outlinks(State state, WamInterpreter wamInterp,
			boolean computeFeatures) throws LogicProgramException {
		List<Outlink> result = new LinkedList<Outlink>();
		String srcConst = wamInterp.getConstantArg(2,1);
		String dstConst = wamInterp.getConstantArg(2,2);
		if(srcConst == null) {
			//throw new LogicProgramException("predicate "+state.getJumpTo()+" called with non-constant first argument!");
			for (String src : indexGet(state.getJumpTo())) {
				wamInterp.restoreState(state);
				wamInterp.setArg(2,1,src);
				State srcState = wamInterp.saveState();
				outlinksPerSource(srcState, wamInterp, computeFeatures, src, dstConst, result);
			}
		} else {
			outlinksPerSource(state, wamInterp, computeFeatures, srcConst, dstConst, result);
		}
		
		return result;
	}
	
	private void outlinksPerSource(State state, WamInterpreter wamInterp, boolean computeFeatures, String srcConst, String dstConst, List<Outlink> result) throws LogicProgramException {
		List<String> values = this.indexGet(state.getJumpTo(), srcConst);
		if (!values.isEmpty()) {
			for (String val : values) {
				if (dstConst != null && val==dstConst) {
					wamInterp.restoreState(state);
					wamInterp.returnp();
					wamInterp.executeWithoutBranching();
				} else if (dstConst == null) {
                    wamInterp.restoreState(state);
                    wamInterp.setArg(2,2,val);
                    wamInterp.returnp();
                    wamInterp.executeWithoutBranching();
				}
				if (computeFeatures) {
					result.add(new Outlink(getFD(), wamInterp.saveState()));
				} else {
					result.add(new Outlink(null, wamInterp.saveState()));
				}
			}
		}
	}
}
