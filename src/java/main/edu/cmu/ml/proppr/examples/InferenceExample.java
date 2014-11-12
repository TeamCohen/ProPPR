package edu.cmu.ml.proppr.examples;

import java.util.Arrays;
import java.util.Set;

import edu.cmu.ml.proppr.prove.wam.Goal;
import edu.cmu.ml.proppr.prove.wam.ProofGraph;
import edu.cmu.ml.proppr.prove.wam.Query;
import edu.cmu.ml.proppr.prove.wam.State;
import edu.cmu.ml.proppr.util.Dictionary;


public class InferenceExample {
	private Query[] negSet;
	private Query[] posSet;
	private Query query;

public InferenceExample(Query q, Query[] posSet, Query[] negSet) {
		this.query  = q;
		
		if (posSet != null) {
			this.posSet = posSet; Arrays.sort(this.posSet);
		} else this.posSet = new Query[0];
		if (negSet != null) {
			this.negSet = negSet; Arrays.sort(this.negSet);
		} else this.negSet = new Query[0];
	}

	public Query[] getNegSet() {
		return negSet;
	}

	public Query[] getPosSet() {
		return posSet;
	}

	public Query getQuery() {
		return this.query;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder(this.query.toString());
		Dictionary.buildString(negSet, new StringBuilder(), " -", false);
		Dictionary.buildString(posSet, new StringBuilder(), " +", false);
		return sb.toString();
	}
}
