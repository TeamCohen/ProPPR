package edu.cmu.ml.proppr.prove.wam.plugins;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import edu.cmu.ml.proppr.GrounderTest;
import edu.cmu.ml.proppr.prove.wam.LogicProgramException;
import edu.cmu.ml.proppr.prove.wam.Outlink;
import edu.cmu.ml.proppr.prove.wam.Query;
import edu.cmu.ml.proppr.prove.wam.WamInterpreter;
import edu.cmu.ml.proppr.util.Configuration;

public class SplitFactsPluginTest {
	public static final String ADDLFACTS = "src/testcases/classifyPredict_minAlphaAdditions.cfacts";
	@Test
	public void test() throws LogicProgramException {
		int input=0;
		int output = 0;
		int constants = Configuration.USE_WAM;
		int modules = 0;
		Configuration c = new Configuration(
				("--programFiles "+GrounderTest.RULES+":"+GrounderTest.FACTS+":"+ADDLFACTS).split(" "),
				input,output,constants,modules);
		assertEquals("# of plugins",c.plugins.length,1);
		assertEquals("# of members",((SplitFactsPlugin)c.plugins[0]).plugins.size(),2);
		assertTrue("claim",c.plugins[0].claim("validClass/1"));
		
		Query q = Query.parse("validClass(X)");
		WamInterpreter interp = new WamInterpreter(c.program,c.plugins);
		int queryStartAddr = c.program.size();
		q.variabilize();
		c.program.append(q);
		interp.executeWithoutBranching(queryStartAddr);
		List<Outlink> outs = c.plugins[0].outlinks(interp.saveState(), interp, true);
		assertEquals("# outlinks",6,outs.size());
	}

}
