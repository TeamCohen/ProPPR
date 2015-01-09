package edu.cmu.ml.proppr;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

import edu.cmu.ml.proppr.prove.PathDprProver;
import edu.cmu.ml.proppr.prove.wam.Goal;
import edu.cmu.ml.proppr.prove.wam.LogicProgramException;
import edu.cmu.ml.proppr.prove.wam.Outlink;
import edu.cmu.ml.proppr.prove.wam.ProofGraph;
import edu.cmu.ml.proppr.prove.wam.Query;
import edu.cmu.ml.proppr.prove.wam.State;
import edu.cmu.ml.proppr.prove.wam.WamBaseProgram;
import edu.cmu.ml.proppr.prove.wam.WamProgram;
import edu.cmu.ml.proppr.prove.wam.plugins.FactsPlugin;
import edu.cmu.ml.proppr.prove.wam.plugins.SparseGraphPlugin;
import edu.cmu.ml.proppr.prove.wam.plugins.SparseGraphPluginTest;
import edu.cmu.ml.proppr.prove.wam.plugins.WamPlugin;
import edu.cmu.ml.proppr.util.APROptions;

@Ignore
public class MiscDatasetTest {
	static final File DIR = new File("examples/20newsgroups");
	static final File RULES = new File(DIR,"20news.wam");
	static final File SPARSE = new File(DIR,"20news.sparse");
	static final File LABELS = new File(DIR,"labels.cfacts");
	static final File SEEDS = new File(DIR,"seed_doc_label.10per.cfacts");

	@Test
	public void test20Newsgroups() throws IOException, LogicProgramException {
		APROptions apr = new APROptions();
		WamProgram program = WamBaseProgram.load(RULES);
		WamPlugin plugins[] = new WamPlugin[] {
				SparseGraphPlugin.load(apr, SPARSE),
				FactsPlugin.load(apr,LABELS,false),
				FactsPlugin.load(apr,SEEDS,false)
		};
		PathDprProver p = new PathDprProver(apr);

		Query query = Query.parse("predict(comp.sys.mac.hardware:51779.txt,Y)");
		ProofGraph pg = new ProofGraph(query,apr,program,plugins);

		
		for (Outlink o : pg.pgOutlinks(pg.getStartState(), false, false)) {
			System.out.println(o.toString()); // seed; nonseed
			if (o.child.getJumpTo().equals("hasWord/2")) {
				for (Outlink oo : pg.pgOutlinks(o.child, false, false)) {
					// hasWord
					for (Outlink ooo : pg.pgOutlinks(oo.child, false, false)) {
						//wordIn
						for (Outlink m : pg.pgOutlinks(ooo.child, false, false)) {
							// predict again: seed; nonseed
							if (m.child.getJumpTo().equals("seed/2")) {
								for (Outlink mm : pg.pgOutlinks(m.child, false, false)) {
									System.out.println("    "+oo.toString().replaceAll("\n","\n    "));
									System.out.println("        "+ooo.toString().replaceAll("\n","\n        "));
									System.out.println("            "+m.toString().replaceAll("\n","\n            "));
									System.out.println("                "+mm.toString().replaceAll("\n","\n                "));
								}
							}
						}
					}		
				}
			}
		}


		//		Map<State,Double> ans = p.prove(pg);
		//		assertTrue(ans.size()>0);
		//		for(Map.Entry<State,Double> e : ans.entrySet()) {
		//			System.out.println(e.getKey());
		//		}
	}

}
