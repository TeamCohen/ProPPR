package edu.cmu.ml.proppr.graph;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import edu.cmu.ml.proppr.prove.wam.Argument;
import edu.cmu.ml.proppr.prove.wam.Goal;
import edu.cmu.ml.proppr.prove.wam.MutableState;
import edu.cmu.ml.proppr.prove.wam.Outlink;
import edu.cmu.ml.proppr.prove.wam.State;

public class InferenceGraphTest {

	@Test
	public void test() {
		InferenceGraph g = new LightweightStateGraph();
		List<Outlink> outlinks = new ArrayList<Outlink>();
		MutableState a = new MutableState(); a.setJumpTo("foo");
		MutableState b = new MutableState(); b.setJumpTo("bar");
		Map<Goal,Double> fd = new HashMap<Goal,Double>();
		fd.put(new Goal("quite",new Argument[0]),1.0);
		outlinks.add(new Outlink(fd, b));
		g.setOutlinks(a, outlinks);
		String s = g.serialize();
		String[] parts = s.split("\t");
		assertEquals(4,parts.length);
		assertEquals("2",parts[0]);
		assertEquals("1",parts[1]);
		assertEquals("quite",parts[2]);
		assertEquals("1->2:1@1.0",parts[3]);
	}

}
