package edu.cmu.ml.praprolog.examples;

import java.util.List;
import java.util.Map;

import edu.cmu.ml.praprolog.graph.AnnotatedGraph;

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
	public static class HiLo<T> {
		T hi;
		T lo;
		public HiLo(T hi, T lo) {
			this.hi = hi;
			this.lo = lo;
		}
		public T getHi() { return hi; }
		public T getLo() { return lo; }
	}
	
	public String toString() {
		return super.toString()+"; "+this.length()+" hiLo pairs";
	}
}
