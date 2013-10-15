package edu.cmu.ml.praprolog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.Before;

import edu.cmu.ml.praprolog.graph.AnnotatedGraph;
import edu.cmu.ml.praprolog.graph.AnnotatedStringGraph;
import edu.cmu.ml.praprolog.graph.Feature;

public class RedBlueGraph {

	protected List<AnnotatedGraph<String>> brGraphs;
	protected Set<String> reds;
	protected Set<String> blues;
	protected int magicNumber = 3;

	public RedBlueGraph() {
		super();
	}

	@Before
	public void setup() {
			brGraphs = new ArrayList<AnnotatedGraph<String>>();
			Collections.addAll(brGraphs, new AnnotatedStringGraph(), new AnnotatedStringGraph(), new AnnotatedStringGraph());
			
	//		brSRWs = new ArrayList<SRW>();
	//		Collections.addAll(brSRWs, new L2SqLossSRW(), new L2SqLossSRW(), new L2SqLossSRW());
			
			addColor(brGraphs, magicNumber,"r");
			addColor(brGraphs, magicNumber,"b");
			{
				String u = "b0", v="r0";
				for (AnnotatedGraph<String> g : brGraphs) {
					ArrayList<Feature> ff = new ArrayList<Feature>();
					ff.add(new Feature("fromb", 1.0));
					ff.add(new Feature("tor",1.0));
					g.addDirectedEdge(u, v, ff);
	
					ff = new ArrayList<Feature>();
					ff.add(new Feature("fromr", 1.0));
					ff.add(new Feature("tob",1.0));
					g.addDirectedEdge(v,u, ff);
				}
			}		
			
			// save sets of red and blue nodes
			reds = new TreeSet<String>();
			blues = new TreeSet<String>();
			for (String u : brGraphs.get(0).getNodes()) {
				if (u.startsWith("b")) blues.add(u);
				else reds.add(u);
			}
			
//			System.err.println("\n"+brGraphs.get(0).dump("r0"));
		}

	public void addColor(List<AnnotatedGraph<String>> graphs, int num, String label) {
		for (int x=0; x<num; x++) {
			for (int y=0; y<num; y++) {
				if (x!=y) {
					String u = label+x;
					String v = label+y;
					for (AnnotatedGraph<String> g : graphs) {
						ArrayList<Feature> ff = new ArrayList<Feature>();
						ff.add(new Feature("from"+label, 1.0));
						ff.add(new Feature("to"+label,1.0));
						g.addDirectedEdge(u, v, ff);
					}
				}
			}
		}
	}

	public Map<String,Double> bluePart(Map<String,Double> vec) { return colorPart(blues,vec); }

	public Map<String,Double> redPart(Map<String,Double> vec) { return colorPart(reds,vec); }

	public Map<String,Double> colorPart(Set<String> color, Map<String,Double> vec) {
		TreeMap<String,Double> result = new TreeMap<String,Double>();
		for (Map.Entry<String,Double> item : vec.entrySet()) {
			if (color.contains(item.getKey())) result.put(item.getKey(),item.getValue());
		}
		return result;
	}

	public void equalScores(Map<String,Double> vec1, Map<String,Double> vec2) {
		for (String x : vec2.keySet()) {
			assertTrue("vec1 must contain vec2 item "+x,vec1.containsKey(x));
		}
		for (String x : vec1.keySet()) {
			assertTrue("vec2 must contain vec1 item "+x,vec2.containsKey(x));
			assertEquals(vec1.get(x),vec2.get(x),1e-10);
		}
	}

	public void lowerScores(Map<String,Double> vec1, Map<String,Double> vec2) {
			for (String x : vec2.keySet()) {
				assertTrue("vec1 must contain vec2 item "+x,vec1.containsKey(x));
			}
			for (String x : vec1.keySet()) {
	//			System.err.println(String.format("%s: vec1 %9.7f vec2 %9.7f",x,vec1.get(x),vec2.get(x)));
				assertTrue("vec2 must contain vec1 item "+x,vec2.containsKey(x));
				assertTrue("vec1 score "+vec1.get(x)+" must be less than vec2 score "+vec2.get(x),vec1.get(x)<vec2.get(x));
			}
		}

}