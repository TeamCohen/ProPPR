package edu.cmu.ml.proppr.prove;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.Test;

import edu.cmu.ml.proppr.prove.wam.ConstantArgument;
import edu.cmu.ml.proppr.prove.wam.Goal;
import edu.cmu.ml.proppr.prove.wam.WamProgram;
import edu.cmu.ml.proppr.prove.wam.Outlink;
import edu.cmu.ml.proppr.prove.wam.Query;
import edu.cmu.ml.proppr.prove.wam.State;
import edu.cmu.ml.proppr.prove.wam.WamInterpreter;
import edu.cmu.ml.proppr.prove.wam.WamBaseProgram;
import edu.cmu.ml.proppr.prove.wam.plugins.WamPlugin;


public class WamInterpreterTest {
	int MAXDEPTH=10;
	@Test
	public void test() throws IOException {
		WamProgram program = WamBaseProgram.load(new File("testcases/wam/simpleProgram.wam"));
		WamInterpreter interp = new WamInterpreter(program, new WamPlugin[0]);
		// ? :- coworker(steve,X).
		Query query = new Query(new Goal("coworker",new ConstantArgument("steve"),new ConstantArgument("X")));
		List<State> answers = findAnswers(interp, query);
		for (State s : answers) System.out.println(s);
		assertEquals(2,answers.size());
	}
	
	public List<State> findAnswers(WamInterpreter wamInterp, Query query) {
		WamProgram program = wamInterp.getProgram();
		int queryStartAddr = program.size();
		query.variabilize();
		program.append(query);
		Map<Goal,Double> features = wamInterp.executeWithoutBranching(queryStartAddr);
		assertEquals(0,features.size());
		List<State> result = allSolutionsDFS(wamInterp, makeFeatures(new Goal("root")));
		program.revert();
		return result;
		
	}
	public Map<Goal,Double> makeFeatures(Goal g) {
		TreeMap<Goal,Double> ret = new TreeMap<Goal,Double>();
		ret.put(g,1.0);
		return ret;
	}
	public List<State> allSolutionsDFS(WamInterpreter wamInterp, Map<Goal,Double> incomingFeatures) {
		return allSolutionsDFS(wamInterp,incomingFeatures,0,new ArrayList<State>());
	}
	public List<State> allSolutionsDFS(WamInterpreter wamInterp, Map<Goal,Double> incomingFeatures,int depth,List<State> tail) {
		if (depth >= MAXDEPTH) return tail;
		if (wamInterp.getState().isCompleted()) {
			tail.add(wamInterp.getState());
			return tail;
		}
		for (Outlink o : outlinks(wamInterp)) {
			wamInterp.setState(o.child.mutableVersion());
			allSolutionsDFS(wamInterp, o.fd, depth+1, tail);
		}
		return tail;
	}
	public List<Outlink> outlinks(WamInterpreter wamInterp) {
		List<Outlink> result = new ArrayList<Outlink>();
		if (!wamInterp.getState().isCompleted() && !wamInterp.getState().isFailed()) {
			assertTrue("not at a call",wamInterp.getState().getJumpTo() != null);
			assertTrue("no definition for "+wamInterp.getState().getJumpTo(),
					wamInterp.getProgram().hasLabel(wamInterp.getState().getJumpTo()));
			State savedState = wamInterp.saveState();
			for (Integer addr : wamInterp.getProgram().getAddresses(wamInterp.getState().getJumpTo())) {
				wamInterp.restoreState(savedState);
				//try and match the rule head
				Map<Goal,Double> features = wamInterp.executeWithoutBranching(addr);
				if (!features.isEmpty() && !wamInterp.getState().isFailed()) {
					wamInterp.executeWithoutBranching();
					if (!wamInterp.getState().isFailed()) {
						result.add(new Outlink(features, wamInterp.getState()));
					}
				}
			}
		}
		return result;
	}
}
