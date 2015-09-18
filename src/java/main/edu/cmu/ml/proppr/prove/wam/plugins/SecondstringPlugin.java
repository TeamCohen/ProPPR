package edu.cmu.ml.proppr.prove.wam.plugins;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.skjegstad.utils.BloomFilter;
import com.wcohen.ss.lookup.SoftTFIDFDictionary;

import edu.cmu.ml.proppr.prove.wam.Feature;
import edu.cmu.ml.proppr.prove.wam.LogicProgramException;
import edu.cmu.ml.proppr.prove.wam.Outlink;
import edu.cmu.ml.proppr.prove.wam.State;
import edu.cmu.ml.proppr.prove.wam.WamInterpreter;
import edu.cmu.ml.proppr.util.APROptions;
import edu.cmu.ml.proppr.util.ParsedFile;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;

/**
 * Plugin for dereferencing surface forms.
 * 
 * Input a DB file of the form
 * 
 * functor [tab] string [tab] id
 * 
 * to claim goals of the form
 * 
 * functor(X,Y) where X is soft-matched to a set of ids Y with link weight the similarity score.
 * @author krivard
 *
 */
public class SecondstringPlugin extends GraphlikePlugin {
	private static final double MIN_SIMILARITY_SCORE = 0.1;
	public static final String FILE_EXTENSION = "ssdb";
	protected HashMap<String,SoftTFIDFDictionary> dicts=new HashMap<String,SoftTFIDFDictionary>();
	protected Map<Feature,Double> fd=new HashMap<Feature,Double>();
	protected String name;
	
	public SecondstringPlugin(APROptions apr, String name) {
		super(apr);
		this.fd.put(WamPlugin.pluginFeature(this, name),1.0);
		this.name = name;
	}
	
	public static SecondstringPlugin load(APROptions apr, File f) {
		SecondstringPlugin p = new SecondstringPlugin(apr, f.getName());
		ParsedFile parsed = new ParsedFile(f);
		for (String line : parsed) {
			String[] parts = line.split("\t");
			if (parts.length < 3) parsed.parseError("expected 3 tab-delimited fields; got "+parts.length);
			if (parts.length==3) {
				p.addEdge(parts[0].trim(),parts[1].trim(),parts[2].trim());
			} else if (parts.length==4) {
				p.addEdge(parts[0].trim(),parts[1].trim(),parts[2].trim(),Double.parseDouble(parts[3].trim()));
			}
		}
		p.freeze();
		return p;
	}
	
	private void freeze() {
		for (SoftTFIDFDictionary dict : dicts.values()) dict.freeze();
	}

	@Override
	protected boolean indexContains(String label) {
		return dicts.containsKey(label);
	}

	@Override
	protected TObjectDoubleMap<String> indexGet(String label, String src) {
		TObjectDoubleMap<String> ret = new TObjectDoubleHashMap<String>();
		SoftTFIDFDictionary dict = dicts.get(label);
		int n = dict.lookup(MIN_SIMILARITY_SCORE, src);
		for (int i=0; i<n; i++) {
			ret.put((String) dict.getValue(i), dict.getScore(i));
		}
		return ret;
	}

	@Override
	protected Collection<String> indexGet(String label) {
		throw new UnsupportedOperationException("Can't get all surface forms from a SecondstringPlugin; first arg must be bound at execution time");
	}

	@Override
	protected void indexAdd(String label, String src, String dst) {
		if (!dicts.containsKey(label)) dicts.put(label, new SoftTFIDFDictionary());
		dicts.get(label).put(src, dst);
	}

	@Override
	protected void indexAdd(String label, String src, String dst, double weight) {
		throw new UnsupportedOperationException("SecondstringPlugin doesn't support weighted surface forms");
	}

	@Override
	protected Map<Feature, Double> getFD() {
		return this.fd;
	}

	@Override
	public String about() {
		return this.getClass().getSimpleName()+":"+this.name;
	}

}
