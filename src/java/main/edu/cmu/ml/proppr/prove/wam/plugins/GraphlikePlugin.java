package edu.cmu.ml.proppr.prove.wam.plugins;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.cmu.ml.proppr.prove.wam.Goal;
import edu.cmu.ml.proppr.prove.wam.LogicProgramException;
import edu.cmu.ml.proppr.prove.wam.Outlink;
import edu.cmu.ml.proppr.prove.wam.State;
import edu.cmu.ml.proppr.prove.wam.WamInterpreter;
import edu.cmu.ml.proppr.util.APROptions;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.procedure.TObjectDoubleProcedure;

/**
 * An 'extensional database' - restricted to be a labeled directed
    graph, or equivalently, a set of f(+X,-Y) unit predicates.
    
 * Alpha is used to limit the minimum restart weight, when you
        use a uniformWeighter (or something related, like a fdWeighter
        with learned weights that are close to 1.0).
        
        With unit feature weights, a graph node of degree n will lead
        to an lpState with degree n+1, and have a restart weight that
        is 1/(n+1).  With alpha set, a new feature (named
        'alphaBooster') is introduced with a non-unit VALUE of n *
        (alpha/(1-alpha)) for the restart weight, which means that
        unit weights will give that edge a total weight of alpha.
 * 
 * @author "William Cohen <wcohen@cs.cmu.edu>"
 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
 *
 */
public abstract class GraphlikePlugin extends WamPlugin {
	protected static final TObjectDoubleMap<String> DEFAULT_DSTLIST = new TObjectDoubleHashMap<String>(0);
	protected static final List<String> DEFAULT_SRCLIST = Collections.emptyList();
	protected static final double DEFAULT_DSTWEIGHT = 1.0;
	protected static final String GRAPH_ARITY = "/2";
	public static final String FILE_EXTENSION = "graph";

	protected abstract boolean indexContains(String label);
	protected abstract TObjectDoubleMap<String> indexGet(String label, String src);
	protected abstract Collection<String> indexGet(String label);
	protected abstract void indexAdd(String label, String src, String dst);
	protected abstract Map<Goal,Double> getFD();
	
	public GraphlikePlugin(APROptions apr) {
		super(apr);
	}
	
	public void addEdge(String functor, String src, String dst) {
		indexAdd(functor+GRAPH_ARITY,src,dst);
	}
	
	@Override
	public boolean claim(String jumpto) {
		if (!jumpto.endsWith(GRAPH_ARITY)) return false;
		return indexContains(jumpto);
	}

//	@Override
//	public void restartFD(State state, WamInterpreter wamInterp) {
//		throw new RuntimeException("Not yet implemented");
//	}

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
	
	private void outlinksPerSource(final State state, final WamInterpreter wamInterp, 
			final boolean computeFeatures, final String srcConst, final String dstConst, final List<Outlink> result) throws LogicProgramException {
		TObjectDoubleMap<String> values = this.indexGet(state.getJumpTo(), srcConst);
		if (!values.isEmpty()) {
			try {
				values.forEachEntry(new TObjectDoubleProcedure<String>() {
					@Override
					public boolean execute(String val, double wt) {
						try {
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
								result.add(new Outlink(scaleFD(getFD(), wt), wamInterp.saveState()));
							} else {
								result.add(new Outlink(null, wamInterp.saveState()));
							}
						} catch (LogicProgramException e) {
							// wow this is awkward but whatcha gonna do
							throw new IllegalStateException(e);
						}
						return true;
					}
				});
			} catch (IllegalStateException e) {
				if (e.getCause() instanceof LogicProgramException)
					throw (LogicProgramException) e.getCause();
			}
		}
	}
	
	private Map<Goal,Double> scaleFD(Map<Goal,Double> fd, double wt) {
		if (wt == 1.0) return fd;
		Map<Goal,Double> ret = new HashMap<Goal,Double>();
		ret.putAll(fd);
		for (Map.Entry<Goal, Double> val : fd.entrySet()) {
			val.setValue(val.getValue() * wt);
		}
		return ret;
	}
}
