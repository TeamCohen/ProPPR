package edu.cmu.ml.praprolog.trove.learn.tools;

import java.util.List;
import java.util.Map;

import edu.cmu.ml.praprolog.trove.graph.AnnotatedTroveGraph;
import edu.cmu.ml.praprolog.trove.graph.Edge;
/**
 * A supervised random walk example which specifies pairs of edges where one ought to be ranked higher than the other.
 * @author krivard
 *
 */
public class PairwiseRWExample extends RWExample {
	protected List<HiLo> hiLoList;
	public PairwiseRWExample(AnnotatedTroveGraph graph, Map<String, Double> queryVec, List<HiLo> hilolist) {
		super(graph, queryVec);
		this.hiLoList = hilolist;
	}

	@Override
	public int length() {
		return hiLoList.size();
	}

	public List<HiLo> getHiLoList() {
		return hiLoList;
	}
	
	/**
	 * A pair of edges of ordered rank, suitable for use as a key or in a set.
	 * @author krivard
	 */
	public static class HiLo extends Edge {
		public HiLo(int hi, int lo) {
			super(hi, lo);
		}
		public int getHi() { return uid; }
		public int getLo() { return vid; }
	}
	
	public String toString() {
		return super.toString()+"; "+this.length()+" hiLo pairs";
	}
}
