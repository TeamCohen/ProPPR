package edu.cmu.ml.proppr.graph;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.prove.wam.Goal;
import edu.cmu.ml.proppr.prove.wam.Outlink;
import edu.cmu.ml.proppr.prove.wam.State;
import edu.cmu.ml.proppr.util.ConcurrentSymbolTable;
import edu.cmu.ml.proppr.util.Sparse;

public class CompactInferenceGraph extends InferenceGraph {

	private static final Logger log = Logger.getLogger(CompactInferenceGraph.class);
	private static final Map<Goal,Double> DEFAULT_FD = Collections.emptyMap();
	private final List<State> DEFAULT_NEAR = Collections.emptyList();
	private ConcurrentSymbolTable<State> nodeTab;
	private ConcurrentSymbolTable<Goal> featureTab;
	private Map<Integer,Sparse.Matrix> graph;
	private int numEdges = 0;

	public CompactInferenceGraph(ConcurrentSymbolTable nodeTab,ConcurrentSymbolTable featureTab) {
		this.nodeTab = nodeTab!=null? nodeTab : new ConcurrentSymbolTable<State>();
		this.featureTab = featureTab!=null? featureTab : new ConcurrentSymbolTable<Goal>();
		this.graph = new HashMap<Integer,Sparse.Matrix>();
	}

	@Override
	public State getState(int u) {
		return nodeTab.getSymbol(u);
	}

	@Override
	public State getRoot() {
		return nodeTab.getSymbol(1);
	}

	@Override
	public int getId(State u) {
		return nodeTab.getId(u);
	}

	@Override
	public void setOutlinks(State u, List<Outlink> outlinks) {
		// todo: check not set
		int ui = this.nodeTab.getId(u);
		// convert the outlinks to a sparse matrix
		Sparse.Matrix mat = new Sparse.Matrix(outlinks.size());
		int i = 0;
		for (Outlink o : outlinks) {
			int vi = this.nodeTab.getId(o.child);
			// convert features for link from u to vi to a Sparse.Vector 
			int numFeats = o.fd.size();
			int[] featBuf = new int[numFeats];
			float[] featVal = new float[numFeats];
			int j=0;
			for (Map.Entry<Goal,Double> e : o.fd.entrySet()) {
				featBuf[j] = featureTab.getId(e.getKey());
				featVal[j] = (float)e.getValue().doubleValue();
				j++;
			}
			mat.val[i] = new Sparse.Vector(featBuf,featVal);
			mat.index[i] = vi;
			i++;
		}
		mat.sortIndex();
		numEdges += mat.index.length;
		graph.put(ui,mat);
	}

	@Override
	public List<State> near(State u) {
		int ui = nodeTab.getId(u);		
		Sparse.Matrix umat = graph.get(ui);
		ArrayList result = new ArrayList(umat.index.length);
		for (int i=0; i<umat.index.length; i++) {
			State v = nodeTab.getSymbol(umat.index[i]);
			result.add(v);
		}
		return result;
	}

	@Override
	public Map<Goal, Double> getFeatures(State u, State v) {
		int ui = nodeTab.getId(u);		
		Sparse.Matrix umat = graph.get(ui);
		int vi = nodeTab.getId(v);
		int k = Arrays.binarySearch(umat.index,vi);
		// todo: check k>=0
		Map<Goal,Double> result = new HashMap<Goal,Double>();
		Sparse.Vector fv = umat.val[k];
		for (int i=0; i<fv.index.length; i++) {
			Goal fi = featureTab.getSymbol(fv.index[i]);
			result.put(fi,new Double(fv.val[i]));
		}
		return result;
	}

	@Override
	public boolean outlinksDefined(State u) {
		return graph.get(nodeTab.getId(u))!=null;
	}

	@Override
	public int nodeSize() {
		return nodeTab.maxId();
	}

	@Override
	public int edgeSize() {
		return numEdges;
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
		return null;
	}
}

