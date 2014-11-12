package edu.cmu.ml.proppr.v1;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.graph.v1.GraphWriter;
import edu.cmu.ml.proppr.prove.v1.Goal;
import edu.cmu.ml.proppr.prove.v1.LogicProgram;
import edu.cmu.ml.proppr.prove.v1.LogicProgramState;
import edu.cmu.ml.proppr.prove.v1.ProPPRLogicProgramState;
import edu.cmu.ml.proppr.prove.v1.Prover;
import edu.cmu.ml.proppr.prove.v1.RawPosNegExample;
import edu.cmu.ml.proppr.prove.v1.ThawedPosNegExample;
import edu.cmu.ml.proppr.prove.v1.TracingDfsProver;
import edu.cmu.ml.proppr.util.Dictionary;

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
		
		return new ThawedPosNegExample(new ProPPRLogicProgramState(rawX.getQuery()),posSet,negSet);
	}

	public LogicProgram getMasterProgram() {
		return this.masterProgram;
	}

}