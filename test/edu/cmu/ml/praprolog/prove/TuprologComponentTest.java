package edu.cmu.ml.praprolog.prove;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import edu.cmu.ml.praprolog.prove.Component.Outlink;
import edu.cmu.ml.praprolog.util.Dictionary;

public class TuprologComponentTest {

	@Test
	public void test() {
		TuprologComponent tc = new TuprologComponent();
		tc.addTheory("testcases/prolog/family.pl");
		
		RuleComponent rc = RuleComponent.loadCompiled("testcases/family.crules");
		GoalComponent gc = GoalComponent.loadCompiled("testcases/family.cfacts");
		
		LogicProgramState state0 = new ProPPRLogicProgramState(Goal.decompile("sim,william,-1"));
		System.out.println("START STATE: "+state0);
		
		List<Outlink> rcOut, gcOut, tcOut;
		rcOut = rc.outlinks(state0);
		for (Outlink o: rcOut) {
			System.out.println("rule     "+o.getState());
			System.out.println("\t"+Dictionary.buildString(o.getFeatureDict(), new StringBuilder(), " ").toString());
		}
		
		tcOut = tc.outlinks(state0);
		for (Outlink o : tcOut) {
			System.out.println("tuprolog "+o.getState());
			System.out.println("\t"+Dictionary.buildString(o.getFeatureDict(), new StringBuilder(), " ").toString());
		}
		
		LogicProgramState state0r1 = rcOut.get(1).getState();
		LogicProgramState state0t1 = tcOut.get(1).getState();
		System.out.println("==");
		System.out.println("NEXT STATE: "+state0r1);
		System.out.println("NEXT STATE: "+state0t1);
		System.out.println("==");
		
		gcOut = gc.outlinks(state0r1);
		for (Outlink o : gcOut) {
			System.out.println("goal     "+o.getState());
			System.out.println("\t"+Dictionary.buildString(o.getFeatureDict(), new StringBuilder(), " ").toString());
		}
		
		tcOut = tc.outlinks(state0t1);
		for (Outlink o : tcOut) {
			System.out.println("tuprolog "+o.getState());
			System.out.println("\t"+Dictionary.buildString(o.getFeatureDict(), new StringBuilder(), " ").toString());
		}
		
//		LogicProgramState state1 = gcOut.get(0).getState();
//		rcOut = rc.outlinks(state1);
	}
	
	@Test
	public void inProgram() {
		LogicProgram lp = new LogicProgram(
				GoalComponent.loadCompiled("testcases/family-more.cfacts"), new TuprologComponent("testcases/prolog/family.pl"));
		Prover p = new DprProver();
		Map<LogicProgramState,Double> result = p.proveState(lp, new ProPPRLogicProgramState(Goal.decompile("rel2,katie,-1")));
		for (Map.Entry<LogicProgramState,Double> e : result.entrySet()) {
			System.out.println(e.getValue()+"\t"+e.getKey());
		}
	}

}
