package edu.cmu.ml.praprolog.prove.v1;

import java.util.Arrays;
import java.util.Set;


public class ThawedPosNegExample {
	private Goal[] negSet;
	private Goal[] posSet;
	private LogicProgramState queryState;

	public ThawedPosNegExample(LogicProgramState q, Goal[] posSet, Goal[] negSet) {
		this.queryState = q;
		this.posSet = posSet; Arrays.sort(this.posSet);
		this.negSet = negSet; Arrays.sort(this.negSet);
	}

	public Goal[] getNegSet() {
		return negSet;
	}

	public Goal[] getPosSet() {
		return posSet;
	}

	public LogicProgramState getQueryState() {
		return queryState;
	}
}
