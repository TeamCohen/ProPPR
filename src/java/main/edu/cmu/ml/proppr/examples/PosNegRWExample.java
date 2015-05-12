package edu.cmu.ml.proppr.examples;


import edu.cmu.ml.proppr.graph.LearningGraph;
import edu.cmu.ml.proppr.util.Dictionary;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.procedure.TIntProcedure;
/**
 * A supervised random walk example which specifies a list of positive examples and a list of negative examples.
 * @author krivard
 */
public class PosNegRWExample extends RWExample {
	protected int[] posList;
	protected int[] negList;
	public double[] p;
	public TIntDoubleMap[] dp;

//	// wwc add, kmm port
//	public PosNegRWExample<F> posOnly() {
//		PosNegRWExample<F> result = new PosNegRWExample<T>(this.graph,this.queryVec);
//		result.posList = this.posList;
//		//System.out.println("posOnly() for "+this+" is "+result);
//		return result;
//	}
//	// wwc add, kmm port
//	public PosNegRWExample<F> negOnly() {
//		PosNegRWExample<F> result = new PosNegRWExample<T>(this.graph,this.queryVec);
//		result.negList = this.negList;
//		//System.out.println("negOnly() for "+this+" is "+result);
//		return result;
//	}

	private PosNegRWExample(String name, LearningGraph graph, TIntDoubleMap queryVec) {
		super(name, graph,queryVec);
		this.allocate();
	}
	
	public PosNegRWExample(LearningGraph graph, TIntDoubleMap queryVec, int[] pos, int[] neg) {
		this("n/a", graph, queryVec, pos, neg);
	}
	public PosNegRWExample(String name, LearningGraph graph, TIntDoubleMap queryVec,
			int[] pos, int[] neg) {
		super(name, graph,queryVec);
		this.posList = pos;
		this.negList = neg;
		this.allocate();
	}
	protected void allocate() {
		this.p = new double[graph.node_hi];
		this.dp = new TIntDoubleMap[graph.node_hi];
	}

//	public PosNegRWExample(InferenceLearningGraph<T> g, Map<T, Double> queryVec,
//			Iterable<T> pos, Iterable<T> neg) {
//		super(g,queryVec);
//		for (T p : pos) this.posList.add(p);
//		for (T n : neg) this.negList.add(n);
//	}

	@Override
	public int length() {
		return posList.length + negList.length;
	}

	public String toString() {
		final StringBuilder sb = new StringBuilder("PosNegRWExample[");
		sb.append(this.name).append(" ");
		sb.append(graph.nodeSize()).append("/").append(graph.edgeSize()).append("; [");
		queryVec.forEachKey(new TIntProcedure() {
			@Override
			public boolean execute(int q) {
				sb.append(q).append(",");
				return true;
			}
		});
		if (sb.length() > 0) sb.deleteCharAt(sb.length()-1);
		sb.append("] -> +[");
		if (posList.length > 0) { Dictionary.buildString(posList, sb, ",",true); };
		sb.append("]; -[");
		if (negList.length > 0) { Dictionary.buildString(negList, sb, ",",true); };
		sb.append("]]");
		return sb.toString();
	}

	public int[] getPosList() {
		return posList;
	}

	public int[] getNegList() {
		return negList;
	}
	
	public String serialize() {
		StringBuilder serialized = new StringBuilder("?")
		.append("\t");
		Dictionary.buildString(this.queryVec.keys(), serialized, ","); //query
		serialized.append("\t");
		Dictionary.buildString(this.posList,serialized,","); // pos
		serialized.append("\t");
		Dictionary.buildString(this.negList,serialized,","); //neg
		serialized.append("\t");
		this.graph.serialize(serialized);
		return serialized.toString();
	}
}
