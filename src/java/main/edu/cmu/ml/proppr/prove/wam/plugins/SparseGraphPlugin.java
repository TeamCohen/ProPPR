package edu.cmu.ml.proppr.prove.wam.plugins;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.prove.wam.Goal;
import edu.cmu.ml.proppr.util.APROptions;
import edu.cmu.ml.proppr.util.ParsedFile;
import edu.cmu.ml.proppr.util.SymbolTable;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

public class SparseGraphPlugin extends GraphlikePlugin {
	private static final Logger log = Logger.getLogger(SparseGraphPlugin.class);	
	public static final String FILE_EXTENSION = ".sparse"; 
	public static final String INDEX_EXTENSION = ".i";
	public static final String MANIFEST="sparseIndex.txt";
	private static final long LOGUPDATE_MS = 10000;

	protected String name;
	protected SymbolTable<String> functors=new SymbolTable<String>();
	protected Map<Goal, Double> featureDict;
	protected TIntObjectMap<SparseMatrixIndex> index;
	protected TIntObjectMap<TObjectIntMap<String>> arg1s;
	protected TIntObjectMap<String[]> arg2s;
	
	public SparseGraphPlugin(APROptions apr, File matrixDir) {
		super(apr);
		
		log.info("Loading sparse graph component "+matrixDir);
		long start = System.currentTimeMillis();
		arg1s = new TIntObjectHashMap<TObjectIntMap<String>>();
		arg2s = new TIntObjectHashMap<String[]>();

		index=new TIntObjectHashMap<SparseMatrixIndex>();
		for(String matrix: new ParsedFile(new File(matrixDir,MANIFEST))) {
			String[] parts = matrix.split("_");
			int[] partIDs = new int[parts.length];
			for (int i=0;i<parts.length;i++) partIDs[i] = functors.getId(parts[i]);
			if (index.containsKey(partIDs[0])) {
				throw new IllegalArgumentException("Only one matrix allowed per functor. You've already used '"+partIDs[0]+"'");
			}
			if (!arg1s.containsKey(partIDs[1])) { // read arg1.i 
				arg1s.put(partIDs[1], new TObjectIntHashMap<String>());
				loadArgs(arg1s.get(partIDs[1]),new File(matrixDir,parts[1]+INDEX_EXTENSION));
			}
			if (!arg2s.containsKey(partIDs[2])) {
				ParsedFile rce = new ParsedFile(new File(matrixDir,matrix+".rce"));
				Iterator<String> rceit = rce.iterator(); rceit.next();
				String line = rceit.next();
				rce.close();
				if (line==null) throw new IllegalArgumentException("Bad format for "+matrix+".rce: line 2 must list #cols");
				int ncols = Integer.parseInt(line.trim());
				arg2s.put(partIDs[2], new String[ncols]);

				loadArgs(arg2s.get(partIDs[2]),new File(matrixDir,parts[2]+INDEX_EXTENSION));
			}
			try {
				index.put(partIDs[0], new SparseMatrixIndex(matrixDir,matrix,arg1s.get(partIDs[1]),arg2s.get(partIDs[2])));
			} catch (Exception e) {
				log.error("Problem reading sparse matrix "+matrix+".* in "+matrixDir,e);
				throw new RuntimeException(e);
			}
		}
		this.featureDict = new HashMap<Goal,Double>();
		this.featureDict.put(WamPlugin.pluginFeature(this, matrixDir.getName()),1.0);
		this.featureDict = Collections.unmodifiableMap(this.featureDict);
		
		long del = System.currentTimeMillis() - start;
		if (del > LOGUPDATE_MS)
			log.info("Finished loading sparse graph component "+matrixDir+" ("+(del/1000.)+" sec)");
	}
	
	private String clipArity(String label) {
		return label.substring(0,label.length()-GRAPH_ARITY.length());
	}
	
	@Override
	protected boolean indexContains(String label) {
		return this.functors.hasId(clipArity(label));
	}

	@Override
	protected TObjectDoubleMap<String> indexGet(String label, String src) {
		label = clipArity(label);
		if (!functors.hasId(label)) return DEFAULT_DSTLIST;
		TObjectDoubleMap<String> ret = index.get(functors.getId(label)).near(src);
		if (ret == null) return DEFAULT_DSTLIST;
		return ret;
	}

	@Override
	protected Collection<String> indexGet(String label) {
		label = clipArity(label);
		if (!functors.hasId(label)) return DEFAULT_SRCLIST;
		int id = functors.getId(label);
		if (!index.containsKey(id)) return DEFAULT_SRCLIST;
		return index.get(id).allSrc();
	}

	@Override
	protected void indexAdd(String label, String src, String dst) {
		throw new UnsupportedOperationException("Can't add to a sparse graph!");
	}

	@Override
	protected void indexAdd(String label, String src, String dst,double weight) {
		throw new UnsupportedOperationException("Can't add to a sparse graph!");
	}

	@Override
	protected Map<Goal, Double> getFD() {
		return this.featureDict;
	}

	@Override
	public String about() {
		return this.getClass().getSimpleName()+":"+this.name;
	}


	/** subroutine - populates an array of strings from a file **/
	private void loadArgs(String[] args,File file) {
		log.debug("Loading args file "+file.getName()+" in ConstantArgument...");
		ParsedFile parsed = new ParsedFile(file);
		for (String line : parsed)
			args[parsed.getLineNumber()] = line.trim();
		parsed.close();
	}
	private void loadArgs(TObjectIntMap<String> args,File file) {
		log.debug("Loading args file "+file.getName()+" in String...");
		ParsedFile parsed = new ParsedFile(file);
		for (String line : parsed)
			args.put((line.trim()),parsed.getLineNumber());
		parsed.close();
	}

	public static SparseGraphPlugin load(APROptions apr, File matrixDir) {
		return new SparseGraphPlugin(apr, matrixDir);
	}
	
}
