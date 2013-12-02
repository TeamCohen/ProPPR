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
		// dressedIn(X,Y) :- wearing(X,Y), owns(X,Y) # covering .
		// owns(X,Y) :- # closet .
		r.add(new Rule(
				new Goal("dressedIn","X","Y"),
				new Goal("covering"),
				new Goal("wearing","X","Y"),
				new Goal("has","X","Y")));
		r.add(new Rule(
				new Goal("has","X","Y"),
				new Goal("closet"),
				new Goal("owns","X","Y")));
		r.add(new Rule(
				new Goal("has","X","Y"),
				new Goal("closet"),
				new Goal("borrowed","X","Y")));
		r.compile();
		
		GoalComponent g = new GoalComponent();
		g.addFact(new Goal("wearing","steve","pants"));
		g.addFact(new Goal("owns","steve","pants"));
		g.addFact(new Goal("borrowed","steve","hat"));
		
		LogicProgram lp = new LogicProgram(new Component[] {r,g});
		
		ProPPRLogicProgramState state = new ProPPRLogicProgramState(Goal.decompile("dressedIn,steve,-1"));
		
		Prover p = new TracingDfsProver();
		Map<LogicProgramState,Double> result = p.proveState(lp, state);
		for (Map.Entry<LogicProgramState, Double> e : result.entrySet()) {
			System.out.println(e.getValue()+"\t"+e.getKey());
			System.out.println("\t"+e.getKey().description());
			if (e.getKey().isSolution()) {
				assertEquals("Steve not allowed to wear "+e.getKey().description()+".\n",
						"-1=c[pants]",e.getKey().description());
			}
		}
	}

}
