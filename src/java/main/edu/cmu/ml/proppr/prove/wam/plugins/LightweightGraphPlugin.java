package edu.cmu.ml.proppr.prove.wam.plugins;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cmu.ml.proppr.prove.wam.ConstantArgument;
import edu.cmu.ml.proppr.prove.wam.Goal;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.ParsedFile;

/**
 * Alpha is used to limit the minimum restart weight, when you
        use a uniformWeighter (or something related, like a fdWeighter
        with learned weights that are close to 1.0).
        
        With unit feature weights, a graph node of degree n will lead
        to an lpState with degree n+1, and have a restart weight that
        is 1/(n+1).  With alpha set, a new feature (named
        'alphaBooster') is introduced with a non-unit VALUE of n *
        (alpha/(1-alpha)) for the restart weight, which means that
        unit weights will give that edge a total weight of alpha.
 * @author "William Cohen <wcohen@cs.cmu.edu>"
 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
 *
 */
public class LightweightGraphPlugin extends GraphlikePlugin {
	private static final List<String> DEFAULT_DSTLIST=Collections.emptyList();
	public static final double DEFAULT_ALPHA=0.2;
	protected Map<String,Map<String,List<String>>> graph = new HashMap<String,Map<String,List<String>>>();
	protected Map<Goal,Double> fd=new HashMap<Goal,Double>();
	protected double alpha;
	protected String name;
	public LightweightGraphPlugin(String name) {
		this(name,DEFAULT_ALPHA);
	}
	public LightweightGraphPlugin(String name,double alpha) {
		this.fd.put(WamPlugin.pluginFeature(this, name),1.0);
		this.name = name;
		this.alpha = alpha;
	}

	@Override
	protected boolean indexContains(String label) {
		return graph.containsKey(label);
	}

	@Override
	protected List<String> indexGet(String label, String src) {
		return Dictionary.safeGetGet(graph,label,src,DEFAULT_DSTLIST);
	}

	@Override
	protected Collection<String> indexGet(String label) {
		if (!graph.containsKey(label)) return DEFAULT_DSTLIST;
		return graph.get(label).keySet();
	}
	
	@Override
	protected void indexAdd(String label, String src, String dst) {
		Dictionary.safeAppend(graph, label, src, dst);
	}

	@Override
	protected Map<Goal, Double> getFD() {
		return this.fd;
	}

	/** Return a simpleGraphComponent with all the components loaded from
        a file.  The format of the file is that each line is a tab-separated 
        triple of edgelabel, sourceNode, destNode. */
	public static LightweightGraphPlugin load(File f) {
		LightweightGraphPlugin p = new LightweightGraphPlugin(f.getName());
		ParsedFile parsed = new ParsedFile(f);
		for (String line : parsed) {
			String[] parts = line.split("\t");
			if (parts.length != 3) parsed.parseError("expected 3 tab-delimited fields; got "+parts.length);
			p.addEdge(parts[0].trim(),parts[1].trim(),parts[2].trim());
		}
		return p;
	}
	@Override
	public String about() {
		return this.getClass().getSimpleName()+":"+this.name;
	}
}
