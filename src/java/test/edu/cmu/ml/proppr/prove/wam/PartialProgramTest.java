package edu.cmu.ml.proppr.prove.wam;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import edu.cmu.ml.proppr.prove.DuplicateSignatureRuleTest;
import edu.cmu.ml.proppr.prove.Prover;
import edu.cmu.ml.proppr.prove.TracingDfsProver;
import edu.cmu.ml.proppr.prove.wam.LogicProgramException;
import edu.cmu.ml.proppr.prove.wam.ProofGraph;
import edu.cmu.ml.proppr.prove.wam.Query;
import edu.cmu.ml.proppr.prove.wam.WamBaseProgram;
import edu.cmu.ml.proppr.prove.wam.WamProgram;
import edu.cmu.ml.proppr.util.APROptions;

public class PartialProgramTest {

	@Test
	public void test() throws IOException, LogicProgramException {
		APROptions apr = new APROptions("depth=10");
		WamProgram program = WamBaseProgram.load(new File(DuplicateSignatureRuleTest.PROGRAM));
		ProofGraph pg = new ProofGraph(Query.parse("canExit(steve,X)"),apr,program);

		List<Outlink> first = pg.pgOutlinks(pg.getStartState(),true);
		assertEquals(2,first.size());
		first.remove(pg.getStartState());
		Outlink f1 = first.get(0);

		List<Outlink> second = pg.pgOutlinks(f1.child,true);
		assertEquals(2,second.size());
		second.remove(pg.getStartState());
		Outlink s1 = second.get(0);

		List<Outlink> third = pg.pgOutlinks(s1.child,true);
		assertEquals(3,third.size());
		third.remove(pg.getStartState());
		Outlink t1 = third.get(0);
		Outlink t2 = third.get(1);

		ProofGraph t1g = pg.subGraph();
		ProofGraph t2g = pg.subGraph();

		Prover p = new TracingDfsProver(apr);
		int i=0;
		for (ProofGraph pi : new ProofGraph[] {t1g,t2g}) {
			if (i++ == 1) System.out.println("t1"); else System.out.println("t2");
			Map<Query,Double> result = p.solvedQueries(pi);
			for (Map.Entry<Query, Double> e : result.entrySet()) {
				System.out.println(e.getValue()+"\t"+e.getKey());
				assertEquals("Steve not allowed to exit "+e.getKey()+"\n",
						"canExit(steve,kitchen).",e.getKey().toString());
			}
		}

		//		List<Outlink> t1_first = t1g.pgOutlinks(t1.child,true);
		//		assertEquals(2,t1_first.size());
		//		t1_first.remove(pg.getStartState());
		//		Outlink t1_f1 = t1_first.get(0);
		//		Query solution1 = pg.fill(t1_f1.child);
		//		System.out.println(solution1);
		//		
		//		List<Outlink> t2_first = t2g.pgOutlinks(t2.child,true);
		//		assertEquals(2,t2_first.size());
		//		t2_first.remove(pg.getStartState());
		//		Outlink t2_f1 = t2_first.get(0);
		//		Query solution2 = pg.fill(t2_f1.child);
		//		System.out.println(solution2);
	}



}
