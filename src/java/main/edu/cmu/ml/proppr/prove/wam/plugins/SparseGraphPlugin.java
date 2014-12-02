package edu.cmu.ml.proppr.prove.wam.plugins;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.prove.wam.Goal;
import edu.cmu.ml.proppr.util.APROptions;
import edu.cmu.ml.proppr.util.ParsedFile;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

public class SparseGraphPlugin extends GraphlikePlugin {
	private static final Logger log = Logger.getLogger(SparseGraphPlugin.class);	
	public static final String FILE_EXTENSION = ".sparse"; 
	public static final String INDEX_EXTENSION = ".i";
	public static final String MANIFEST="sparseIndex.txt";
	private static final long LOGUPDATE_MS = 10000;

	protected Map<Goal,Double> fd=new HashMap<Goal,Double>();
	protected String name;
	protected Map<Goal, Double> featureDict;
	protected Map<String,SparseMatrixIndex> index;
	protected Map<String,TObjectIntMap<String>> arg1s;
	protected Map<String,String[]> arg2s;
	
	public SparseGraphPlugin(APROptions apr, String matrixDir) {
		super(apr);
		
		log.info("Loading sparse graph component "+matrixDir);
		long start = System.currentTimeMillis();
		arg1s = new HashMap<String,TObjectIntMap<String>>();
		arg2s = new HashMap<String,String[]>();

		// TODO: Get list of functors from index file
		ArrayList<String> matrices = new ArrayList<String>();
		ParsedFile file = new ParsedFile(new File(matrixDir,MANIFEST));
		for (String line : file) { matrices.add(line); }
		file.close();
		// TODO: Initialize sparseMatrixIndex for each functor
		index=new HashMap<String,SparseMatrixIndex>();
		for(String matrix: matrices) {
			String[] parts = matrix.split("_");
			if (index.containsKey(parts[0])) {
				throw new IllegalArgumentException("Only one matrix allowed per functor. You've already used '"+parts[0]+"'");
			}
			if (!arg1s.containsKey(parts[1])) { // read arg1.i 
				arg1s.put(parts[1], new TObjectIntHashMap<String>());
				loadArgs(arg1s.get(parts[1]),new File(matrixDir,parts[1]+INDEX_EXTENSION));
			}
			if (!arg2s.containsKey(parts[2])) {
				ParsedFile rce = new ParsedFile(new File(matrixDir,matrix+".rce"));
				Iterator<String> rceit = rce.iterator(); rceit.next();
				String line = rceit.next();
				rce.close();
				if (line==null) throw new IllegalArgumentException("Bad format for "+matrix+".rce: line 2 must list #cols");
				int ncols = Integer.parseInt(line.trim());
				arg2s.put(parts[2], new String[ncols]);

				loadArgs(arg2s.get(parts[2]),new File(matrixDir,parts[2]+INDEX_EXTENSION));
			}
			try {
				index.put(parts[0], new SparseMatrixIndex(matrixDir,matrix,arg1s.get(parts[1]),arg2s.get(parts[2])));
			} catch (Exception e) {
				log.error("Problem reading sparse matrix "+matrix+".* in "+matrixDir,e);
				throw new RuntimeException(e);
			}
		}
		this.featureDict = new HashMap<Goal,Double>();
		this.featureDict.put(WamPlugin.pluginFeature(this, matrixDir),1.0);
		
		long del = System.currentTimeMillis() - start;
		if (del > LOGUPDATE_MS)
			log.info("Finished loading sparse graph component "+matrixDir+" ("+(del/1000.)+" sec)");
	}
	
	@Override
	protected boolean indexContains(String label) {
		return this.index.containsKey(label);
	}

	@Override
	protected List<String> indexGet(String label, String src) {
		List<String> ret = index.get(label).near(src);
		if (ret == null) return DEFAULT_DSTLIST;
		return ret;
	}

	@Override
	protected Collection<String> indexGet(String label) {
		if (!index.containsKey(label)) return DEFAULT_DSTLIST;
		return index.get(label).allSrc();
	}

	@Override
	protected void indexAdd(String label, String src, String dst) {
		throw new UnsupportedOperationException("Can't add to a sparse graph!");
	}

	@Override
	protected Map<Goal, Double> getFD() {
		return this.fd;
	}

	@Override
	public String about() {
		return this.getClass().getSimpleName()+":"+this.name;
	}


	/** subroutine - populates an array of strings from a file **/
	private void loadArgs(String[] args,File file) {
		log.info("Loading args file "+file.getName()+" in ConstantArgument...");
		ParsedFile parsed = new ParsedFile(file);
		for (String line : parsed)
			args[parsed.getLineNumber()] = line.trim();
		parsed.close();
	}
	private void loadArgs(TObjectIntMap<String> args,File file) {
		log.info("Loading args file "+file.getName()+" in String...");
		ParsedFile parsed = new ParsedFile(file);
		for (String line : parsed)
			args.put((line.trim()),parsed.getLineNumber());
		parsed.close();
	}

	public static SparseGraphPlugin load(APROptions apr, String string) {
		return new SparseGraphPlugin(apr, string);
	}
	
}
