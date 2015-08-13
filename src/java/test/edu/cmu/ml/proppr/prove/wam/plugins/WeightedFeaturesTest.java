package edu.cmu.ml.proppr.prove.wam.plugins;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import edu.cmu.ml.proppr.Grounder;
import edu.cmu.ml.proppr.examples.GroundedExample;
import edu.cmu.ml.proppr.examples.InferenceExample;
import edu.cmu.ml.proppr.prove.DprProver;
import edu.cmu.ml.proppr.prove.Prover;
import edu.cmu.ml.proppr.prove.wam.Feature;
import edu.cmu.ml.proppr.prove.wam.Goal;
import edu.cmu.ml.proppr.prove.wam.LogicProgramException;
import edu.cmu.ml.proppr.prove.wam.ProofGraph;
import edu.cmu.ml.proppr.prove.wam.Query;
import edu.cmu.ml.proppr.prove.wam.StateProofGraph;
import edu.cmu.ml.proppr.prove.wam.WamBaseProgram;
import edu.cmu.ml.proppr.prove.wam.WamProgram;
import edu.cmu.ml.proppr.util.APROptions;
import edu.cmu.ml.proppr.util.SimpleSymbolTable;

public class WeightedFeaturesTest {
	public static final File DIR = new File("src/testcases/weighted");
	public static final File RULES = new File(DIR,"tiny.wam");
	public static final File LABELS = new File(DIR,"tinylabels.cfacts");
	public static final File WORDSGRAPH = new File(DIR,"tinycorpus.graph");
	public static final File WORDSFACTS = new File(DIR,"tinycorpus.cfacts");
	

	@Test
	public void testAsGraph() throws IOException, LogicProgramException {
		APROptions apr = new APROptions();
		Prover p = new DprProver(apr);
		WamProgram program = WamBaseProgram.load(RULES);
		WamPlugin plugins[] = new WamPlugin[] {FactsPlugin.load(apr, LABELS, false), LightweightGraphPlugin.load(apr, WORDSGRAPH, -1)};
		Grounder grounder = new Grounder(apr, p, program, plugins);
		assertTrue(plugins[1].claim("hasWord#/3"));

		Query query = Query.parse("predict(p1,Y)");
		ProofGraph pg = new StateProofGraph(new InferenceExample(query, 
				new Query[] {Query.parse("predict(p1,pos)")}, 
				new Query[] {Query.parse("predict(p1,neg)")}),
				apr,new SimpleSymbolTable<Feature>(),program, plugins);
		GroundedExample ex = grounder.groundExample(p, pg);
		String serialized = grounder.serializeGroundedExample(pg, ex).replaceAll("\t", "\n");
		System.out.println( serialized );
		// hack
		assertTrue("Word weights must appear in ground graph",serialized.indexOf("0.9")>0);
		assertTrue("Word weights must appear in ground graph",serialized.indexOf("0.1")>0);
		
	}
	
	@Test
	public void testAsFacts() throws IOException, LogicProgramException {
		APROptions apr = new APROptions();
		Prover p = new DprProver(apr);
		WamProgram program = WamBaseProgram.load(RULES);
		WamPlugin plugins[] = new WamPlugin[] {FactsPlugin.load(apr, LABELS, false), FactsPlugin.load(apr, WORDSFACTS, false)};
		Grounder grounder = new Grounder(apr, p, program, plugins);
		assertTrue(plugins[1].claim("hasWord#/3"));

		Query query = Query.parse("predict(p1,Y)");
		ProofGraph pg = new StateProofGraph(new InferenceExample(query, 
				new Query[] {Query.parse("predict(p1,pos)")}, 
				new Query[] {Query.parse("predict(p1,neg)")}),
				apr,new SimpleSymbolTable<Feature>(),program, plugins);
		GroundedExample ex = grounder.groundExample(p, pg);
		String serialized = grounder.serializeGroundedExample(pg, ex).replaceAll("\t", "\n");
		System.out.println( serialized );
		// hack
		assertTrue("Word weights must appear in ground graph",serialized.indexOf("0.9")>0);
		assertTrue("Word weights must appear in ground graph",serialized.indexOf("0.1")>0);
	}

}
