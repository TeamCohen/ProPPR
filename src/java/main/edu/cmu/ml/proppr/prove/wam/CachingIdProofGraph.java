package edu.cmu.ml.proppr.prove.wam;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.util.Arrays;

import edu.cmu.ml.proppr.examples.GroundedExample;
import edu.cmu.ml.proppr.examples.InferenceExample;
import edu.cmu.ml.proppr.graph.InferenceGraph;
import edu.cmu.ml.proppr.graph.LearningGraphBuilder;
import edu.cmu.ml.proppr.prove.FeatureDictWeighter;
import edu.cmu.ml.proppr.prove.wam.plugins.WamPlugin;
import edu.cmu.ml.proppr.util.APROptions;
import edu.cmu.ml.proppr.util.ConcurrentSymbolTable;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.SimpleSymbolTable;
import edu.cmu.ml.proppr.util.SymbolTable;
import edu.cmu.ml.proppr.util.math.LongDense;
import edu.cmu.ml.proppr.util.math.SimpleSparse;
import edu.cmu.ml.proppr.util.math.SmoothFunction;
import gnu.trove.iterator.TIntDoubleIterator;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.list.array.TIntArrayList;

/* ************************** optimized version of the proofgraph  *********************** */
public class CachingIdProofGraph extends ProofGraph implements InferenceGraph {
	private LongDense.ObjVector<SimpleSparse.FloatMatrix> nodeVec;
	private SymbolTable<State> nodeTab;
	private SymbolTable<Feature> featureTab;
	private int edgeCount=0;

	public CachingIdProofGraph(Query query, APROptions apr, WamProgram program, WamPlugin ... plugins) throws LogicProgramException { 
		super(query, apr, program, plugins);
	}
	public CachingIdProofGraph(InferenceExample ex, APROptions apr, SymbolTable<Feature> featureTab, WamProgram program, WamPlugin ... plugins) throws LogicProgramException {
		super(ex,apr,featureTab,program,plugins);
	}
	public CachingIdProofGraph(ConcurrentSymbolTable.HashingStrategy<State> strat) {
		super();
		nodeVec = new LongDense.ObjVector<SimpleSparse.FloatMatrix>();
		this.featureTab = new ConcurrentSymbolTable<Feature>();
		nodeTab = new ConcurrentSymbolTable<State>(strat);
	}


	protected void init(SymbolTable<Feature> featureTab) {
		nodeVec = new LongDense.ObjVector<SimpleSparse.FloatMatrix>();
		this.featureTab = featureTab;
		nodeTab = new ConcurrentSymbolTable<State>(new ConcurrentSymbolTable.HashingStrategy<State>() {
			@Override
			public Object computeKey(State s) {
				return s.canonicalHash();
			}
			@Override
			public boolean equals(State s1, State s2) {
				if (s1.canonicalHash() != s2.canonicalHash()) return false;
				s1.setCanonicalForm(interpreter, startState);
				s2.setCanonicalForm(interpreter, startState);
				return s1.canonicalForm().equals(s2.canonicalForm());
			}});
		this.nodeTab.insert(this.getStartState());
	}
	@Override
	public State getState(int uid) { 
		return nodeTab.getSymbol(uid); 
	}
	public int getRootId() { 
		return 1; 
	}
	public int getId(State u) { 
		return nodeTab.getId(u); 
	}
	public int getDegreeById(int ui, FeatureDictWeighter weighter) throws LogicProgramException { 
		expandIfNeeded(ui, weighter);
		return nodeVec.get(ui).index.length; 
	}
	public int getIthNeighborById(int ui,int i, FeatureDictWeighter weighter)  throws LogicProgramException {
		expandIfNeeded(ui, weighter);
		return nodeVec.get(ui).index[i]; 
	}
	public double getIthWeightById(int ui,int i,LongDense.AbstractFloatVector params,FeatureDictWeighter weighter)  throws LogicProgramException {
		expandIfNeeded(ui, weighter);
		SimpleSparse.FloatVector phi = nodeVec.get(ui).val[i]; 
		return Math.max(0,weighter.getSquashingFunction().compute(phi.dot(params, (float) weighter.getSquashingFunction().defaultValue())));
	}
	public double getTotalWeightOfOutlinks(int ui,LongDense.AbstractFloatVector params,FeatureDictWeighter weighter) throws LogicProgramException {
		expandIfNeeded(ui, weighter);
		double z = 0.0;
		int d = getDegreeById(ui, weighter);
		for (int i=0; i<d; i++) {
			z += getIthWeightById(ui,i,params,weighter);
		}
		return z;
	}

	/** Convert a vector indexed by state id's to a map **/

	public Map<State,Double> asMap(LongDense.FloatVector vec) {
		Map<State,Double> result = new HashMap<State,Double>();
		for (int uid=getRootId(); uid<vec.size(); uid++) {
			double vu = vec.get(uid);
			State s = getState(uid);
			if (s != null && vu >= 0.0) {
				result.put(s, vu );
			}
		}
		return result;
	}

	/* produce and cache outlinks if you haven't yet */
	private void expandIfNeeded(int uid, FeatureDictWeighter weighter) throws LogicProgramException {
		if (nodeVec.get(uid)==null) {
			State u = nodeTab.getSymbol(uid);
			if (u!=null) {
				List<Outlink> outlinks = this.computeOutlinks(u,true);
				setOutlinks(uid,outlinks,weighter);
			}
		}
	}


	public void setOutlinks(int uid, List<Outlink> outlinks) { setOutlinks(uid,outlinks,null); }
	public void setOutlinks(int uid, List<Outlink> outlinks, FeatureDictWeighter weighter) {
		edgeCount += outlinks.size();
		nodeVec.set(uid, outlinksAsMatrix(outlinks, weighter));
	}

	public SimpleSparse.FloatMatrix outlinksAsMatrix(List<Outlink> outlinks, FeatureDictWeighter weighter) {
		// convert the outlinks to a sparse matrix
		SimpleSparse.FloatMatrix mat = new SimpleSparse.FloatMatrix(outlinks.size());
		int i = 0;
		for (Outlink o : outlinks) {
			int vi = this.nodeTab.getId(o.child);
			// convert features for link from u to vi to a SimpleSparse.Vector 
			int numFeats = o.fd.size();
			int[] featBuf = new int[numFeats];
			float[] featVal = new float[numFeats];
			int j=0;
			for (Map.Entry<Feature,Double> e : o.fd.entrySet()) {
				if (weighter != null) weighter.countFeature(e.getKey());
				featBuf[j] = featureTab.getId(e.getKey());
				featVal[j] = e.getValue().floatValue();
				j++;
			}
			mat.val[i] = new SimpleSparse.FloatVector(featBuf,featVal);
			mat.index[i] = vi;
			i++;
		}
		mat.sortIndex();
		return mat;
	}
	
	public LongDense.FloatVector paramsAsVector(Map<Feature, Double> weights,Double dflt) {
		return paramsAsVector(weights,dflt,featureTab);
	}
	
	public static LongDense.FloatVector paramsAsVector(Map<Feature, Double> weights,Double dflt, SymbolTable<Feature> featureTab) {
		int numFeats = featureTab.size();
		float[] featVal = new float[numFeats+1];
		for (int j=0;j<numFeats;j++) {
			featVal[j+1] = Dictionary.safeGet(weights,featureTab.getSymbol(j+1),dflt).floatValue();
		}
		return new LongDense.FloatVector(featVal,featVal.length-1,dflt.floatValue());
	}
	

	@Override
	protected InferenceGraph _getGraph() { 
		return this;
	}
	@Override
	public int nodeSize() {
		return nodeTab.size();
	}
	@Override
	public int edgeSize() {
		return edgeCount;
	}
	@Override
	public String serialize() {
		return serialize(false);
	}
	public String serialize(boolean featureIndex) {
		StringBuilder ret = new StringBuilder().append(this.nodeSize()) //numNodes
				.append("\t")
				.append(this.edgeCount)
				.append("\t"); // waiting for label dependency size
		int labelDependencies = 0;

		StringBuilder sb = new StringBuilder();
		boolean first = true;

		if (featureIndex) {
			sb.append("\t");
			for (int fi = 1; fi <= this.featureTab.size(); fi++) {
				if (!first) sb.append(LearningGraphBuilder.FEATURE_INDEX_DELIM);
				else first = false;
				Feature f = this.featureTab.getSymbol(fi);
				sb.append(f);
			}
		}

		// foreach src node
		for (int u=getRootId(); u<=this.nodeSize(); u++) {
			SimpleSparse.FloatMatrix nearu = this.nodeVec.get(u);
			if (nearu==null) continue;
			HashSet<Integer> outgoingFeatures = new HashSet<Integer>();
			//foreach dst from src
			for (int vi=0; vi<nearu.index.length; vi++) {
				int v = nearu.index[vi];
				sb.append("\t");
				sb.append(u).append(LearningGraphBuilder.SRC_DST_DELIM).append(v);
				sb.append(LearningGraphBuilder.EDGE_DELIM);
				SimpleSparse.FloatVector uvf = nearu.val[vi];
				//foreach feature on src,dst
				for (int fi=0; fi<uvf.index.length; fi++) {
					int f = uvf.index[fi];
					double w = uvf.val[fi];
					outgoingFeatures.add(fi);
					sb.append(f).append(LearningGraphBuilder.FEATURE_WEIGHT_DELIM)
					.append(w).append(LearningGraphBuilder.EDGE_FEATURE_DELIM);
				}
				// drop last ','
				sb.deleteCharAt(sb.length()-1);
			}
			labelDependencies += outgoingFeatures.size() * nearu.index.length;
		}
		
		ret.append(labelDependencies).append(sb);
		return ret.toString();

	}
	public String toString() {
		return this.serialize(true);
	}

	/** Prune a graph by removing 'invisible states', which would
	 * typically be states that have, on the call stack, some predicate
	 * P, where P is used in theorem-proving but ignored in learning.
	 * Edges between visible states are copied over.  It's assumed that
	 * leaves and roots are visible. Edges between a visible state and
	 * an invisible state are discarded - instead there will be edges,
	 * with the appropriate weights, leading into each visible state
	 * from its closest visible ancestor in the graph.
	 **/

	public CachingIdProofGraph prunedCopy(CachingIdProofGraph unpruned,
																				LongDense.AbstractFloatVector params,FeatureDictWeighter weighter,
																				VisibilityTest test) {
		try {
			CachingIdProofGraph pruned = new CachingIdProofGraph(example,apr,featureTab,program);
			// this is all the work of figuring out what to prune
			EdgeCollector collector = new EdgeCollector();
			unpruned.collectUnprunedEdges(collector,params,weighter,new HashSet(),test,unpruned.getRootId(),unpruned.getRootId(),1.0);
			// now convert collected edges to a CachingIdProofGraph.  You
			// need to recode the node id's into the right range for the new
			// graph.
			int[] virtualFeatureIndex = new int[] { pruned.featureTab.getId(new Feature("subproofSummary")) };
			// first cycle through the src nodes for 'real' edge
			for (Integer u : collector.realEdgeSources()) {
				int ui = u.intValue();
				int uj = pruned.getId(unpruned.getState(ui));
				int rd = collector.realEdgeDegree(ui);
				int vd = collector.virtualEdgeDegree(ui);
				int[] destIndex = new int[rd + vd];
				SimpleSparse.FloatVector[] destValue = new SimpleSparse.FloatVector[rd + vd];
				int k = 0;
				for (Integer v : collector.realEdgeDestinations(ui)) {
					int vi = v.intValue();
					int vj = pruned.getId(unpruned.getState(vi));
					destIndex[k] = vj;
					destValue[k] = collector.realEdgeFeatures(ui,vi);
					k++;
				}
				for (Integer v : collector.virtualEdgeDestinations(ui)) {
					int vi = v.intValue();					
					int vj = pruned.getId(unpruned.getState(vi));
					destIndex[k] = vj;
					double w = collector.virtualEdgeWeight(ui,vi);
					destValue[k] = new SimpleSparse.FloatVector(virtualFeatureIndex, new float[] { (float)w });
					k++;
				}
				SimpleSparse.FloatMatrix m = new SimpleSparse.FloatMatrix(destIndex,destValue);
				m.sortIndex();
				pruned.edgeCount += destIndex.length;
				pruned.nodeVec.set(uj, m);
			}
			// now cycle through the src nodes with 'virtal' edges that we missed the first time around
			for (Integer u : collector.virtualEdgeSources()) {
				int ui = u.intValue();
				if (!collector.hasRealEdge(ui)) {
					int uj = pruned.getId(unpruned.getState(ui));					
					int d = collector.virtualEdgeDegree(ui);
					int[] destIndex = new int[d];
					SimpleSparse.FloatVector[] destValue = new SimpleSparse.FloatVector[d];
					int k = 0;
					for (Integer v : collector.virtualEdgeDestinations(ui)) {
						int vi = v.intValue();					
						int vj = pruned.getId(unpruned.getState(vi));
						destIndex[k] = vj;
						double w = collector.virtualEdgeWeight(ui,vi);
						destValue[k] = new SimpleSparse.FloatVector(virtualFeatureIndex, new float[] { (float)w });
						k++;
					}
					SimpleSparse.FloatMatrix m = new SimpleSparse.FloatMatrix(destIndex,destValue);
					m.sortIndex();
					pruned.edgeCount += destIndex.length;
					pruned.nodeVec.set(uj, m);
				}
			}
			return pruned;
		} catch (LogicProgramException ex) {
			throw new IllegalStateException("I really shouldn't have seen a LogicProgramException here. How awkward...");
		}
	}

	/** Buffers up edges to add to a pruned version of this proofgraph.
	 * Real edges are between visible nodes; virtual edges are from a
	 * visible node through a path through invisible nodes to a visible
	 * descendant node. */

	private static class EdgeCollector {
		HashMap<Integer, HashMap<Integer, SimpleSparse.FloatVector> > space = new HashMap<Integer, HashMap<Integer, SimpleSparse.FloatVector> >();
		HashMap<Integer, HashMap<Integer, Double> > accum = new HashMap<Integer, HashMap<Integer, Double> >();
		static final HashSet<Integer> EMPTYSET = new HashSet<Integer>();
		public void collectRealEdge(int ui,int vi,SimpleSparse.FloatVector featureVec) {
			if (space.get(ui)==null) {
				space.put(ui, new HashMap<Integer,SimpleSparse.FloatVector>());
			}
			space.get(ui).put(vi,featureVec);
		}
		public void collectVirtualEdge(int ui,int vi,double delta) {
			if (accum.get(ui)==null) {
				accum.put(ui, new HashMap<Integer,Double>());
			}
			if (accum.get(ui).get(vi)==null) {
				accum.get(ui).put(vi, 0.0);
			}
			double old = accum.get(ui).get(vi).doubleValue();
			accum.get(ui).put(vi, old + delta);
		}
		public boolean hasRealEdge(int ui) {
			return space.get(ui)!=null;
		}

		public Set<Integer> realEdgeSources() {
			return space.keySet();
		}
		public Set<Integer> realEdgeDestinations(int ui) {
			return space.get(ui).keySet();
		}
		public int realEdgeDegree(int ui) {
			return space.get(ui).keySet().size();
		}
		public SimpleSparse.FloatVector realEdgeFeatures(int ui,int vi) {
			return space.get(ui).get(vi);
		}
		public boolean hasVirtualEdge(int ui) {
			return accum.get(ui)!=null;
		}
		public Set<Integer> virtualEdgeSources() {
			return accum.keySet();
		}
		public Set<Integer> virtualEdgeDestinations(int ui) {
			if (accum.get(ui)==null) {
				return EMPTYSET;
			} else {
				return accum.get(ui).keySet();
			}
		}
		public int virtualEdgeDegree(int ui) {
			if (accum.get(ui)==null) {
				return 0;
			} else {
				return accum.get(ui).keySet().size();
			}
		}
		public double virtualEdgeWeight(int ui,int vi) {
			return accum.get(ui).get(vi).doubleValue();
		}
	}

	/** Recursively traverse the graph and figure out what edges to
	 * keep **/

	private void collectUnprunedEdges(EdgeCollector collector,
																		LongDense.AbstractFloatVector params,FeatureDictWeighter weighter, 
																		HashSet previouslyProcessed,VisibilityTest test,
																		int visibleAncestor,int ui,double weightOfPathFromVisibleAncestor) 
		throws LogicProgramException
	{
		if (!previouslyProcessed.add(ui)) {
			int du = getDegreeById(ui,weighter);
			for (int i=0; i<du; i++) {
				int vi = getIthNeighborById(ui,i,weighter);
				double wuv = getIthWeightById(ui,i,params,weighter);
				boolean vIsVisible = test.visible(getState(vi));
				if (vIsVisible) {
					if (ui==visibleAncestor) {
						// add an edge from the ancestor to the visible node vi
						SimpleSparse.FloatMatrix uiOutlinkMat = nodeVec.get(ui);
						int vx = Arrays.binarySearch(uiOutlinkMat.index,vi);
						collector.collectRealEdge(ui,vi,uiOutlinkMat.val[vx]);
					} else if (ui!=visibleAncestor) {
						// add a virtual edge from the ancestor to the visible node vi
						collector.collectVirtualEdge(visibleAncestor,vi,weightOfPathFromVisibleAncestor*wuv);
					}
					// recurse through v
					collectUnprunedEdges(collector,params,weighter,previouslyProcessed,test,vi,vi,1.0);
				} else if (!vIsVisible) {
					// recurse through children of v
					int dv = getDegreeById(vi,weighter);
					for (int j=0; i<dv; j++) {
						int xi = getIthNeighborById(ui,i,weighter);
						double wvx = getIthWeightById(ui,i,params,weighter);
						collectUnprunedEdges(collector,params,weighter,previouslyProcessed,test,
																 visibleAncestor,xi,
																 weightOfPathFromVisibleAncestor*wuv*wvx*(1.0 - apr.alpha)*(1.0 - apr.alpha));
					}
				} else {
					throw new IllegalStateException("impossible!");
				}
			}
		}
	}

	public static interface VisibilityTest {
		public boolean visible(State state);		
	}
}
