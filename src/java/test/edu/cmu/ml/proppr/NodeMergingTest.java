package edu.cmu.ml.proppr;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import edu.cmu.ml.proppr.examples.GroundedExample;
import edu.cmu.ml.proppr.examples.InferenceExample;
import edu.cmu.ml.proppr.prove.DprProver;
import edu.cmu.ml.proppr.prove.Prover;
import edu.cmu.ml.proppr.prove.wam.LogicProgramException;
import edu.cmu.ml.proppr.prove.wam.ProofGraph;
import edu.cmu.ml.proppr.prove.wam.Query;
import edu.cmu.ml.proppr.prove.wam.State;
import edu.cmu.ml.proppr.prove.wam.StateProofGraph;
import edu.cmu.ml.proppr.prove.wam.WamBaseProgram;
import edu.cmu.ml.proppr.prove.wam.WamProgram;
import edu.cmu.ml.proppr.prove.wam.plugins.FactsPlugin;
import edu.cmu.ml.proppr.prove.wam.plugins.LightweightGraphPlugin;
import edu.cmu.ml.proppr.prove.wam.plugins.WamPlugin;
import edu.cmu.ml.proppr.util.APROptions;

/** 

 * @author krivard
 *
 */
public class NodeMergingTest {
	public static final File DIR = new File("src/testcases/nodeMerging");
	public static final File DIAMOND_RULES = new File(DIR,"diamond.wam");
	public static final File DIAMOND_FACTS = new File(DIR,"diamond.facts");
	public static final File TRAPEZOID_RULES = new File(DIR,"trapezoid.wam");
	public static final File TRAPEZOID_FACTS = new File(DIR,"trapezoid.facts");
	public static final File RECURSION_RULES = new File(DIR,"recursion.wam");
	public static final File RECURSION_FACTS = new File(DIR,"recursion.facts");
	public static final File LOOP_RULES = new File(DIR,"loop.wam");
	public static final File LOOP_FACTS = new File(DIR,"loop.facts");
	public static final File MULTIRANK_RULES = new File(DIR,"multirankwalk.wam");
	public static final File MULTIRANK_GRAPH = new File(DIR,"multirankwalk.graph");

	/**
	 * Diamond case: Solution is reachable by two equal-length paths.
	 * 
	 *                                          ,--db:r-- [5] -. 
	 * [1] --p/2-- [2] --v/2-- [3] --q/3-- [4] <                --db:s-- [7] --t/1-- [8] 
	 *                                          `--db:r-- [6] -' 
	 * @throws IOException
	 * @throws LogicProgramException
	 */
	@Test
	public void testDiamond() throws IOException, LogicProgramException {

		APROptions apr = new APROptions();
		Prover p = new DprProver(apr);
		WamProgram program = WamBaseProgram.load(DIAMOND_RULES);
		WamPlugin plugins[] = new WamPlugin[] {FactsPlugin.load(apr, DIAMOND_FACTS, false)};
		Grounder grounder = new Grounder(apr, p, program, plugins);

		Query query = Query.parse("p(a,P)");
		ProofGraph pg = new StateProofGraph(new InferenceExample(query, 
				new Query[] {Query.parse("p(a,d)")}, 
				new Query[] {}),
				apr,program, plugins);
		GroundedExample ex = grounder.groundExample(p, pg);
		System.out.println( grounder.serializeGroundedExample(pg, ex).replaceAll("\t", "\n"));
		assertEquals("improper node duplication",8,ex.getGraph().nodeSize());
	}
	
	/**
	 * Correct graph:
	 * 
	 *     ,-p/2-- [2] --db:q-- [4] --r/1-.
	 * [1] --------------p/2--------------- [3]
	 * 
	 * @throws IOException
	 * @throws LogicProgramException
	 */
	@Test
	public void testTrapezoid() throws IOException, LogicProgramException {

		APROptions apr = new APROptions();
		Prover p = new DprProver(apr);
		WamProgram program = WamBaseProgram.load(TRAPEZOID_RULES);
		WamPlugin plugins[] = new WamPlugin[] {FactsPlugin.load(apr, TRAPEZOID_FACTS, false)};
		Grounder grounder = new Grounder(apr, p, program, plugins);

		Query query = Query.parse("p(a,P)");
		ProofGraph pg = new StateProofGraph(new InferenceExample(query, 
				new Query[] {Query.parse("p(a,d)")}, 
				new Query[] {}),
				apr,program, plugins);
		GroundedExample ex = grounder.groundExample(p, pg);
		System.out.println( grounder.serializeGroundedExample(pg, ex).replaceAll("\t", "\n"));
		assertEquals("improper node duplication",4,ex.getGraph().nodeSize());
	}
	
	@Test
	public void testRecursion() throws IOException, LogicProgramException {

		APROptions apr = new APROptions();
		Prover p = new DprProver(apr);
		WamProgram program = WamBaseProgram.load(RECURSION_RULES);
		WamPlugin plugins[] = new WamPlugin[] {FactsPlugin.load(apr, RECURSION_FACTS, false)};
		Grounder grounder = new Grounder(apr, p, program, plugins);

		Query query = Query.parse("p(a,P)");
		ProofGraph pg = new StateProofGraph(new InferenceExample(query, 
				new Query[] {Query.parse("p(a,d)")}, 
				new Query[] {}),
				apr,program, plugins);
		GroundedExample ex = grounder.groundExample(p, pg);
		System.out.println( grounder.serializeGroundedExample(pg, ex).replaceAll("\t", "\n"));
		assertEquals("improper node duplication",7,ex.getGraph().nodeSize());
	}
	
	@Test
	public void testLoop() throws IOException, LogicProgramException {

		APROptions apr = new APROptions();
		Prover p = new DprProver(apr);
		WamProgram program = WamBaseProgram.load(LOOP_RULES);
		WamPlugin plugins[] = new WamPlugin[] {FactsPlugin.load(apr, LOOP_FACTS, false)};
		Grounder grounder = new Grounder(apr, p, program, plugins);

		Query query = Query.parse("p(a,P)");
		ProofGraph pg = new StateProofGraph(new InferenceExample(query, 
				new Query[] {Query.parse("p(a,d)")}, 
				new Query[] {}),
				apr,program, plugins);
		GroundedExample ex = grounder.groundExample(p, pg);
		System.out.println( grounder.serializeGroundedExample(pg, ex).replaceAll("\t", "\n"));
		assertEquals("improper node duplication",4,ex.getGraph().nodeSize());
	}
	
	@Test
	public void testMultiRank() throws IOException, LogicProgramException {

		APROptions apr = new APROptions();
		Prover p = new DprProver(apr);
		WamProgram program = WamBaseProgram.load(MULTIRANK_RULES);
		WamPlugin plugins[] = new WamPlugin[] {LightweightGraphPlugin.load(apr, MULTIRANK_GRAPH, -1)};
		Grounder grounder = new Grounder(apr, p, program, plugins);
		// predict(pos,X)  +predict(pos,seed1)     +predict(pos,other)
		Query query = Query.parse("predict(pos,X)");
		ProofGraph pg = new StateProofGraph(new InferenceExample(query, 
				new Query[] {Query.parse("predict(pos,seed1)"),
				Query.parse("predict(pos,f1)"),
				Query.parse("predict(pos,other)")}, 
				new Query[] {}),
				apr,program, plugins);
		GroundedExample ex = grounder.groundExample(p, pg);
		System.out.println( grounder.serializeGroundedExample(pg, ex).replaceAll("\t", "\n"));
		assertEquals("improper # solutions found",3, ex.getPosList().size());
	}
}
