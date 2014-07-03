package edu.cmu.ml.praprolog.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.apache.log4j.Logger;

import edu.cmu.ml.praprolog.prove.Goal;
import edu.cmu.ml.praprolog.prove.LogicProgramState;

/**
 * @author krivard
 *
 */
public class GraphWriter {
private static final Logger log = Logger.getLogger(GraphWriter.class);
	private AnnotatedGraph<String> graph;
	private Map<Object,String> nodeDict;
	private ArrayList<Object> nodes= new ArrayList<Object>();
	private int nodeCtr;
	public GraphWriter() {
		clear();
	}
	private String intern(Object u) {
        if (this.nodeDict.containsKey(u)) { 
        	if (log.isDebugEnabled()) log.debug("old "+this.nodeDict.get(u)+" "+u);
            return this.nodeDict.get(u);
        } else { 
            this.nodeCtr += 1;
            String uId = String.valueOf(this.nodeCtr);
            this.nodeDict.put(u, uId);
            this.nodes.add(nodeCtr,u);
            if (log.isDebugEnabled()) log.debug("new "+uId+" "+u);
            return uId;
        }
	}
	public void clear() {
		this.graph = new AnnotatedStringGraph();
		this.nodeDict = new HashMap<Object,String>();
		this.nodeCtr = 0;
		nodes.add(0,null);
	}

	static int id = 1;
	public void writeEdge(Object u, Object v, List<Feature> f) {
		String uid = this.intern(u);
		String vid = this.intern(v);
		this.graph.addDirectedEdge(uid,vid,f);
		if(log.isDebugEnabled()) {
			log.debug("id "+id);id++;
			log.debug("update: "+this.graph.getNumNodes()+" "+this.graph.getNumEdges());
		}
	}
	public AnnotatedGraph<String> getGraph() { return this.graph; }
	public List<Feature> getFeatures(String u, String v) {
		return this.graph.phi(u,v);
	}
	public String getId(Object u) { 
		if (this.nodeDict.containsKey(u)) return this.nodeDict.get(u);
		else return "-1";
	}
	public int getNumNodes() {
		return nodeCtr;
	}
	public ArrayList<Object> getNodes() { return this.nodes; }
}
