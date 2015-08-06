package edu.cmu.ml.proppr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Before;

import edu.cmu.ml.proppr.graph.LearningGraph;
import edu.cmu.ml.proppr.graph.ArrayLearningGraphBuilder;
import edu.cmu.ml.proppr.graph.LearningGraph;
import edu.cmu.ml.proppr.graph.LearningGraphBuilder;
import edu.cmu.ml.proppr.graph.RWOutlink;
import edu.cmu.ml.proppr.util.SymbolTable;
import edu.cmu.ml.proppr.util.SimpleSymbolTable;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.procedure.TIntDoubleProcedure;

/**
 * This is a template for a set of test cases that all use the same style of graph.
 * @author katie
 *
 */
/**
 * Test template (graph scaffolding)
 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
 *
 */
public class RedBlueGraph {
	protected LearningGraph brGraph;
	protected Set<String> reds;
	protected Set<String> blues;
	protected SymbolTable<String> nodes = new SimpleSymbolTable<String>();
	protected int magicNumber;

	public RedBlueGraph() {
		this(3);
	}

	public RedBlueGraph(int mn) {
		super();
		this.magicNumber = mn;
	}
	
	protected RWOutlink makeOutlink(LearningGraphBuilder lgb, Map<String,Double> fd, int dest) {
		int[] fid = new int[fd.size()];
		double[] wt = new double[fd.size()];
		int i=0;
		for (Map.Entry<String, Double> e : fd.entrySet()) {
			fid[i] = lgb.getFeatureLibrary().getId(e.getKey());
			wt[i] = e.getValue();
			i++;
		}
		return new RWOutlink(fid,wt,dest);
	}

	@Before
	public void setup() {
//		if (!Logger.getRootLogger().getAllAppenders().hasMoreElements()) {
//			BasicConfigurator.configure(); Logger.getRootLogger().setLevel(Level.WARN);
//		}

		LearningGraphBuilder lgb = new ArrayLearningGraphBuilder();
		brGraph = (LearningGraph) lgb.create(new SimpleSymbolTable<String>());
		lgb.index(1);
		lgb.setGraphSize(brGraph, magicNumber*2, -1, -1);

		//		brSRWs = new ArrayList<SRW>();
		//		Collections.addAll(brSRWs, new L2SqLossSRW(), new L2SqLossSRW(), new L2SqLossSRW());

		{
			int u = nodes.getId("b0"), v=nodes.getId("r0");
			HashMap<String,Double> ff = new HashMap<String,Double>();
			ff.put("fromb", 1.0);
			ff.put("tor",1.0);
			lgb.addOutlink(brGraph, u, makeOutlink(lgb,ff,v));

			ff = new HashMap<String,Double>();
			ff.put("fromr", 1.0);
			ff.put("tob",1.0);
			lgb.addOutlink(brGraph, v, makeOutlink(lgb,ff,u));
		}
		addColor(lgb, brGraph, magicNumber,"r");
		addColor(lgb, brGraph, magicNumber,"b");

		// save sets of red and blue nodes
		reds = new TreeSet<String>();
		blues = new TreeSet<String>();
		for (int ui=1; ui<(2*magicNumber+1); ui++) {
			String u = nodes.getSymbol(ui);
			if (u.startsWith("b")) blues.add(u);
			else reds.add(u);
		}
		
		
		moreSetup(lgb);
		lgb.freeze(brGraph);

		//			System.err.println("\n"+brGraphs.get(0).dump("r0"));
	}
	//template
	public void moreSetup(LearningGraphBuilder lgb) {}
	//template
	public void moreOutlinks(LearningGraphBuilder lgb,LearningGraph graph,int u) {}

	public void addColor(LearningGraphBuilder lgb, LearningGraph graph, int num, String label) {
		for (int x=0; x<num; x++) {
			String u = label+x;
			int uid=nodes.getId(u);
			for (int y=0; y<num; y++) {
//				if (x!=y) {
					String v = label+y;
					HashMap<String,Double> ff = new HashMap<String,Double>();
					ff.put("from"+label, 1.0);
					ff.put("to"+label,1.0);
					lgb.addOutlink(graph, uid, makeOutlink(lgb,ff,nodes.getId(v)));
//				}
			}
			moreOutlinks(lgb,graph,uid);
		}
	}

	public TIntDoubleMap bluePart(TIntDoubleMap vec) { return colorPart(blues,vec); }
	public Map<String,Double> bluePart(Map<String,Double> vec) { return colorPart(blues,vec); }

	public TIntDoubleMap redPart(TIntDoubleMap vec) { return colorPart(reds,vec); }
	public Map<String,Double> redPart(Map<String,Double> vec) { return colorPart(reds,vec); }

	public Map<String,Double> colorPart(Set<String> color, Map<String,Double> vec) {
		TreeMap<String,Double> result = new TreeMap<String,Double>();
		for (Map.Entry<String,Double> item : vec.entrySet()) {
			if (color.contains(item.getKey())) result.put(item.getKey(),item.getValue());
		}
		return result;
	}
	public TIntDoubleMap colorPart(final Set<String> color, TIntDoubleMap vec) {
		final TIntDoubleMap result = new TIntDoubleHashMap();
		vec.forEachEntry(new TIntDoubleProcedure() {

			@Override
			public boolean execute(int k, double v) {
				if (color.contains(nodes.getSymbol(k))) result.put(k, v);
				return true;
			}
		});
		return result;
	}



	public void equalScores(TIntDoubleMap vec1, TIntDoubleMap vec2) {
		for (int x : vec2.keys()) {
			assertTrue("vec1 must contain vec2 item "+x,vec1.containsKey(x));
		}
		for (int x : vec1.keys()) {
			assertTrue("vec2 must contain vec1 item "+x,vec2.containsKey(x));
			assertEquals(vec1.get(x),vec2.get(x),1e-10);
		}
	}

	public void lowerScores(TObjectDoubleMap<String> vec1, TObjectDoubleMap<String> vec2) {
		for (String x : vec2.keySet()) {
			assertTrue("vec1 must contain vec2 item "+x,vec1.containsKey(x));
		}
		for (String x : vec1.keySet()) {
			//			System.err.println(String.format("%s: vec1 %9.7f vec2 %9.7f",x,vec1.get(x),vec2.get(x)));
			assertTrue("vec2 must contain vec1 item "+x,vec2.containsKey(x));
			assertTrue("vec1 score "+vec1.get(x)+" must be less than vec2 score "+vec2.get(x),vec1.get(x)<vec2.get(x));
		}
	}
	public void lowerScores(TIntDoubleMap vec1, TIntDoubleMap vec2) {
		for (int x : vec2.keys()) assertTrue("vec1 must contain vec2 item "+x,vec1.containsKey(x));
		for (int x : vec1.keys()) {
			assertTrue("vec2 must contain vec1 item "+x,vec2.containsKey(x));
			assertTrue("for key "+x+" vec1 score "+vec1.get(x)+" must be less than vec2 score "+vec2.get(x),vec1.get(x)<vec2.get(x));
		}
	}

}