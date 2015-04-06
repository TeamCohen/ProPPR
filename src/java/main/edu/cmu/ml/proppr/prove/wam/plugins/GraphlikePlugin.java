package edu.cmu.ml.proppr.prove.wam.plugins;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

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

	 As an alternative usage, the predicate f#(+X,-Y,-W) will return
	 the weight assigned to the edge, encoded as an atom which
	 can be converted back to a double with Doubel.parseDouble()
 * 
 * @author "William Cohen <wcohen@cs.cmu.edu>"
 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
 *
 */
public abstract class GraphlikePlugin extends WamPlugin {
	private static final Logger log = Logger.getLogger(LightweightGraphPlugin.class);

	protected static final TObjectDoubleMap<String> DEFAULT_DSTLIST = new TObjectDoubleHashMap<String>(0);
	protected static final List<String> DEFAULT_SRCLIST = Collections.emptyList();
	protected static final double DEFAULT_DSTWEIGHT = 1.0;
	protected static final String GRAPH_ARITY = "/2";
	protected static final String WEIGHTED_GRAPH_SUFFIX_PLUS_ARITY = "#/3";
	public static final String WEIGHTED_GRAPH_SUFFIX = "#";
	public static final String FILE_EXTENSION = "graph";

	protected abstract boolean indexContains(String label);
	protected abstract TObjectDoubleMap<String> indexGet(String label, String src);
	protected abstract Collection<String> indexGet(String label);
	protected abstract void indexAdd(String label, String src, String dst);
	protected abstract void indexAdd(String label, String src, String dst,double weight);
	protected abstract Map<Goal,Double> getFD();

	public GraphlikePlugin(APROptions apr) {
		super(apr);
	}

	public void addEdge(String functor, String src, String dst) {
		indexAdd(functor+GRAPH_ARITY,src,dst);
	}

	public void addEdge(String functor, String src, String dst,double weight) {
		if (weight<=0) {
			log.error("Negative weights discarded for graph edge"+functor+"("+src+","+dst+")");
		} else { 
			indexAdd(functor+GRAPH_ARITY,src,dst,weight);
		}
	}

	@Override
	public boolean claim(String jumpto) {
		return indexContains(jumpto);
	}

	@Override
	public List<Outlink> outlinks(State state, WamInterpreter wamInterp,
			boolean computeFeatures) throws LogicProgramException {
		List<Outlink> result = new LinkedList<Outlink>();
		String srcConst,dstConst,weightConst,indexKey;
		boolean returnWeights = state.getJumpTo().endsWith(WEIGHTED_GRAPH_SUFFIX_PLUS_ARITY);
		if (!returnWeights) {
			srcConst = wamInterp.getConstantArg(2,1);
			dstConst = wamInterp.getConstantArg(2,2);
			weightConst = null;
			indexKey = state.getJumpTo();
		} else {
			srcConst = wamInterp.getConstantArg(3,1);
			dstConst = wamInterp.getConstantArg(3,2);
			weightConst = wamInterp.getConstantArg(3,3);
			indexKey = unweightedJumpto(state.getJumpTo());
		}
		if (returnWeights && weightConst!=null) {
			throw new LogicProgramException("predicate "+state.getJumpTo()+" called with bound third argument!");
		}
		if(srcConst == null) {
			//throw new LogicProgramException("predicate "+state.getJumpTo()+" called with non-constant first argument!");
			for (String src : indexGet(indexKey)) {
				wamInterp.restoreState(state);
				wamInterp.setArg(2,1,src);
				State srcState = wamInterp.saveState();
				outlinksPerSource(srcState, wamInterp, computeFeatures, returnWeights, indexKey, src, dstConst, weightConst, result);
			}
		} else {
			outlinksPerSource(state, wamInterp, computeFeatures, returnWeights, indexKey, srcConst, dstConst, weightConst, result);
		}
		return result;
	}

	private void outlinksPerSource(final State state, final WamInterpreter wamInterp, 
			final boolean computeFeatures, final boolean returnWeights, final String indexKey,
			final String srcConst, final String dstConst,final String weightConst,
			final List<Outlink> result) throws LogicProgramException 
	{
		TObjectDoubleMap<String> values = this.indexGet(indexKey, srcConst);
		if (!values.isEmpty()) {
			try {
				values.forEachEntry(new TObjectDoubleProcedure<String>() {
					@Override
					public boolean execute(String val, double wt) {
						try {
							String weightString = returnWeights ? Double.toString(wt) : null;
							if (dstConst != null && val==dstConst) {
								wamInterp.restoreState(state);
								if (returnWeights) {
									wamInterp.setArg(3,3,weightString);
								}
								wamInterp.returnp();
								wamInterp.executeWithoutBranching();
							} else if (dstConst == null) {
								wamInterp.restoreState(state);
								if (returnWeights) {
									wamInterp.setArg(3,2,val);										
									wamInterp.setArg(3,3,weightString);
								} else {
									wamInterp.setArg(2,2,val);
								}
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
				// awkward c.f. above
				if (e.getCause() instanceof LogicProgramException)
					throw (LogicProgramException) e.getCause();
			}
		}
	}

	private Map<Goal,Double> scaleFD(Map<Goal,Double> fd, double wt) {
		if (wt == 1.0) return fd;
		Map<Goal,Double> ret = new HashMap<Goal,Double>();
		ret.putAll(fd);
		for (Map.Entry<Goal, Double> val : ret.entrySet()) {
			val.setValue(val.getValue() * wt);
		}
		return ret;
	}
}
