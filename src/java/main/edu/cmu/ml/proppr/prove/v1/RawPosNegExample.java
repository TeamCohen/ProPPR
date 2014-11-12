package edu.cmu.ml.proppr.prove.v1;

import java.util.List;

/**
 * An example, in the form of a query goal and a list of positive
    and negative ground substitutions that answer that query
 * @author wcohen,krivard
 *
 */
public class RawPosNegExample {
	private Goal query;
	private String[] posList;
	private String[] negList;
	
	public RawPosNegExample(Goal query, List<String> p, List<String> n) {
		this.query = query;
		this.posList = p.toArray(new String[0]);
		this.negList = n.toArray(new String[0]);
	}

	public Goal getQuery() {
		return query;
	}

	public String[] getPosList() {
		return posList;
	}

	public String[] getNegList() {
		return negList;
	}

}
