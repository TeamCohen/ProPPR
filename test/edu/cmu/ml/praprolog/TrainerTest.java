package edu.cmu.ml.praprolog;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Before;
import org.junit.Test;

import edu.cmu.ml.praprolog.learn.L2PosNegLossTrainedSRW;
import edu.cmu.ml.praprolog.learn.SRW;
import edu.cmu.ml.praprolog.learn.tools.PosNegRWExample;
import edu.cmu.ml.praprolog.learn.tools.ReLUWeightingScheme;
import edu.cmu.ml.praprolog.util.Dictionary;
import edu.cmu.ml.praprolog.util.ParamVector;

public class TrainerTest extends RedBlueGraph {
	public Trainer<String> trainer;
	public SRW<PosNegRWExample<String>> srw;
	public Map<String,Double> query;
	public ArrayList<PosNegRWExample<String>> examples;
	
	public TrainerTest() {
		super(10);
	}
	
	public void initTrainer() {
		this.trainer = new Trainer<String>(this.srw);
	}
	
	@Before
	public void setup() {
		super.setup();
		this.srw = new L2PosNegLossTrainedSRW<String>();
		this.srw.setWeightingScheme(new ReLUWeightingScheme());
		this.initTrainer();
		
		query = new TreeMap<String,Double>();
		query.put("r0",1.0);
		examples = new ArrayList<PosNegRWExample<String>>();
		for (int k=0;k<this.magicNumber;k++) {
			for (int p=0;p<this.magicNumber;p++) {
				examples.add(new PosNegRWExample<String>(brGraphs.get(0), query, new String[]{"b"+k},new String[]{"r"+p}));
			}
		}
	}
	
	public ParamVector train() {
		return this.trainer.trainParametersOnCookedIterator(examples, 1, true);
	}

	@Test
	public void test() {
		ParamVector params = train();
		System.err.println(Dictionary.buildString((Map<String,Double>) params,new StringBuilder(),"\n"));
		for (Object o : params.keySet()) {
			String f = (String) o;
			if (f.equals("tob")) assertTrue(params.get(o) >= this.srw.getWeightingScheme().defaultWeight());
			if (f.equals("tor")) assertTrue(params.get(o) <= this.srw.getWeightingScheme().defaultWeight());
		}
	}

}
