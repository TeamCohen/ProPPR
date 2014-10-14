package edu.cmu.ml.praprolog.prove.v1;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

import edu.cmu.ml.praprolog.prove.v1.Component;
import edu.cmu.ml.praprolog.prove.v1.Goal;
import edu.cmu.ml.praprolog.prove.v1.GoalComponent;
import edu.cmu.ml.praprolog.prove.v1.LogicProgram;
import edu.cmu.ml.praprolog.prove.v1.LogicProgramState;
import edu.cmu.ml.praprolog.prove.v1.ProPPRLogicProgramState;
import edu.cmu.ml.praprolog.prove.v1.Rule;
import edu.cmu.ml.praprolog.prove.v1.RuleComponent;
import edu.cmu.ml.praprolog.prove.v1.TracingDfsProver;

public class RecursionTest {

	@Test @Ignore
	public void test() {
		/*
		 * interp(P,X,Y) :- interp(R1,X,Z), interp(R2,Z,Y), abduce_chain(P,R1,R2) #fixedWeight.
		 * interp(P,X,Y) :- extensionalVersionOf(P,R), rel(R,X,Z) #fixedWeight.
		 * >> interp(P,X,Y) :- extensionalVersionOf(P,R), rel(R,X,Y) #fixedWeight.
		 * abduce_chain(P,R1,R2) :- # chain(P,R1,R2).
		 */
		RuleComponent rc = new RuleComponent();
		rc.add(new Rule(
				Goal.decompile("interp,-1,-2,-3"),
				new Goal[] {Goal.decompile("interp,-4,-2,-5"),Goal.decompile("interp,-6,-5,-3"),Goal.decompile("abduce_chain,-1,-4,-6")},
				new Goal[] {new Goal("fixedWeight")}));
		rc.add(new Rule(
				Goal.decompile("interp,-1,-2,-3"),
				new Goal[] {Goal.decompile("extensionalVersionOf,-1,-4"),Goal.decompile("rel,-4,-2,-3")},
				new Goal[] {new Goal("fixedWeight")}));
		rc.add(new Rule(
				Goal.decompile("abduce_chain,-1,-2,-3"),
				new Goal[] {},
				new Goal[] {Goal.decompile("chain,-1,-2,-3")}));
		/*
		 * extensionalVersionOf(i_father,father)
extensionalVersionOf(i_mother,mother)
extensionalVersionOf(i_husband,husband)
extensionalVersionOf(i_wife,wife)
extensionalVersionOf(i_son,son)
extensionalVersionOf(i_daughter,daughter)
extensionalVersionOf(i_brother,brother)
extensionalVersionOf(i_sister,sister)
extensionalVersionOf(i_uncle,uncle)
extensionalVersionOf(i_aunt,aunt)
extensionalVersionOf(i_nephew,nephew)
extensionalVersionOf(i_niece,niece)

rel     brother alfonso sophia
rel     brother emilio  lucia
rel     brother marco   angela
rel     daughter        angela  francesca
rel     daughter        angela  pierro
rel     daughter        lucia   maria
rel     daughter        lucia   roberto
rel     daughter        sophia  lucia
rel     daughter        sophia  marco
rel     father  marco   alfonso
rel     father  marco   sophia
rel     father  pierro  angela
rel     father  pierro  marco
rel     father  roberto emilio
rel     father  roberto lucia
rel     husband emilio  gina
rel     husband marco   lucia
rel     husband pierro  francesca
rel     husband roberto maria
rel     husband tomaso  angela
rel     mother  francesca       angela
rel     mother  francesca       marco
rel     mother  lucia   alfonso
rel     mother  lucia   sophia
rel     mother  maria   emilio
rel     mother  maria   lucia
rel     sister  angela  marco
rel     sister  lucia   emilio
rel     sister  sophia  alfonso
rel     son     alfonso lucia
rel     son     alfonso marco
rel     son     emilio  maria
rel     son     emilio  roberto
rel     son     marco   francesca
rel     son     marco   pierro
rel     wife    angela  tomaso
rel     wife    francesca       pierro
rel     wife    gina    emilio
rel     wife    lucia   marco
rel     wife    maria   roberto

goal(interp,c[i_aunt],c[angela],v[-1])
		 */
		GoalComponent extensions = GoalComponent.loadCompiled("testcases/recursion/extensions.cfacts");
		GoalComponent family = GoalComponent.loadCompiled("testcases/recursion/family.cfacts");
		
		LogicProgram lp = new LogicProgram(new Component[] {rc,extensions,family});
		TracingDfsProver prover = new TracingDfsProver(10);
		Map<LogicProgramState,Double> angela = prover.proveState(lp, new ProPPRLogicProgramState(Goal.decompile("interp,i_aunt,angela,-1")));
		for (LogicProgramState s : angela.keySet()) {
			if (s.isSolution()) System.out.println(s);
		}
	}
}
