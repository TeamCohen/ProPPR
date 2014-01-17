import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Map;

import org.junit.Test;

import edu.cmu.ml.praprolog.QueryAnswerer;
import edu.cmu.ml.praprolog.prove.Argument;
import edu.cmu.ml.praprolog.prove.Component;
import edu.cmu.ml.praprolog.prove.Goal;
import edu.cmu.ml.praprolog.prove.InnerProductWeighter;
import edu.cmu.ml.praprolog.prove.LogicProgram;
import edu.cmu.ml.praprolog.prove.LogicProgramState;
import edu.cmu.ml.praprolog.prove.ProPPRLogicProgramState;
import edu.cmu.ml.praprolog.prove.Prover;
import edu.cmu.ml.praprolog.prove.TracingDfsProver;
import edu.cmu.ml.praprolog.util.Dictionary;


public class TestUngroundedSolutions {

	@Test
	public void test() throws IOException {
		LogicProgram program = new LogicProgram(
				Component.loadComponents("git/grand/grand.crules:git/grand/grand.cfacts".split(":"),Component.ALPHA_DEFAULT));
		Prover p = new TracingDfsProver(10);

		Map<LogicProgramState,Double> ans = p.proveState(program, new ProPPRLogicProgramState(Goal.decompile("grandparent,-1,-2")));
		
		System.out.println("===");
		for (LogicProgramState s : ans.keySet()) {
			if (s.isSolution()) {
				System.out.println(s);
				for (Argument a : s.getGroundGoal().getArgs()) assertTrue(a.isConstant());
				assertFalse("Description includes variables",s.description().contains("v["));
			}
		}
		
//		System.out.println("===");
//		for (String s : Prover.filterSolutions(ans).keySet()) {
//			System.out.println(s);
//			assertFalse("Filtered solutions contain variables",s.contains("v["));
//		}
	}

}
