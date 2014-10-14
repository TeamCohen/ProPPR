package edu.cmu.ml.praprolog;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Map;

import org.junit.Test;

import edu.cmu.ml.praprolog.Tester.TestResults;
import edu.cmu.ml.praprolog.learn.L2PosNegLossTrainedSRW;
import edu.cmu.ml.praprolog.prove.v1.Component;
import edu.cmu.ml.praprolog.prove.v1.DprProver;
import edu.cmu.ml.praprolog.prove.v1.InnerProductWeighter;
import edu.cmu.ml.praprolog.prove.v1.LogicProgram;
import edu.cmu.ml.praprolog.prove.v1.Prover;
import edu.cmu.ml.praprolog.util.Dictionary;
import edu.cmu.ml.praprolog.util.ParamVector;
import edu.cmu.ml.praprolog.util.SimpleParamVector;

public class RerankingTesterTest {
	double EPSILON=1e-10;

	@Test
	public void test() {
		LogicProgram program = new LogicProgram(
				Component.loadComponents(
						"testcases/textcattoy/textcat.crules:testcases/textcattoy/toylabels.cfacts:testcases/textcattoy/toywords.graph".split(":"), 
						Component.ALPHA_DEFAULT,
						null));
		Prover prover = new DprProver();
		
		ParamVector params = new SimpleParamVector(Dictionary.load("testcases/textcattoy/params.wts.gold"));
		
		L2PosNegLossTrainedSRW<String> srw = new L2PosNegLossTrainedSRW<String>();
		
		RerankingTester rt = new RerankingTester(prover, program, srw);

		rt.setParams(params, null);
		TestResults rt_results = rt.testExamples(new File("testcases/textcattoy/toytest.data"));
		
		program.setFeatureDictWeighter(InnerProductWeighter.fromParamVec(params));
		Tester t = new Tester(prover, program);
		TestResults t_results = t.testExamples(new File("testcases/textcattoy/toytest.data"));
		
		assertEquals("pairTotal",t_results.pairTotal,rt_results.pairTotal, EPSILON);
		assertEquals("pairErrors",t_results.pairErrors,rt_results.pairErrors, EPSILON);
		assertEquals("map",t_results.map,rt_results.map, EPSILON);
	}

}
