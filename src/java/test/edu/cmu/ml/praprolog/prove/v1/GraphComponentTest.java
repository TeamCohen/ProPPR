package edu.cmu.ml.praprolog.prove.v1;

import static org.junit.Assert.*;

import org.junit.Test;

import edu.cmu.ml.praprolog.prove.v1.Goal;
import edu.cmu.ml.praprolog.prove.v1.LogicProgramState;
import edu.cmu.ml.praprolog.prove.v1.ProPPRLogicProgramState;
import edu.cmu.ml.praprolog.util.SymbolTable;

public class GraphComponentTest {

	@Test
	public void test() {
		// goal(hasWord,c[dh],v[-2])
		Goal g = new Goal("hasWord","dh","X");
		g.compile(new SymbolTable());
		assertTrue(g.getArg(0).isConstant());
		assertTrue(g.getArg(1).isVariable());
		LogicProgramState state = new ProPPRLogicProgramState(g);
	}

}
