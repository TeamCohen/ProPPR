package edu.cmu.ml.proppr;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import edu.cmu.ml.proppr.examples.GroundedExample;
import edu.cmu.ml.proppr.examples.InferenceExample;
import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.prove.DprProver;
import edu.cmu.ml.proppr.prove.PprProver;
import edu.cmu.ml.proppr.prove.Prover;
import edu.cmu.ml.proppr.prove.wam.Goal;
import edu.cmu.ml.proppr.prove.wam.LogicProgramException;
import edu.cmu.ml.proppr.prove.wam.ProofGraph;
import edu.cmu.ml.proppr.prove.wam.Query;
import edu.cmu.ml.proppr.prove.wam.WamBaseProgram;
import edu.cmu.ml.proppr.prove.wam.WamProgram;
import edu.cmu.ml.proppr.prove.wam.plugins.FactsPlugin;
import edu.cmu.ml.proppr.prove.wam.plugins.WamPlugin;
import edu.cmu.ml.proppr.util.APROptions;
import edu.cmu.ml.proppr.util.Dictionary;

public class GrounderTest {
	public static final String FACTS = "src/testcases/classifyPredict.cfacts";
	public static final String RULES = "src/testcases/classifyPredict.wam";


	@Before
	public void setup() {
		BasicConfigurator.configure(); Logger.getRootLogger().setLevel(Level.WARN);
	}
	@Test
	public void testGroundExample() throws IOException, LogicProgramException {
		// python newexamplecooker.py 
		// --programFiles testcases/classify.crules:testcases/toy.cfacts 
		// --data testcases/toyTrain.data 
		// --output testcases/toy.cooked
		
		// python examplecooker.py 
		// --programFiles demo/textcat/textcat.rules:demo/textcat/toylabels.facts:demo/textcat/toywords.graph 
//		--data demo/textcat/toytrain.data 
//		--prover 'prv.dprProver(epsilon=0.00001,maxDepth=5)' 
//		--output demo/textcat/toy.cooked
		doGroundExampleTest("dpr: ",new DprProver(),
				10, //nodes
				23, //edges
				1.0,//value
				"7",  //pos
				"8",//neg
				APROptions.MINALPH_DEFAULT); 
		doGroundExampleTest("ppr: ",new PprProver(),
				10, //nodes
				23, //edges
				1.0,//value
				"9",  //pos
				"10",//neg
				APROptions.MINALPH_DEFAULT); 
	}
	public void doGroundExampleTest(String msg, Prover p, int nodes, int edges, double value, String npos, String nneg,double alpha) throws IOException, LogicProgramException {
		APROptions apr = new APROptions();
		WamProgram program = WamBaseProgram.load(new File(RULES));
		WamPlugin plugins[] = new WamPlugin[] {FactsPlugin.load(apr, new File(FACTS), false)};
		Grounder grounder = new Grounder(apr, p, program, plugins);
		
		Query query = Query.parse("predict(howard,Y)");
		GroundedExample ex = grounder.groundExample(p, new InferenceExample(query, 
				new Query[] {Query.parse("predict(howard,bird)")}, 
				new Query[] {Query.parse("predict(howard,mammal)")}));

		makeAssertions(ex,msg,nodes,edges,value,1,npos,1,nneg);
		// predict(howard,Y)	+predict(howard,bird)	-predict(howard,mammal)
	}

	
	private void makeAssertions(GroundedExample ex, String msg,
			int nodes, int edges, double value, int npos, String pos, int nneg, String neg) {
		assertEquals(msg+"pos size",npos,ex.getPosList().size());
//		if (npos>0) assertEquals(msg+"pos value",pos,ex.getPosList().get(0));
		assertEquals(msg+"neg size",nneg,ex.getNegList().size());
//		if (nneg>0) assertEquals(msg+"neg value",neg,ex.getNegList().get(0));
		assertEquals(msg+"nodes",nodes,ex.getGraph().nodeSize());
		assertEquals(msg+"edges",edges,ex.getGraph().edgeSize());
		assertEquals(msg+"query vec value",value,ex.getQueryVec().values().iterator().next(),1e-5);
	}
	
}
