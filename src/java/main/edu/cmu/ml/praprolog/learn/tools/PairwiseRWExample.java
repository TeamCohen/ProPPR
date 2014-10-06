package edu.cmu.ml.praprolog.learn.tools;

import java.util.List;
import java.util.Map;

import edu.cmu.ml.praprolog.graph.AnnotatedGraph;
import edu.cmu.ml.praprolog.graph.Edge;
/**
 * A supervised random walk example which specifies pairs of edges where one ought to be ranked higher than the other.
 * @author krivard
 *
 */
public class PairwiseRWExample<T> extends RWExample<T> {
	protected List<HiLo<T>> hiLoList;
	public PairwiseRWExample(AnnotatedGraph<T> graph, Map<T, Double> queryVec, List<HiLo<T>> hilolist) {
		super(graph, queryVec);
		this.hiLoList = hilolist;
	}

	@Override
	public int length() {
		return hiLoList.size();
	}

	public List<HiLo<T>> getHiLoList() {
		return hiLoList;
	}
	
	/**
	 * A pair of edges of ordered rank, suitable for use as a key or in a set.
	 * @author krivard
	 */
	public static class HiLo<T> extends Edge<T> {
		public HiLo(T hi, T lo) {
			super(hi, lo);
		}
		public T getHi() { return uid; }
		public T getLo() { return vid; }
	}
	
	public String toString() {
		return super.toString()+"; "+this.length()+" hiLo pairs";
	}
}
