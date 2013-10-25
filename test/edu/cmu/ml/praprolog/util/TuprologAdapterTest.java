package edu.cmu.ml.praprolog.util;

import static org.junit.Assert.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.Test;

import alice.tuprolog.InvalidTheoryException;
import alice.tuprolog.MalformedGoalException;
import alice.tuprolog.NoSolutionException;
import alice.tuprolog.Prolog;
import alice.tuprolog.SolveInfo;
import alice.tuprolog.Struct;
import alice.tuprolog.Term;
import alice.tuprolog.Theory;
import alice.tuprolog.Var;

import edu.cmu.ml.praprolog.prove.Argument;
import edu.cmu.ml.praprolog.prove.ConstantArgument;
import edu.cmu.ml.praprolog.prove.Goal;
import edu.cmu.ml.praprolog.prove.LogicProgramState;
import edu.cmu.ml.praprolog.prove.VariableArgument;

public class TuprologAdapterTest {

	@Test
	public void testArgToTerm() {
		Argument a = new ConstantArgument("william");
		Term ta = TuprologAdapter.argToTerm(a);
		assertTrue("Constant argument",ta instanceof Struct);
		
		Argument b = new VariableArgument(-1);
		Term tb = TuprologAdapter.argToTerm(b);
		assertTrue("Variable argument",tb instanceof Var);
		
		Goal g = Goal.decompile("writes,william,-1");
		Term ga = TuprologAdapter.argToTerm(g.getArg(0));
		Term gb = TuprologAdapter.argToTerm(g.getArg(1));
		
		assertTrue("Constant argument from goal",ga instanceof Struct);
		assertTrue("Constant argument is ground",ga.isGround());
		assertTrue("Variable argument from goal",gb instanceof Var);
		assertFalse("Variable isn't bound", ((Var)gb).isBound());
	}
	
	@Test
	public void testGoalToTerm() {
		Goal g = Goal.decompile("writes,william,-1");
		Term tg = TuprologAdapter.goalToTerm(g);
		Term tog = Term.createTerm("writes(william,X)");
		assertTrue("Gold truth by hand",tg.isEqual(tog)); // :(
		assertTrue(tg instanceof Struct);
		assertEquals(2,((Struct)tg).getArity());
		assertTrue(((Struct)tg).getArg(0) instanceof Struct);
		assertTrue(((Struct)tg).getArg(1) instanceof Var);
	}
	
	@Test
	public void testLpStateToTerm() {
		Goal g = Goal.decompile("writes,william,-1");
		LogicProgramState state = new LogicProgramState(g);
		Term ts = TuprologAdapter.lpStateToTerm(state);
		assertTrue(ts instanceof Struct);
		Struct sts = (Struct) ts;
		assertEquals(3,sts.getArity());
		for (int i=0; i<3; i++) {
			Term arg = sts.getArg(i);
			assertTrue(arg instanceof Struct);
			Struct sarg = (Struct) arg;
			assertTrue(sarg.isList());
			assertEquals(1,sarg.listSize());
			Term head = sarg.listHead(); 
			assertTrue(head instanceof Struct);
			assertTrue (head.isEqual(TuprologAdapter.goalToTerm(g)));
		}
	}

	@Test
	public void testLogicProgramState() throws InvalidTheoryException, FileNotFoundException, IOException, MalformedGoalException, NoSolutionException {
		Prolog engine = new Prolog();
		engine.addTheory(new Theory(new FileInputStream("outlinks.2p")));
		SolveInfo info = engine.solve("startState(writes(william,X),S).");
		Term t = info.getVarValue("S");
		Term tp = TuprologAdapter.lpStateToTerm(new LogicProgramState(Goal.decompile("writes,william,-1")));
		System.out.println(t);
		System.out.println(tp);
		assertTrue(t.isEqual(tp)); // :(
	}
	
}
