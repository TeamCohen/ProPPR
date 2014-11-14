package edu.cmu.ml.proppr.prove;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.junit.Test;

import edu.cmu.ml.proppr.prove.DfsProver;
import edu.cmu.ml.proppr.prove.Prover;
import edu.cmu.ml.proppr.prove.UniformWeighter;
import edu.cmu.ml.proppr.prove.wam.Argument;
import edu.cmu.ml.proppr.prove.wam.ConstantArgument;
import edu.cmu.ml.proppr.prove.wam.Goal;
import edu.cmu.ml.proppr.prove.wam.AWamProgram;
import edu.cmu.ml.proppr.prove.wam.LogicProgramException;
import edu.cmu.ml.proppr.prove.wam.ProofGraph;
import edu.cmu.ml.proppr.prove.wam.Query;
import edu.cmu.ml.proppr.prove.wam.State;
import edu.cmu.ml.proppr.prove.wam.VariableArgument;
import edu.cmu.ml.proppr.prove.wam.WamProgram;
import edu.cmu.ml.proppr.prove.wam.plugins.GraphlikePlugin;
import edu.cmu.ml.proppr.prove.wam.plugins.LightweightGraphPlugin;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.v1.QueryAnswerer;


public class UngroundedSolutionsTest {

	@Test
	public void test() throws IOException, LogicProgramException {
		AWamProgram program = WamProgram.load(new File("testcases/grand/grand.wam"));
		GraphlikePlugin facts = LightweightGraphPlugin.load(new File("testcases/grand/grand.cfacts"));
		Query q = new Query(new Goal("grandparent",new ConstantArgument("X"),new ConstantArgument("Y")));
//		q.variabilize();
		ProofGraph pg = new ProofGraph(q,program,facts);
		Prover p = new DfsProver(new UniformWeighter(), 20);
		
		Map<State,Double> ans = p.prove(pg);

//		Map<LogicProgramState,Double> ans = p.proveState(program, new ProPPRLogicProgramState(Goal.decompile("grandparent,-1,-2")));
		
		System.out.println("===");
		for (State s : ans.keySet()) {
			if (s.isCompleted()) {
				System.out.println(s);
				Map<Argument,String> dict = Prover.asDict(pg.getInterpreter().getConstantTable(), s);
				System.out.println(Dictionary.buildString(dict, new StringBuilder(), "\n\t").substring(1));
				for (String a : dict.values()) {
//					a = a.substring(a.indexOf(":"));
					assertFalse(a.startsWith("X"));
				}
			}
		}
		
//		System.out.println("===");
//		for (String s : Prover.filterSolutions(ans).keySet()) {
//			System.out.println(s);
//			assertFalse("Filtered solutions contain variables",s.contains("v["));
//		}
	}

}
