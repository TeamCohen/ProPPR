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
import edu.cmu.ml.proppr.prove.TracingDfsProver;
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

public class BoundVariableGraphTest {
		public static final File DIR = new File("src/testcases/weighted");
		public static final File RULES = new File(DIR,"dbWeights.wam");
		public static final File GRAPH = new File(DIR,"tinycorpus.graph");

		@Test
		public void test() throws IOException, LogicProgramException {
			APROptions apr = new APROptions();
			Prover p = new DprProver(apr);
//			Prover p = new TracingDfsProver(apr);
			WamProgram program = WamBaseProgram.load(RULES);
			WamPlugin plugins[] = new WamPlugin[] {LightweightGraphPlugin.load(apr, GRAPH)};
			Grounder grounder = new Grounder(apr, p, program, plugins);

			Query query = Query.parse("hasWord(p1,good)");
			ProofGraph pg = new StateProofGraph(new InferenceExample(query, 
					new Query[] {Query.parse("hasWord(p1,good)")}, 
					new Query[0]),
					apr,new SimpleSymbolTable<Feature>(),program, plugins);
//			Map<String,Double> m = p.solutions(pg);
//			System.out.println(Dictionary.buildString(m, new StringBuilder(), "\n").toString());
			GroundedExample ex = grounder.groundExample(p, pg);
			ex.getGraph().serialize();
			String serialized = grounder.serializeGroundedExample(pg, ex).replaceAll("\t", "\n");
			System.out.println( serialized );
			
			assertEquals("Too many edges",4,ex.getGraph().edgeSize());
//			Map<String,Double> m = p.solvedQueries(pg);
//			System.out.println(Dictionary.buildString(m, new StringBuilder(), "\n"));
		}


}
