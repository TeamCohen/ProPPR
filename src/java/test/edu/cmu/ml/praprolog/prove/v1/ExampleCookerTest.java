package edu.cmu.ml.praprolog.prove.v1;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import edu.cmu.ml.praprolog.examples.PosNegRWExample;
import edu.cmu.ml.praprolog.prove.v1.Component;
import edu.cmu.ml.praprolog.prove.v1.DprProver;
import edu.cmu.ml.praprolog.prove.v1.Goal;
import edu.cmu.ml.praprolog.prove.v1.LogicProgram;
import edu.cmu.ml.praprolog.prove.v1.PprProver;
import edu.cmu.ml.praprolog.prove.v1.Prover;
import edu.cmu.ml.praprolog.prove.v1.RawPosNegExample;
import edu.cmu.ml.praprolog.util.Dictionary;
import edu.cmu.ml.praprolog.v1.ExampleCooker;

public class ExampleCookerTest {

	
	@Before
	public void setup() {
		BasicConfigurator.configure(); Logger.getRootLogger().setLevel(Level.WARN);
	}
	@Test
	public void testCookExample() {
		// python newexamplecooker.py 
		// --programFiles testcases/classify.crules:testcases/toy.cfacts 
		// --data testcases/toyTrain.data 
		// --output testcases/toy.cooked
		
		// python examplecooker.py 
		// --programFiles demo/textcat/textcat.rules:demo/textcat/toylabels.facts:demo/textcat/toywords.graph 
//		--data demo/textcat/toytrain.data 
//		--prover 'prv.dprProver(epsilon=0.00001,maxDepth=5)' 
//		--output demo/textcat/toy.cooked
		doCookExampleTest("dpr: ",new DprProver(.00001, DprProver.MINALPH_DEFAULT),
				10, //nodes
				23, //edges
				"1",//key
				1.0,//value
				"7",  //pos
				"8",//neg
				DprProver.MINALPH_DEFAULT); 
		doCookExampleTest("ppr: ",new PprProver(),//.00001, DprProver.MINALPH_DEFAULT),
				10, //nodes
				23, //edges
				"1",//key
				1.0,//value
				"9",  //pos
				"10",//neg
				Component.ALPHA_DEFAULT); 
	}
	public void doCookExampleTest(String msg, Prover p, int nodes, int edges, String key, double value, String npos, String nneg,double alpha) {
		String[] programFiles = {"testcases/classify.crules","testcases/toy.cfacts"}; 
		String data = null;//"testcases/toyTrain.cdata";
		ExampleCooker cooker = new ExampleCooker(p, new LogicProgram(Component.loadComponents(programFiles,alpha,null)));
		
		ArrayList<String> pos = new ArrayList<String>(), neg = new ArrayList<String>();
		pos.add("predict howard bird");
		neg.add("predict howard mammal");
		
		Goal query = new Goal("predict","howard","Y");
		PosNegRWExample<String> ex = cooker.cookExample(new RawPosNegExample(query, pos, neg),cooker.getMasterProgram());

		assertTrue(msg+"query not compiled!",query.isCompiled());

		makeAssertions(ex,msg,nodes,edges,key,value,1,npos,1,nneg);
		// predict(howard,Y)	+predict(howard,bird)	-predict(howard,mammal)
	}

	
	private void makeAssertions(PosNegRWExample<String> ex, String msg,
			int nodes, int edges, String key, double value, int npos, String pos, int nneg, String neg) {
		assertEquals(msg+"pos size",npos,ex.getPosList().size());
//		if (npos>0) assertEquals(msg+"pos value",pos,ex.getPosList().get(0));
		assertEquals(msg+"neg size",nneg,ex.getNegList().size());
//		if (nneg>0) assertEquals(msg+"neg value",neg,ex.getNegList().get(0));
		assertEquals(msg+"nodes",nodes,ex.getGraph().getNodes().size());
		assertEquals(msg+"edges",edges,ex.getGraph().getNumEdges());
		assertTrue(msg+"query vec key",ex.getQueryVec().containsKey(key));
		assertEquals(msg+"query vec value",value,ex.getQueryVec().get(key),1e-5);
	}
	
}
