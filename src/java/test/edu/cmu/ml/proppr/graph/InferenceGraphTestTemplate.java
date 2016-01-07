package edu.cmu.ml.proppr.graph;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
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

public abstract class InferenceGraphTestTemplate {
	public abstract InferenceGraph getGraph();
	
	@Test
	public void test() {
		InferenceGraph g = getGraph();
		MutableState a = new MutableState(); a.setJumpTo("foo"); a.setCanonicalHash(2); a.setCanonicalForm("a");
		MutableState b = new MutableState(); b.setJumpTo("bar"); b.setCanonicalHash(2); b.setCanonicalForm("b");
		MutableState b2 = new MutableState(); b2.setJumpTo("bar"); b2.setCanonicalHash(2); b2.setCanonicalForm("b");
		MutableState c = new MutableState(); c.setJumpTo("baz"); c.setCanonicalHash(3); c.setCanonicalForm("c");
		Map<Feature,Double> fd = new HashMap<Feature,Double>();
		fd.put(new Feature("quite"),1.0);
		// a -> b
		List<Outlink> outlinks = new ArrayList<Outlink>();
		outlinks.add(new Outlink(fd, b));
		g.setOutlinks(g.getId(a), outlinks);
		// c -> b2 (=b)
		outlinks = new ArrayList<Outlink>();
		outlinks.add(new Outlink(fd, b2));
		g.setOutlinks(g.getId(c), outlinks);
		{
			String s = g.serialize(true);
			String[] parts = s.split("\t");
			assertEquals(6,parts.length);
			assertEquals("3",parts[0]);
			assertEquals("2",parts[1]);
			assertEquals("2",parts[2]);
			assertEquals("quite",parts[3]);
			String[] edges = new String[] {parts[4],parts[5]};
			Arrays.sort(edges);
			assertEquals("1->2:1@1.0",edges[0]);
			assertEquals("3->2:1@1.0",edges[1]);
		}
		
		{
			String s = g.serialize(false);
			String[] parts = s.split("\t");
			assertEquals(5,parts.length);
			assertEquals("3",parts[0]);
			assertEquals("2",parts[1]);
			assertEquals("2",parts[2]);
			String[] edges = new String[] {parts[3],parts[4]};
			Arrays.sort(edges);
			assertEquals("1->2:1@1.0",edges[0]);
			assertEquals("3->2:1@1.0",edges[1]);
		}
	}

}
