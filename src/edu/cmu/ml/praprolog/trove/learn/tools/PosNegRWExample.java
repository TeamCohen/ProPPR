package edu.cmu.ml.praprolog.trove.learn.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import edu.cmu.ml.praprolog.trove.graph.AnnotatedTroveGraph;
import edu.cmu.ml.praprolog.util.Dictionary;
import gnu.trove.map.hash.TIntDoubleHashMap;
/**
 * A supervised random walk example which specifies a list of positive examples and a list of negative examples.
 * @author krivard
 */
public class PosNegRWExample extends RWExample {
	protected int[] posList;// = new ArrayList();
	protected int[] negList;// = new ArrayList();

    // wwc add
    public PosNegRWExample posOnly() {
	PosNegRWExample result = new PosNegRWExample(this.graph,this.queryVec,new String[]{},new String[]{});
	result.posList = this.posList;
	//System.out.println("posOnly() for "+this+" is "+result);
	return result;
    }
    // wwc add
    public PosNegRWExample negOnly() {
	PosNegRWExample result = new PosNegRWExample(this.graph,this.queryVec,new String[]{},new String[]{});
	result.negList = this.negList;
	//System.out.println("negOnly() for "+this+" is "+result);
	return result;
    }

	public PosNegRWExample(AnnotatedTroveGraph g, Map<String, Double> queryVec,
			String[] pos, String[] neg) {
		super(g,queryVec);
		init(pos,neg);
	}
	
	private void init(String[] pos, String[] neg) {
		posList = new int[pos.length];
		negList = new int[neg.length];
		for (int i=0; i<pos.length; i++) posList[i] = this.graph.keyToId(pos[i]);
		for (int i=0; i<neg.length; i++) negList[i] = this.graph.keyToId(neg[i]);
	}

	public PosNegRWExample(AnnotatedTroveGraph g, TIntDoubleHashMap queryVec,
			String[] pos, String[] neg) {
		super(g,queryVec);
		init(pos,neg);
	}

	@Override
	public int length() {
		return posList.length + negList.length;
	}
	
	public String toString() {
		return String.format("PosNegRWExample[%d/%d; [%s] -> +[%s]; -[%s]]",graph.getNodes().size(),graph.getNumEdges(),
				Dictionary.buildString(queryVec, new StringBuilder(), "','").substring(2)+"'",
				posList.length > 0 ? (Dictionary.buildString(posList, new StringBuilder(), "','").substring(2)+"'") : "",
				negList.length > 0 ? (Dictionary.buildString(negList, new StringBuilder(), "','").substring(2)+"'") : "");
		
	}

	public int[] getPosList() {
		return posList;
	}

	public int[] getNegList() {
		return negList;
	}
}
