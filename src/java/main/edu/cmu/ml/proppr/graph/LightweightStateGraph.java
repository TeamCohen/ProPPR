package edu.cmu.ml.proppr.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.prove.wam.Feature;
import edu.cmu.ml.proppr.prove.wam.Goal;
import edu.cmu.ml.proppr.prove.wam.Outlink;
import edu.cmu.ml.proppr.prove.wam.State;
import edu.cmu.ml.proppr.util.SymbolTable;
import edu.cmu.ml.proppr.util.SimpleSymbolTable;
import gnu.trove.iterator.TIntDoubleIterator;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.procedure.TIntDoubleProcedure;
import gnu.trove.procedure.TIntObjectProcedure;
import gnu.trove.procedure.TIntProcedure;
import gnu.trove.strategy.HashingStrategy;

public class LightweightStateGraph implements InferenceGraph {
	private static final Logger log = Logger.getLogger(LightweightStateGraph.class);
	private static final Map<Feature,Double> DEFAULT_FD = Collections.emptyMap();
	private final List<State> DEFAULT_NEAR = Collections.emptyList();
	private SymbolTable<State> nodeTab;
	private SymbolTable<Feature> featureTab;
	private TIntObjectHashMap<TIntArrayList> near = new TIntObjectHashMap<TIntArrayList>();
	private TIntObjectHashMap<TIntObjectHashMap<TIntDoubleHashMap>> edgeFeatureDict = new TIntObjectHashMap<TIntObjectHashMap<TIntDoubleHashMap>>();
	private int edgeCount = 0;

	public LightweightStateGraph() {
		this(new SimpleSymbolTable<State>(), new SimpleSymbolTable<Feature>());
	}

	public LightweightStateGraph(HashingStrategy<State> nodeHash) {
		this(new SimpleSymbolTable<State>(nodeHash), new SimpleSymbolTable<Feature>());
	}
	
	public LightweightStateGraph(HashingStrategy<State> nodeHash, SymbolTable<Feature> ftab) {
		this(new SimpleSymbolTable<State>(nodeHash), ftab);
	}
	
	public LightweightStateGraph(SymbolTable<State> ntab, SymbolTable<Feature> ftab) {
		this.nodeTab = ntab;
		this.featureTab = ftab;
	}

	@Override
	public State getState(int u) {
		return nodeTab.getSymbol(u);
	}

	public State getRoot() {
		return nodeTab.getSymbol(1);
	}

	public int getId(State u) {
		return nodeTab.getId(u);
	}

	public List<State> near(State u) {
		int ui = this.nodeTab.getId(u);
		if (!near.containsKey(ui)) return DEFAULT_NEAR;
		TIntArrayList vs = near.get(ui);
		final ArrayList<State> ret = new ArrayList<State>(vs.size());
		vs.forEach(new TIntProcedure(){
			@Override
			public boolean execute(int vi) {
				ret.add(nodeTab.getSymbol(vi));
				return true;
			}});
		return ret;
	}

	public Map<Feature, Double> getFeatures(State u, State v) {
		int ui = this.nodeTab.getId(u), vi = this.nodeTab.getId(v);
		if (!edgeFeatureDict.containsKey(ui)) return DEFAULT_FD;
		TIntObjectHashMap<TIntDoubleHashMap> fu = edgeFeatureDict.get(ui);
		if (!fu.containsKey(vi)) return DEFAULT_FD;
		TIntDoubleHashMap fuvi = fu.get(vi); 
		final HashMap<Feature,Double> ret = new HashMap<Feature,Double>();
		fuvi.forEachEntry(new TIntDoubleProcedure(){
			@Override
			public boolean execute(int fi, double wt) {
				ret.put(featureTab.getSymbol(fi), wt);
				return true;
			}});
		return ret;
	}

	/** Return the neighbors of node u. */
	public List<Outlink> getOutlinks(State u) {
		// why do we need to recompute these each time?
		List<Outlink> result = new ArrayList<Outlink>();
		for (State v : near(u)) {
			Map<Feature,Double> fd = getFeatures(u,v);
			result.add(new Outlink(fd,v));
		}
		return result;
	}
	
	public void setOutlinks(int uid, List<Outlink> outlinks) {
		setOutlinks(this.nodeTab.getSymbol(uid), outlinks);
	}
	public void setOutlinks(State u, List<Outlink> outlinks) {
		// wwc: why are we saving these outlinks as a trove thing? space?
		int ui = this.nodeTab.getId(u);
		if (near.containsKey(ui)) {
			log.warn("Overwriting previous outlinks for state "+u);
			edgeCount -= near.get(ui).size();
		}
		TIntArrayList nearui = new TIntArrayList(outlinks.size());
		near.put(ui, nearui);
		TIntObjectHashMap<TIntDoubleHashMap> fui = new TIntObjectHashMap<TIntDoubleHashMap>();
		edgeFeatureDict.put(ui, fui);
		for (Outlink o : outlinks) {
			int vi = this.nodeTab.getId(o.child);
			nearui.add(vi);
			edgeCount++;
			TIntDoubleHashMap fvui = new TIntDoubleHashMap(o.fd.size());
			for (Map.Entry<Feature,Double> e : o.fd.entrySet()) {
				fvui.put(this.featureTab.getId(e.getKey()), e.getValue());
			}
			fui.put(vi, fvui);
		}
	}

	public boolean outlinksDefined(State u) {
		return near.containsKey(this.nodeTab.getId(u));
	}

	@Override
	public int nodeSize() {
		return this.nodeTab.size();
	}

	@Override
	public int edgeSize() {
		return this.edgeCount;
	}

	/**
	 * Serialization format: tab-delimited fields
	 * 1: node count
	 * 2: edge count
	 * 3: featurename1:featurename2:featurename3:...:featurenameN
	 * 4..N: srcId->dstId:fId_1,fId_2,...,fId_k
	 * 
	 * All IDs are 1-indexed.
	 * 
	 * @return
	 */
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
		for (TIntObjectIterator<TIntArrayList> it = this.near.iterator(); it.hasNext();) {
			it.advance();
			int ui=it.key();
			TIntArrayList nearu = it.value();
			HashSet<Integer> outgoingFeatures = new HashSet<Integer>();
			//foreach dst from src
			for (TIntIterator vit = nearu.iterator(); vit.hasNext();) {
				int vi = vit.next();
				sb.append("\t");
				sb.append(ui).append(LearningGraphBuilder.SRC_DST_DELIM).append(vi);
				sb.append(LearningGraphBuilder.EDGE_DELIM);
				//foreach feature on src,dst
				for (TIntDoubleIterator fit = edgeFeatureDict.get(ui).get(vi).iterator(); fit.hasNext();) {
					fit.advance();
					int fi = fit.key();
					double wi = fit.value();
					outgoingFeatures.add(fi);
					sb.append(fi).append(LearningGraphBuilder.FEATURE_WEIGHT_DELIM)
					.append(wi).append(LearningGraphBuilder.EDGE_FEATURE_DELIM);
				}
				// drop last ','
				sb.deleteCharAt(sb.length()-1);
			}
			labelDependencies += outgoingFeatures.size() * nearu.size();
		}
		
		ret.append(labelDependencies).append(sb);
		return ret.toString();
	}
	
	public String toString() {
		return this.serialize(true);
	}
}
