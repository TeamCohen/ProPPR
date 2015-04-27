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
import edu.cmu.ml.proppr.prove.wam.LogicProgramException;
import edu.cmu.ml.proppr.prove.wam.ProofGraph;
import edu.cmu.ml.proppr.prove.wam.Query;
import edu.cmu.ml.proppr.prove.wam.WamBaseProgram;
import edu.cmu.ml.proppr.prove.wam.WamProgram;
import edu.cmu.ml.proppr.util.APROptions;

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
		ProofGraph pg = new ProofGraph(new InferenceExample(query, 
				new Query[] {Query.parse("predict(p1,pos)")}, 
				new Query[] {Query.parse("predict(p1,neg)")}),
				apr,program, plugins);
		GroundedExample ex = grounder.groundExample(p, pg);
		System.out.println( grounder.serializeGroundedExample(pg, ex).replaceAll("\t", "\n"));
		
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
		ProofGraph pg = new ProofGraph(new InferenceExample(query, 
				new Query[] {Query.parse("predict(p1,pos)")}, 
				new Query[] {Query.parse("predict(p1,neg)")}),
				apr,program, plugins);
		GroundedExample ex = grounder.groundExample(p, pg);
		System.out.println( grounder.serializeGroundedExample(pg, ex).replaceAll("\t", "\n"));
		
	}

}
