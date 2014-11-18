package edu.cmu.ml.proppr.examples;

import java.util.List;
import java.util.Map;

import edu.cmu.ml.proppr.graph.LearningGraph;
import gnu.trove.map.TIntDoubleMap;

/**
 * A supervised random walk example which specifies pairs of edges where one ought to be ranked higher than the other.
 * @author krivard
 *
 */
public class PairwiseRWExample extends RWExample {
	protected List<HiLo> hiLoList;
	public PairwiseRWExample(LearningGraph graph, TIntDoubleMap queryVec, List<HiLo> hilolist) {
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
	public static class HiLo {
		int hi;
		int lo;
		public HiLo(int hi, int lo) {
			this.hi = hi;
			this.lo = lo;
		}
		public int getHi() { return hi; }
		public int getLo() { return lo; }
	}
	
	public String toString() {
		return super.toString()+"; "+this.length()+" hiLo pairs";
	}
}
