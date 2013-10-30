package edu.cmu.ml.praprolog.prove;

import static org.junit.Assert.*;

import java.util.Iterator;
import java.util.List;

import org.junit.Test;

import edu.cmu.ml.praprolog.prove.Component.Outlink;

public class TuprologLogicProgramStateTest {

	@Test
	public void testEquals() {
		TuprologLogicProgramState s1 = new TuprologLogicProgramState(Goal.decompile("sim,katie,-1"));
		TuprologComponent c = new TuprologComponent("testcases/prolog/family.pl");
		GoalComponent g = GoalComponent.loadCompiled("testcases/family-more.cfacts");
		List<Outlink> results = c.outlinks(s1);
		assertEquals(3,results.size());
		results.addAll(g.outlinks(results.get(1).state));
		assertEquals(4,results.size());
		results.addAll(c.outlinks(results.get(3).state));
		for (Iterator<Outlink> it = results.iterator(); it.hasNext(); ) {
			Outlink o = it.next();
			if (! o.getState().isSolution()) it.remove();
			else System.out.println(o.getState());
		}
		for (int i=0; i<results.size(); i++) {
			for (int j=i+1; j<results.size(); j++) {
				assertFalse("i "+i+" j "+j,results.get(i).state.equals(results.get(j).state));
			}
		}
	}

}
