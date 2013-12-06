package edu.cmu.ml.praprolog.prove;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Test;

/** 
 * When two goals in a rule have the same signature, ProPPR doesn't resolve their variables properly.
 * 
 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
 */
public class DuplicateSignatureRuleTest {

	@Test
	public void test() {
		RuleComponent r = new RuleComponent();
		/*
		 * canExit(Player,Room) :- location(Player,Room),hasKey(Player,Room) .
hasKey(Player,Room) :- class(Player,wizard) .
hasKey(Player,Room) :- doorPuzzle(Room,Puzzle),solved(Player,Puzzle) .
		 */
		r.add(new Rule(
				new Goal("canExit","X","Y"),
				new Goal("ability"),
				new Goal("location","X","Y"),
				new Goal("hasKey","X","Y")));
		r.add(new Rule(
				new Goal("hasKey","P","Q"),
				new Goal("unlock0"),
				new Goal("class","P","wizard")));
		r.add(new Rule(
				new Goal("hasKey","M","N"),
				new Goal("unlock1"),
				new Goal("doorPuzzle","N","K"),
				new Goal("solved","M","K")));
		r.compile();
		
		GoalComponent g = new GoalComponent();
		g.addFact(new Goal("doorPuzzle","kitchen","puzzle_kitchen"));
		g.addFact(new Goal("doorPuzzle","parlor","puzzle_parlor"));
		g.addFact(new Goal("class","steve","wizard"));
		g.addFact(new Goal("location","steve","kitchen"));
		g.addFact(new Goal("solved","steve","puzzle_parlor"));
		
		LogicProgram lp = new LogicProgram(new Component[] {r,g});
		
		ProPPRLogicProgramState state = new ProPPRLogicProgramState(Goal.decompile("canExit,steve,-1"));
		
		Prover p = new TracingDfsProver();
		Map<LogicProgramState,Double> result = p.proveState(lp, state);
		for (Map.Entry<LogicProgramState, Double> e : result.entrySet()) {
			System.out.println(e.getValue()+"\t"+e.getKey());
			System.out.println("\t"+e.getKey().description());
			if (e.getKey().isSolution()) {
				assertEquals("Steve not allowed to exit "+e.getKey().description()+".\n",
						"-1=c[kitchen]",e.getKey().description());
			}
		}
	}

}
