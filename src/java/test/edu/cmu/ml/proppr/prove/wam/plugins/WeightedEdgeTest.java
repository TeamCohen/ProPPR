package edu.cmu.ml.proppr.prove.wam.plugins;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.junit.Test;

import edu.cmu.ml.proppr.Grounder;
import edu.cmu.ml.proppr.examples.GroundedExample;
import edu.cmu.ml.proppr.examples.InferenceExample;
import edu.cmu.ml.proppr.prove.DprProver;
import edu.cmu.ml.proppr.prove.Prover;
import edu.cmu.ml.proppr.prove.wam.Feature;
import edu.cmu.ml.proppr.prove.wam.LogicProgramException;
import edu.cmu.ml.proppr.prove.wam.ProofGraph;
import edu.cmu.ml.proppr.prove.wam.Query;
import edu.cmu.ml.proppr.prove.wam.StateProofGraph;
import edu.cmu.ml.proppr.prove.wam.WamBaseProgram;
import edu.cmu.ml.proppr.prove.wam.WamProgram;
import edu.cmu.ml.proppr.util.APROptions;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.SimpleSymbolTable;

public class WeightedEdgeTest {
		public static final File DIR = new File("src/testcases/weighted");
		public static final File RULES = new File(DIR,"dbWeights.wam");
		public static final File FACTS = new File(DIR,"tinycorpus.cfacts");

		@Test
		public void test() throws IOException, LogicProgramException {
			APROptions apr = new APROptions();
			Prover p = new DprProver(apr);
			WamProgram program = WamBaseProgram.load(RULES);
			WamPlugin plugins[] = new WamPlugin[] {FactsPlugin.load(apr, FACTS, false)};
			Grounder grounder = new Grounder(apr, p, program, plugins);
			assertTrue("Missing weighted functor",plugins[0].claim("hasWord#/3"));

			Query query = Query.parse("words(p1,W)");
			ProofGraph pg = new StateProofGraph(new InferenceExample(query, 
					new Query[] {Query.parse("words(p1,good)")}, 
					new Query[] {Query.parse("words(p1,thing)")}),
					apr,new SimpleSymbolTable<Feature>(),program, plugins);
//			Map<String,Double> m = p.solutions(pg);
//			System.out.println(Dictionary.buildString(m, new StringBuilder(), "\n").toString());
			GroundedExample ex = grounder.groundExample(p, pg);
			ex.getGraph().serialize();
			String serialized = grounder.serializeGroundedExample(pg, ex).replaceAll("\t", "\n");
			System.out.println( serialized );
			assertTrue("Rule weights must appear in ground graph (0.9)",serialized.indexOf("0.9")>=0);
			assertTrue("Rule weights must appear in ground graph (0.1)",serialized.indexOf("0.1")>=0);
//			Map<String,Double> m = p.solvedQueries(pg);
//			System.out.println(Dictionary.buildString(m, new StringBuilder(), "\n"));
		}


}
