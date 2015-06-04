package edu.cmu.ml.proppr.prove.wam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cmu.ml.proppr.examples.GroundedExample;
import edu.cmu.ml.proppr.examples.InferenceExample;
import edu.cmu.ml.proppr.graph.InferenceGraph;
import edu.cmu.ml.proppr.prove.wam.plugins.WamPlugin;
import edu.cmu.ml.proppr.util.APROptions;
import edu.cmu.ml.proppr.util.ConcurrentSymbolTable;
import edu.cmu.ml.proppr.util.LongDense;
import edu.cmu.ml.proppr.util.SimpleSparse;
import edu.cmu.ml.proppr.util.SmoothFunction;

/* ************************** optimized version of the proofgraph  *********************** */
public class CachingIdProofGraph extends ProofGraph implements InferenceGraph {
	private LongDense.ObjVector<SimpleSparse.FloatMatrix> nodeVec;
	private ConcurrentSymbolTable<State> nodeTab;
	private ConcurrentSymbolTable<Goal> featureTab;

	public CachingIdProofGraph(Query query, APROptions apr, WamProgram program, WamPlugin ... plugins) throws LogicProgramException { 
		super(query, apr, program, plugins);
	}
	public CachingIdProofGraph(InferenceExample ex, APROptions apr, WamProgram program, WamPlugin ... plugins) throws LogicProgramException {
		super(ex, apr, program, plugins);
		nodeVec = new LongDense.ObjVector<SimpleSparse.FloatMatrix>();
		featureTab = new ConcurrentSymbolTable<Goal>();
		nodeTab = new ConcurrentSymbolTable<State>(new ConcurrentSymbolTable.HashingStrategy<State>() {
			@Override
			public int computeHashCode(State s) {
				return s.canonicalHash();
			}
			@Override
			public boolean equals(State s1, State s2) {
				return s1.canonicalHash() == s2.canonicalHash();
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
	public int getDegreeById(int ui) throws LogicProgramException { 
		expandIfNeeded(ui);
		return nodeVec.get(ui).index.length; 
	}
	public int getIthNeighborById(int ui,int i)  throws LogicProgramException {
		expandIfNeeded(ui);
		return nodeVec.get(ui).index[i]; 
	}
	public double getIthWeightById(int ui,int i,LongDense.AbstractFloatVector params,SmoothFunction squashingFunction)  throws LogicProgramException {
		expandIfNeeded(ui);
		SimpleSparse.FloatVector phi = nodeVec.get(ui).val[i]; 
		return squashingFunction.compute(phi.dot(params));
	}
	public double getTotalWeightOfOutlinks(int ui,LongDense.AbstractFloatVector params,SmoothFunction squashingFunction) throws LogicProgramException {
		expandIfNeeded(ui);
		double z = 0.0;
		int d = getDegreeById(ui);
		for (int i=0; i<d; i++) {
			z += getIthWeightById(ui,i,params,squashingFunction);
		}
		return z;
	}

	/** Convert a vector indexed by state id's to a map **/

	public Map<State,Double> asMap(LongDense.FloatVector vec) {
		Map<State,Double> result = new HashMap<State,Double>();
		for (int uid=getRootId(); uid<vec.size(); uid++) {
			double vu = vec.get(uid);
			if (vu >= 0.0) {
				result.put( getState(uid), vu );
			}
		}
		return result;
	}

	/* produce and cache outlinks if you haven't yet */
	private void expandIfNeeded(int uid) throws LogicProgramException {
		if (nodeVec.get(uid)==null) {
			State u = nodeTab.getSymbol(uid);
			if (u!=null) {
				nodeVec.set(uid, outlinksAsMatrix(this.computeOutlinks(u,true)));
			}
		}
	}

	public SimpleSparse.FloatMatrix outlinksAsMatrix(List<Outlink> outlinks) {
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
			for (Map.Entry<Goal,Double> e : o.fd.entrySet()) {
				featBuf[j] = featureTab.getId(e.getKey());
				featVal[j] = (float)e.getValue().doubleValue();
				j++;
			}
			mat.val[i] = new SimpleSparse.FloatVector(featBuf,featVal);
			mat.index[i] = vi;
			i++;
		}
		mat.sortIndex();
		return mat;
	}
	
	@Override
	public GroundedExample makeRWExample(Map<State, Double> ans) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public int nodeSize() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public int edgeSize() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public String serialize() {
		// TODO Auto-generated method stub
		return null;
	}
}
