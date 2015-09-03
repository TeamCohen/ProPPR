package edu.cmu.ml.proppr;

import static org.junit.Assert.*;
import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.graph.ArrayLearningGraphBuilder;
import edu.cmu.ml.proppr.learn.L2SRW;
import edu.cmu.ml.proppr.learn.SRW;
import edu.cmu.ml.proppr.learn.tools.ReLU;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.math.ParamVector;
import edu.cmu.ml.proppr.util.math.SimpleParamVector;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;

import java.util.ArrayList;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;

public class GradientFinderTest extends RedBlueGraph {

	public Trainer trainer;
	public SRW srw;
	public TIntDoubleMap query;
	public ArrayList<String> examples;
	
	public GradientFinderTest() {
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
				StringBuilder serialized = new StringBuilder("r0")
				.append("\t").append(nodes.getId("r0")) //query
				.append("\t").append(nodes.getId("b"+k)) // pos
				.append("\t").append(nodes.getId("r"+p)) //neg
				.append("\t").append(brGraph.nodeSize()) // nodes
				.append("\t").append(brGraph.edgeSize()) //edges
				.append("\t"); // waiting for .append(-1) // label dependencies
				int labelDependencies = 0;
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i<brGraph.getFeatureSet().size(); i++) {
					if (i>0) sb.append(":");
					sb.append(brGraph.featureLibrary.getSymbol(i+1));
				}
				for (int u=0; u<brGraph.node_hi; u++) {
					HashSet<Integer> outgoingFeatures = new HashSet<Integer>();
					for (int ec=brGraph.node_near_lo[u]; ec<brGraph.node_near_hi[u]; ec++) {
						int v = brGraph.edge_dest[ec];
						sb.append("\t").append(u).append("->").append(v).append(":");
						for (int lc = brGraph.edge_labels_lo[ec]; lc < brGraph.edge_labels_hi[ec]; lc++) {
							outgoingFeatures.add(brGraph.label_feature_id[lc]);
							if (lc > brGraph.edge_labels_lo[ec]) sb.append(",");
							sb.append(brGraph.label_feature_id[lc]).append("@").append(brGraph.label_feature_weight[lc]);
						}
					}
					labelDependencies += outgoingFeatures.size() * (brGraph.node_near_hi[u]-brGraph.node_near_lo[u]);
				}
				serialized.append(labelDependencies).append("\t").append(sb);
				examples.add(serialized.toString());
			}
		}
	}
	
	public ParamVector<String,?> train() {
		return this.trainer.findGradient(examples, new ArrayLearningGraphBuilder(), new SimpleParamVector<String>());
	}

	@Test
	public void test() {
		ParamVector<String,?> params = train();
		System.err.println(Dictionary.buildString(params,new StringBuilder(),"\n"));
		for (Object o : params.keySet()) {
			String f = (String) o;
			if (f.equals("tob")) assertTrue("tob "+f,params.get(o) <= 0);//this.srw.getWeightingScheme().defaultWeight());
			if (f.equals("tor")) assertTrue("tor "+f,params.get(o) >= 0);//this.srw.getWeightingScheme().defaultWeight());
		}
	}


}
