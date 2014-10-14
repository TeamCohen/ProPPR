package edu.cmu.ml.praprolog.prove.v1;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Map;

import org.junit.Test;

import edu.cmu.ml.praprolog.prove.v1.Argument;
import edu.cmu.ml.praprolog.prove.v1.Component;
import edu.cmu.ml.praprolog.prove.v1.DprProver;
import edu.cmu.ml.praprolog.prove.v1.Goal;
import edu.cmu.ml.praprolog.prove.v1.LogicProgram;
import edu.cmu.ml.praprolog.prove.v1.LogicProgramState;
import edu.cmu.ml.praprolog.prove.v1.ProPPRLogicProgramState;
import edu.cmu.ml.praprolog.prove.v1.Prover;
import edu.cmu.ml.praprolog.prove.v1.TracingDfsProver;

public class BacktraceTest {

	@Test(expected = IllegalStateException.class)
	public void testDpr() throws IOException {
		try {
			LogicProgram program = new LogicProgram(
					Component.loadComponents("testcases/grand/grand.crules:testcases/grand/grand.cfacts".split(":"),Component.ALPHA_DEFAULT,null));
			Prover p = new DprProver();

			Map<LogicProgramState,Double> ans = p.proveState(program, new ProPPRLogicProgramState(Goal.decompile("grandmother,-1,-2")));

			System.out.println("===");
			for (LogicProgramState s : ans.keySet()) {
				if (s.isSolution()) {
					System.out.println(s);
					for (Argument a : s.getGroundGoal().getArgs()) assertTrue(a.isConstant());
					assertFalse("Description includes variables",s.description().contains("v["));
				}
			}
		} catch (IllegalStateException e) { e.printStackTrace(); throw e; }
	}

	@Test(expected = IllegalStateException.class)
	public void testTracingDfs() throws IOException {
		try {
			LogicProgram program = new LogicProgram(
					Component.loadComponents("testcases/grand/grand.crules:testcases/grand/grand.cfacts".split(":"),Component.ALPHA_DEFAULT,null));
			Prover p = new TracingDfsProver(7);

			Map<LogicProgramState,Double> ans = p.proveState(program, new ProPPRLogicProgramState(Goal.decompile("grandmother,-1,-2")));

			System.out.println("===");
			for (LogicProgramState s : ans.keySet()) {
				if (s.isSolution()) {
					System.out.println(s);
					for (Argument a : s.getGroundGoal().getArgs()) assertTrue(a.isConstant());
					assertFalse("Description includes variables",s.description().contains("v["));
				}
			}
		} catch (IllegalStateException e) { e.printStackTrace(); throw e; }
	}

}
