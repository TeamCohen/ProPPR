package edu.cmu.ml.proppr;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;

import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.graph.ArrayLearningGraphBuilder;
import edu.cmu.ml.proppr.learn.L2SRW;
import edu.cmu.ml.proppr.learn.SRW;
import edu.cmu.ml.proppr.learn.tools.ReLU;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.SimpleSymbolTable;
import edu.cmu.ml.proppr.util.math.ParamVector;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;

public class TrainerTest extends RedBlueGraph {
	public Trainer trainer;
	public SRW srw;
	public TIntDoubleMap query;
	public ArrayList<String> examples;
	
	public TrainerTest() {
		super(10);
	}
	
	public void initTrainer() {
		this.trainer = new Trainer(this.srw);
	}
	
	@Before
	public void setup() {
		super.setup();
		this.srw = new L2SRW();
		this.srw.setSquashingFunction(new ReLU<String>());
		this.initTrainer();
		
		query = new TIntDoubleHashMap();
		query.put(nodes.getId("r0"),1.0);
		examples = new ArrayList<String>();
		for (int k=0;k<this.magicNumber;k++) {
			for (int p=0;p<this.magicNumber;p++) {
				examples.add(new PosNegRWExample(brGraph, query, 
						new int[]{nodes.getId("b"+k)},
						new int[]{nodes.getId("r"+p)}).serialize());
			}
		}
	}
	
	public ParamVector train() {
		return this.trainer.train(new SimpleSymbolTable<String>(),examples, new ArrayLearningGraphBuilder(), 5, true);
	}

	@Test
	public void test() {
		ParamVector params = train();
		System.err.println(Dictionary.buildString(params,new StringBuilder(),"\n"));
		for (Object o : params.keySet()) {
			String f = (String) o;
			if (f.equals("tob")) assertTrue("tob "+f,params.get(o) >= this.srw.getSquashingFunction().defaultValue());
			if (f.equals("tor")) assertTrue("tor "+f,params.get(o) <= this.srw.getSquashingFunction().defaultValue());
		}
	}

}
