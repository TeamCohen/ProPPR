package edu.cmu.ml.praprolog.prove.v1;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Test;

import edu.cmu.ml.praprolog.prove.v1.Component;
import edu.cmu.ml.praprolog.prove.v1.Goal;
import edu.cmu.ml.praprolog.prove.v1.GoalComponent;
import edu.cmu.ml.praprolog.prove.v1.LogicProgram;
import edu.cmu.ml.praprolog.prove.v1.LogicProgramState;
import edu.cmu.ml.praprolog.prove.v1.ProPPRLogicProgramState;
import edu.cmu.ml.praprolog.prove.v1.Prover;
import edu.cmu.ml.praprolog.prove.v1.Rule;
import edu.cmu.ml.praprolog.prove.v1.RuleComponent;
import edu.cmu.ml.praprolog.prove.v1.TracingDfsProver;

public class EqualityTest {

	@Test
	public void test() {
		/*
		 * eq(X,X) :- .
		 * moral (X) :- wife(X,Y),sweetheart(X,W),eq(Y,W).
		 * 
		 * wife(bob,sue).
		 * sweetheart(bob,sue).
		 * wife(rob,sam).
		 * sweetheart(rob,sandy).
		 */
		RuleComponent rc = new RuleComponent();
		rc.add(new Rule(Goal.decompile("eq,-1,-1"),new Goal[0],new Goal[] {new Goal("r1")}));
		rc.add(new Rule(
				Goal.decompile("moral,-1"),
				new Goal[] {Goal.decompile("wife,-1,-2"),Goal.decompile("sweetheart,-1,-3"),Goal.decompile("eq,-2,-3")},
				new Goal[] {new Goal("r2")}));
		GoalComponent gc = new GoalComponent();
		gc.addFact(Goal.decompile("wife,bob,sue"));
		gc.addFact(Goal.decompile("sweetheart,bob,sue"));
		gc.addFact(Goal.decompile("wife,rob,sam"));
		gc.addFact(Goal.decompile("sweetheart,rob,sandy"));
		
		LogicProgram lp = new LogicProgram(new Component[] {rc,gc});
		TracingDfsProver prover = new TracingDfsProver(10);
		Map<LogicProgramState,Double> bob = prover.proveState(lp, new ProPPRLogicProgramState(Goal.decompile("moral,-1")));
		for (LogicProgramState s : bob.keySet()) {
			if (s.isSolution()) System.out.println(s);
		}
		Map<String,Double> bobs = Prover.filterSolutions(bob);
		assertEquals(1,bobs.size());
		assertTrue("Answer should be bob",bobs.keySet().iterator().next().contains("bob"));
	}

}
