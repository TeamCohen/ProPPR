package edu.cmu.ml.proppr.examples;

import edu.cmu.ml.proppr.graph.LearningGraph;
import gnu.trove.map.TIntDoubleMap;

public class PprExample extends PosNegRWExample {

	// length = sum(nodes i) (degree of i) = #edges
	public double[][] M;
	// length = sum(edges e) (# features on e) = #feature assignments
	public int[] dM_feature_id;
	public double[] dM_value;
	// length = sum(nodes i) degree of i = #edges
	public int[][] dM_lo;
	public int[][] dM_hi;
	public PprExample(String name, LearningGraph graph, TIntDoubleMap queryVec,
			int[] pos, int[] neg) {
		super(name, graph, queryVec, pos, neg);
		this.allocate();
	}
	@Override
	protected void allocate() {
		super.allocate();
		this.M=new double[graph.node_hi][];
		this.dM_lo = new int[graph.node_hi][];
		this.dM_hi = new int[graph.node_hi][];
		for (int uid=0; uid<graph.node_hi; uid++) {
			int udeg = graph.node_near_hi[uid] - graph.node_near_lo[uid];
			this.M[uid] = new double[udeg];
			this.dM_lo[uid] = new int[udeg];
			this.dM_hi[uid] = new int[udeg];
		}
		this.dM_feature_id = new int[graph.labelDependencySize()];
		this.dM_value = new double[graph.labelDependencySize()];
	}

}
