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
import edu.cmu.ml.proppr.prove.wam.StateProofGraph;
import edu.cmu.ml.proppr.prove.wam.WamProgram;
import edu.cmu.ml.proppr.prove.wam.LogicProgramException;
import edu.cmu.ml.proppr.prove.wam.ProofGraph;
import edu.cmu.ml.proppr.prove.wam.Query;
import edu.cmu.ml.proppr.prove.wam.State;
import edu.cmu.ml.proppr.prove.wam.WamBaseProgram;
import edu.cmu.ml.proppr.prove.wam.plugins.LightweightGraphPlugin;
import edu.cmu.ml.proppr.prove.wam.plugins.WamPlugin;
import edu.cmu.ml.proppr.util.APROptions;
import edu.cmu.ml.proppr.util.Dictionary;


public class UngroundedSolutionsTest {

	private static final String FACTS = "src/testcases/ungroundedSolutionsTest/grand.cfacts";
	private static final String PROGRAM = "src/testcases/ungroundedSolutionsTest/grand.wam";

	@Test
	public void test() throws IOException, LogicProgramException {
		WamProgram program = WamBaseProgram.load(new File(PROGRAM));
		APROptions apr = new APROptions("depth=20");
		WamPlugin facts = LightweightGraphPlugin.load(apr,new File(FACTS));
		Query q = new Query(new Goal("grandparent",new ConstantArgument("X"),new ConstantArgument("Y")));
//		q.variabilize();
		StateProofGraph pg = new StateProofGraph(q,apr,program,facts);
		Prover p = new DfsProver(apr);
		
		Map<State,Double> ans = p.prove(pg);

//		Map<LogicProgramState,Double> ans = p.proveState(program, new ProPPRLogicProgramState(Goal.decompile("grandparent,-1,-2")));
		
		System.out.println("===");
		for (State s : ans.keySet()) {
			if (s.isCompleted()) {
				System.out.println(s);
				Map<Argument,String> dict = pg.asDict(s);
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
