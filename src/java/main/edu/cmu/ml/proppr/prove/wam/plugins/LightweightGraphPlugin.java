package edu.cmu.ml.proppr.prove.wam.plugins;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.skjegstad.utils.BloomFilter;

import edu.cmu.ml.proppr.prove.DprProver;
import edu.cmu.ml.proppr.prove.wam.ConstantArgument;
import edu.cmu.ml.proppr.prove.wam.Goal;
import edu.cmu.ml.proppr.util.APROptions;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.ParsedFile;
import gnu.trove.map.TObjectDoubleMap;

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
	private static final Logger log = Logger.getLogger(LightweightGraphPlugin.class);
	protected Map<String,Map<String,TObjectDoubleMap<String>>> graph = new HashMap<String,Map<String,TObjectDoubleMap<String>>>();
	protected Map<Goal,Double> fd=new HashMap<Goal,Double>();
	protected String name;
	public LightweightGraphPlugin(APROptions apr, String name) {
		super(apr);
		this.fd.put(WamPlugin.pluginFeature(this, name),1.0);
		this.name = name;
	}

	@Override
	protected boolean indexContains(String label) {
		return graph.containsKey(label);
	}

	@Override
	protected TObjectDoubleMap<String> indexGet(String label, String src) {
		return Dictionary.safeGetGet(graph,label,src,DEFAULT_DSTLIST);
	}

	@Override
	protected Collection<String> indexGet(String label) {
		if (!graph.containsKey(label)) return DEFAULT_SRCLIST;
		return graph.get(label).keySet();
	}
	
	@Override
	protected void indexAdd(String label, String src, String dst) {
		Dictionary.safePut(graph, label, src, dst, DEFAULT_DSTWEIGHT);
	}

	@Override
	protected Map<Goal, Double> getFD() {
		return this.fd;
	}

	public static GraphlikePlugin load(APROptions apr, File f) {
		return load(apr, f, -1);
	}
	/** Return a simpleGraphComponent with all the components loaded from
        a file.  The format of the file is that each line is a tab-separated 
        triple of edgelabel, sourceNode, destNode. */
	public static GraphlikePlugin load(APROptions apr, File f, int duplicates) {
		GraphlikePlugin p = new LightweightGraphPlugin(apr, f.getName());
		ParsedFile parsed = new ParsedFile(f);
		BloomFilter<String> lines = null;
		if (duplicates>0) lines = new BloomFilter<String>(1e-5,duplicates);
		boolean exceeds=false;
		for (String line : parsed) {
			String[] parts = line.split("\t");
			if (parts.length < 3) parsed.parseError("expected 3 tab-delimited fields; got "+parts.length);
			if (duplicates>0) {
				if (lines.contains(line)) {
					log.warn("Skipping duplicate fact at "+f.getName()+":"+parsed.getAbsoluteLineNumber()+": "+line);
					continue;
				} else lines.add(line);
	
				if (!exceeds & parsed.getLineNumber() > duplicates) {
					exceeds=true;
					log.warn("Number of graph edges exceeds "+duplicates+"; duplicate detection may encounter false positives. We should add a command line option to fix this.");
				}
			}
			p.addEdge(parts[0].trim(),parts[1].trim(),parts[2].trim());
		}
		return p;
	}
	@Override
	public String about() {
		return this.getClass().getSimpleName()+":"+this.name;
	}
}
