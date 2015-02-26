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

	private PosNegRWExample(LearningGraph graph, TIntDoubleMap queryVec) {
		super(graph,queryVec);
	}
	
	public PosNegRWExample(LearningGraph graph, TIntDoubleMap queryVec,
			int[] pos, int[] neg) {
		super(graph,queryVec);
		this.posList = pos;
		this.negList = neg;
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
		sb.append(graph.nodeSize()).append("/").append(graph.edgeSize()).append("; [");
		queryVec.forEachKey(new TIntProcedure() {
			@Override
			public boolean execute(int q) {
				sb.append("'").append(q).append("',");
				return true;
			}
		});
		if (sb.length() > 0) sb.deleteCharAt(sb.length()-1);
		sb.append("] -> +[");
		if (posList.length > 0) { sb.append("'"); Dictionary.buildString(posList, sb, "','"); sb.append("'"); };
		sb.append("]; -[");
		if (negList.length > 0) { sb.append("'"); Dictionary.buildString(negList, sb, "','"); sb.append("'"); };
		sb.append("]]");
		return sb.toString();
	}

	public int[] getPosList() {
		return posList;
	}

	public int[] getNegList() {
		return negList;
	}
}
