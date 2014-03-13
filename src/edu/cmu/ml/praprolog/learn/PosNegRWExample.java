package edu.cmu.ml.praprolog.learn;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import edu.cmu.ml.praprolog.graph.AnnotatedGraph;
import edu.cmu.ml.praprolog.util.Dictionary;
/**
 * A supervised random walk example which specifies a list of positive examples and a list of negative examples.
 * @author krivard
 */
public class PosNegRWExample<T> extends RWExample<T> {
	protected List<T> posList = new ArrayList<T>();
	protected List<T> negList = new ArrayList<T>();

	// wwc add, kmm port
	public PosNegRWExample<T> posOnly() {
		PosNegRWExample<T> result = new PosNegRWExample<T>(this.graph,this.queryVec);
		result.posList = this.posList;
		//System.out.println("posOnly() for "+this+" is "+result);
		return result;
	}
	// wwc add, kmm port
	public PosNegRWExample<T> negOnly() {
		PosNegRWExample<T> result = new PosNegRWExample<T>(this.graph,this.queryVec);
		result.negList = this.negList;
		//System.out.println("negOnly() for "+this+" is "+result);
		return result;
	}

	private PosNegRWExample(AnnotatedGraph<T> graph, Map<T, Double> queryVec) {
		super(graph,queryVec);
	}
	
	public PosNegRWExample(AnnotatedGraph<T> g, Map<T, Double> queryVec,
			T[] pos, T[] neg) {
		super(g,queryVec);
		for (T p : pos) {
			this.posList.add(p);
		}
		for (T n : neg) {
			this.negList.add(n);
		}
	}

	public PosNegRWExample(AnnotatedGraph<T> g, Map<T, Double> queryVec,
			Iterable<T> pos, Iterable<T> neg) {
		super(g,queryVec);
		for (T p : pos) this.posList.add(p);
		for (T n : neg) this.negList.add(n);
	}

	@Override
	public int length() {
		return posList.size() + negList.size();
	}

	public String toString() {
		return String.format("PosNegRWExample[%d/%d; [%s] -> +[%s]; -[%s]]",graph.getNodes().size(),graph.getNumEdges(),
				Dictionary.buildString(queryVec.keySet(), new StringBuilder(), "','").toString()+"'",
				posList.size() > 0 ? (Dictionary.buildString(posList, new StringBuilder(), "','").toString()+"'") : "",
						negList.size() > 0 ? (Dictionary.buildString(negList, new StringBuilder(), "','").toString()+"'") : "");
	}

	public List<T> getPosList() {
		return posList;
	}

	public List<T> getNegList() {
		return negList;
	}
}
