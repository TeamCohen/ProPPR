package edu.cmu.ml.proppr.prove.v1;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import org.junit.Test;

import edu.cmu.ml.proppr.prove.DprProver;
import edu.cmu.ml.proppr.prove.Prover;
import edu.cmu.ml.proppr.prove.wam.AWamProgram;
import edu.cmu.ml.proppr.prove.wam.LogicProgramException;
import edu.cmu.ml.proppr.prove.wam.ProofGraph;
import edu.cmu.ml.proppr.prove.wam.Query;
import edu.cmu.ml.proppr.prove.wam.State;
import edu.cmu.ml.proppr.prove.wam.WamProgram;

public class EqualityTest {
	private static final String EQUALITY_PROGRAM="src/testcases/equalityTest.wam";

	@Test
	public void test() throws LogicProgramException, IOException {
		
		/*
		 * eq(X,X) :- .
		 * moral (X) :- wife(X,Y),sweetheart(X,W),eq(Y,W).
		 * 
		 * wife(bob,sue).
		 * sweetheart(bob,sue).
		 * wife(rob,sam).
		 * sweetheart(rob,sandy).
		 */
//		RuleComponent rc = new RuleComponent();
//		rc.add(new Rule(Goal.decompile("eq,-1,-1"),new Goal[0],new Goal[] {new Goal("r1")}));
//		rc.add(new Rule(
//				Goal.decompile("moral,-1"),
//				new Goal[] {Goal.decompile("wife,-1,-2"),Goal.decompile("sweetheart,-1,-3"),Goal.decompile("eq,-2,-3")},
//				new Goal[] {new Goal("r2")}));
//		GoalComponent gc = new GoalComponent();
//		gc.addFact(Goal.decompile("wife,bob,sue"));
//		gc.addFact(Goal.decompile("sweetheart,bob,sue"));
//		gc.addFact(Goal.decompile("wife,rob,sam"));
//		gc.addFact(Goal.decompile("sweetheart,rob,sandy"));
//		
//		LogicProgram lp = new LogicProgram(new Component[] {rc,gc});
//		TracingDfsProver prover = new TracingDfsProver(10);
//		Map<LogicProgramState,Double> bob = prover.proveState(lp, new ProPPRLogicProgramState(Goal.decompile("moral,-1")));
//		for (LogicProgramState s : bob.keySet()) {
//			if (s.isSolution()) System.out.println(s);
//		}
//		Map<String,Double> bobs = Prover.filterSolutions(bob);
		
		AWamProgram program = WamProgram.load(new File(EQUALITY_PROGRAM));
		Prover prover = new DprProver();
		ProofGraph moral = new ProofGraph(Query.parse("moral(X)"),program);
		Map<State,Double> ans = prover.prove(moral);
		ArrayList<Query> bobs = new ArrayList<Query>();
		for (Map.Entry<State,Double> e : ans.entrySet()) {
			if (e.getKey().isCompleted()) bobs.add(moral.fill(e.getKey()));
		}
		
		
		assertEquals(1,bobs.size());
		assertEquals("Answer should be bob","bob",bobs.get(0).getRhs()[0].getArg(0).getName());
	}

}
