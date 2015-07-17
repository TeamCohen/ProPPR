package edu.cmu.ml.proppr.graph;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import edu.cmu.ml.proppr.prove.wam.Argument;
import edu.cmu.ml.proppr.prove.wam.Feature;
import edu.cmu.ml.proppr.prove.wam.Goal;
import edu.cmu.ml.proppr.prove.wam.MutableState;
import edu.cmu.ml.proppr.prove.wam.Outlink;
import edu.cmu.ml.proppr.prove.wam.State;

public class InferenceGraphTest {
	@Test
	public void test() {
		LightweightStateGraph g = new LightweightStateGraph();
		List<Outlink> outlinks = new ArrayList<Outlink>();
		MutableState a = new MutableState(); a.setJumpTo("foo");
		MutableState b = new MutableState(); b.setJumpTo("bar");
		Map<Feature,Double> fd = new HashMap<Feature,Double>();
		fd.put(new Feature("quite"),1.0);
		outlinks.add(new Outlink(fd, b));
		g.setOutlinks(a, outlinks);
		{
			String s = g.serialize(true);
			String[] parts = s.split("\t");
			assertEquals(5,parts.length);
			assertEquals("2",parts[0]);
			assertEquals("1",parts[1]);
			assertEquals("1",parts[2]);
			assertEquals("quite",parts[3]);
			assertEquals("1->2:1@1.0",parts[4]);
		}
		
		{
			String s = g.serialize(false);
			String[] parts = s.split("\t");
			assertEquals(4,parts.length);
			assertEquals("2",parts[0]);
			assertEquals("1",parts[1]);
			assertEquals("1",parts[2]);
			assertEquals("1->2:1@1.0",parts[3]);
		}
	}

}
