package edu.cmu.ml.proppr.prove.wam.plugins;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.prove.wam.Feature;
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
	private static final Logger log = Logger.getLogger(GraphlikePlugin.class);
	protected static final TObjectDoubleMap<String> DEFAULT_DSTLIST = new TObjectDoubleHashMap<String>(0);
	protected static final List<String> DEFAULT_SRCLIST = Collections.emptyList();
	protected static final String GRAPH_ARITY = "/2";
	public static final String FILE_EXTENSION = "graph";

	protected abstract boolean indexContains(String label);
	protected abstract TObjectDoubleMap<String> indexGet(String label, String src);
	protected abstract Collection<String> indexGet(String label);
	protected abstract void indexAdd(String label, String src, String dst);
	protected abstract void indexAdd(String label, String src, String dst,double weight);
	protected abstract Map<Feature,Double> getFD();

	public GraphlikePlugin(APROptions apr) {
		super(apr);
	}

	public void addEdge(String functor, String src, String dst) {
		indexAdd(functor+GRAPH_ARITY,src,dst);
	}

	public void addEdge(String functor, String src, String dst,double weight) {
		if (weight<=0) {
			log.error("Weights must be positive. Discarded graph edge "+functor+"("+src+","+dst+") with weight "+weight);
		} else { 
			indexAdd(functor+GRAPH_ARITY,src,dst,weight);
		}
	}

	@Override
	public boolean _claim(String jumpto) {
		return indexContains(jumpto);
	}

	@Override
	public List<Outlink> outlinks(State state, WamInterpreter wamInterp,
			boolean computeFeatures) throws LogicProgramException {
		List<Outlink> result = new LinkedList<Outlink>();
		String indexKey = state.getJumpTo();
		int delim = indexKey.indexOf(WamInterpreter.JUMPTO_DELIMITER);
		int arity = Integer.parseInt(indexKey.substring(delim+1));
		boolean returnWeights = indexKey.substring(0,delim).endsWith(WamPlugin.WEIGHTED_SUFFIX);
		
		String srcConst = wamInterp.getConstantArg(arity,1);
		String dstConst = wamInterp.getConstantArg(arity,2);
		String weightConst = null;
		if (returnWeights) {
			indexKey = unweightedJumpto(indexKey);
			weightConst = wamInterp.getConstantArg(arity,3);
			if (weightConst!=null) {
				throw new LogicProgramException("predicate "+state.getJumpTo()+" called with bound third argument!");
			}
		}
		if(srcConst == null) {
			//throw new LogicProgramException("predicate "+state.getJumpTo()+" called with non-constant first argument!");
			for (String src : indexGet(indexKey)) {
				wamInterp.restoreState(state);
				wamInterp.setArg(arity,1,src);
				State srcState = wamInterp.saveState();
				outlinksPerSource(srcState, wamInterp, computeFeatures, returnWeights, indexKey, src, dstConst, weightConst, result, arity);
			}
		} else {
			outlinksPerSource(state, wamInterp, computeFeatures, returnWeights, indexKey, srcConst, dstConst, weightConst, result, arity);
		}
		return result;
	}

	private void outlinksPerSource(final State state, final WamInterpreter wamInterp, 
			final boolean computeFeatures, final boolean returnWeights, final String indexKey,
			final String srcConst, final String dstConst,final String weightConst,
			final List<Outlink> result, final int arity) throws LogicProgramException 
	{
		TObjectDoubleMap<String> values = this.indexGet(indexKey, srcConst);
		if (!values.isEmpty()) {
			try {
				values.forEachEntry(new TObjectDoubleProcedure<String>() {
					@Override
					public boolean execute(String val, double wt) {
						try {
//							String weightString = returnWeights ? Double.toString(wt) : null;
							if (dstConst != null && val==dstConst) {
								wamInterp.restoreState(state);
								if (returnWeights) {
									wamInterp.setWt(arity,3,wt);
								}
								wamInterp.returnp();
								wamInterp.executeWithoutBranching();
							} else if (dstConst == null) {
								wamInterp.restoreState(state);
								wamInterp.setArg(arity,2,val);
								if (returnWeights) {							
									wamInterp.setWt(arity,3,wt);
								}
								wamInterp.returnp();
								wamInterp.executeWithoutBranching();
							} else { log.debug("dstConst "+dstConst+", val "+val); return true; }
							if (computeFeatures) {
								result.add(new Outlink(scaleFD(getFD(), wt), wamInterp.saveState()));
							} else {
							    State save = wamInterp.saveState();
							    if (log.isDebugEnabled()) log.debug("Result "+save);
								result.add(new Outlink(null, save));
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
}
