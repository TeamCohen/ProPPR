package edu.cmu.ml.proppr.prove.v1;

import static org.junit.Assert.*;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import edu.cmu.ml.proppr.prove.v1.Goal;
import edu.cmu.ml.proppr.prove.v1.GoalComponent;
import edu.cmu.ml.proppr.prove.v1.LogicProgram;
import edu.cmu.ml.proppr.prove.v1.Rule;
import edu.cmu.ml.proppr.prove.v1.RuleComponent;

public class LogicProgramTest {

	
	@Before
	public void setup() {
		BasicConfigurator.configure(); Logger.getRootLogger().setLevel(Level.WARN);
	}
	@Test
	public void testCompilationGuarantee() {
		Goal isaduck = new Goal("isa","X","duck"),
				hasFeathers = new Goal("hasFeathers","X"),
				f_covering = new Goal("covering"),
				howard = new Goal("hasFeathers","howard");

		RuleComponent p = new RuleComponent();
		p.add(new Rule(isaduck, f_covering, hasFeathers));
		GoalComponent g = new GoalComponent();
		g.addFact(howard);
		
		LogicProgram lp = new LogicProgram(p,g);
		// all goals should compile properly as a side effect of logic program construction
		assertTrue("isa _X_ duck",isaduck.getArg(0).isVariable());
		assertTrue("isa X _duck_",isaduck.getArg(1).isConstant());
		assertTrue("hasFeathers _X_",hasFeathers.getArg(0).isVariable());
		assertTrue("hasFeathers _howard_",howard.getArg(0).isConstant());
	}

}
