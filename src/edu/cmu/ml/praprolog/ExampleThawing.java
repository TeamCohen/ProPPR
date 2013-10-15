package edu.cmu.ml.praprolog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.cmu.ml.praprolog.graph.GraphWriter;
import edu.cmu.ml.praprolog.learn.PosNegRWExample;
import edu.cmu.ml.praprolog.prove.Goal;
import edu.cmu.ml.praprolog.prove.LogicProgram;
import edu.cmu.ml.praprolog.prove.LogicProgramState;
import edu.cmu.ml.praprolog.prove.Prover;
import edu.cmu.ml.praprolog.prove.RawPosNegExample;
import edu.cmu.ml.praprolog.prove.ThawedPosNegExample;
import edu.cmu.ml.praprolog.prove.TracingDfsProver;
import edu.cmu.ml.praprolog.util.Dictionary;

public class ExampleThawing {
	private static final Logger log = Logger.getLogger(ExampleThawing.class);
	protected Prover prover;
	protected LogicProgram masterProgram;

	public void init(Prover p, LogicProgram lp) {
		this.prover = p;
		this.masterProgram = lp;
	}

	/**
	 * Convert a raw example, encoded as strings, as a 
	    query goal and labeled ground goals,
	    with respect to a logic program (for symbol table purposes)
	 * @param rawX
	 * @return
	 */
	public ThawedPosNegExample thawExample(RawPosNegExample rawX, LogicProgram p) {
		rawX.getQuery().compile(p.getSymbolTable());
		
		String[] examples = rawX.getPosList();
		Goal[] posSet = new Goal[examples.length];
		for (int i=0; i<examples.length; i++) {
			posSet[i]=Goal.parseGoal(examples[i]);
			posSet[i].compile(p.getSymbolTable());
		}
		Arrays.sort(posSet);
		
		examples = rawX.getNegList();
		Goal[] negSet = new Goal[examples.length];
		for (int i=0; i<examples.length; i++) { 
			negSet[i]=Goal.parseGoal(examples[i]);
			negSet[i].compile(p.getSymbolTable());
		}
		Arrays.sort(negSet);
		
		return new ThawedPosNegExample(new LogicProgramState(rawX.getQuery()),posSet,negSet);
	}

	public LogicProgram getMasterProgram() {
		return this.masterProgram;
	}

}