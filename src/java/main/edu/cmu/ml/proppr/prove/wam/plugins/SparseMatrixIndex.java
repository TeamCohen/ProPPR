package edu.cmu.ml.proppr.prove.wam.plugins;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.util.ParsedFile;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.procedure.TObjectIntProcedure;


/**
 * Reads in 5 files to produce a CRS-type sparse matrix:
 *  - [functor]_[arg1type]_[arg2type].rce: [int]\n[int]\n[int] for the matrix dimensions: rows, columns, entries
 *  - [functor]_[arg1type]_[arg2type].colIndex: [int]-per-line, in row order. The kth line stores the column index (j) of the kth value. Optional kth value included after a tab.
 *  - [functor]_[arg1type]_[arg2type].rowOffset: [int]-per-line. The ith line stores the line number (k) in colIndices where row i begins.
 *  - [arg1type].i: Sorted in lex order. The ith line stores the name of row i.
 *  - [arg2type].i: The jth line stores the name of column j.
 * All nonzero matrix values are set to 1.0.
 * 
 * The lengths of files are thus constrained:
 *  - *.rowOffsets has #rows lines
 *  - *.colIndex has #entries lines
 * @author wcohen,krivard
 */
public class SparseMatrixIndex {
	private static final Logger log = Logger.getLogger(SparseMatrixIndex.class);
	private static final String WEIGHT_DELIMITER = "\t";
	private static final int LOGUPDATE_MS=5000;
	String name;
	/** counts */
	int rows, cols, entries;
	/** names */
	String[] arg2;
	TObjectIntMap<String> arg1;
	/** indexes */
	int[] rowOffsets, colIndices;
	float[] values;

	public SparseMatrixIndex() {}
	public SparseMatrixIndex(File matrixDir, String functor_arg1type_arg2type, final TObjectIntMap<String> arg1, final String[] arg2) throws IOException {
		this.arg1=arg1;
		this.arg2=arg2;
		this.load(matrixDir,functor_arg1type_arg2type);
	}
	public void load(File dir, String functor_arg1type_arg2type) throws IOException {
		log.info("Loading matrix "+functor_arg1type_arg2type+" from "+dir.getName()+"...");
		this.name = dir+":"+functor_arg1type_arg2type;
		long start0 = System.currentTimeMillis();
		/* Read the number of rows, columns, and entries - entry is a triple (i,j,m[i,j]) */
		ParsedFile file = new ParsedFile(new File(dir,functor_arg1type_arg2type+".rce"));
		{
			Iterator<String> it = file.iterator();
			String line=it.next(); if (line==null) throw new IllegalArgumentException("Bad format for "+functor_arg1type_arg2type+".rce: line 1 must list #rows");
			this.rows = Integer.parseInt(line.trim());
			line=it.next(); if (line==null) throw new IllegalArgumentException("Bad format for "+functor_arg1type_arg2type+".rce: line 2 must list #cols");
			this.cols = Integer.parseInt(line.trim());
			line=it.next(); if (line==null) throw new IllegalArgumentException("Bad format for "+functor_arg1type_arg2type+".rce: line 3 must list #entries");
			this.entries = Integer.parseInt(line.trim());
			file.close();
		}
		
		/* Data is stored like this: colIndices[] is one long
		 * array, and values is a parallel array.  rowsOffsets is another array so that 
		 * rowOffsets[i] is where the column indices for row i start. Thus
		 *
		 * for (k=rowOffsets[i]; k<rowOffsets[i+1]; k++) {
		 *   j = colIndices[k];
		 *   m_ij = values[k];
		 *   // this would retrieve i,j and the corresponding value in the sparse matrix m[i,j]
		 *   doSomethingWith(i,j,m_ij);
		 * }
		 *
		 */
		ArrayList<Integer> rowsOffsets = new ArrayList<Integer>();
		this.colIndices = new int[entries];
		this.values = new float[entries];

		long start = System.currentTimeMillis(), last=start;
		file = new ParsedFile(new File(dir,functor_arg1type_arg2type+".rowOffset"));
		for(String line : file) {
			rowsOffsets.add(Integer.parseInt(line));
			if (log.isInfoEnabled()) {
				long now = System.currentTimeMillis();
				if ( (now-last) > LOGUPDATE_MS) {
					log.info("rowOffset: "+file.getLineNumber()+" lines ("+(file.getLineNumber()/(now-start))+" klps)");
					last = now;
				}
			}
		}
		file.close();
		
		start = System.currentTimeMillis(); last=start;
		file = new ParsedFile(new File(dir,functor_arg1type_arg2type+".colIndex"));
		for(String line : file) {
			int ln = file.getLineNumber();
			String[] parts = line.split(WEIGHT_DELIMITER);
			colIndices[ln] = Integer.parseInt(parts[0]);
			values[ln] = (float) (parts.length>1?Float.parseFloat(parts[1]):1.0);
			if (colIndices[ln] >= arg2.length) {
				throw new IllegalArgumentException("Malformed sparsegraph! For index "+this.name+", colIndices["+ln+"]="+colIndices[ln]+"; arg2.length is only "+arg2.length);
			}
			if (log.isInfoEnabled()) {
				long now = System.currentTimeMillis();
				if ( (now-last) > LOGUPDATE_MS) {
					log.info("colIndex: "+file.getLineNumber()+" lines ("+(file.getLineNumber()/(now-start))+" klps)");
					last = now;
				}
			}
		}
		file.close();
		this.rowOffsets = new int[rowsOffsets.size()+1];
		for (int i=0; i<rowsOffsets.size(); i++) {
			rowOffsets[i] = rowsOffsets.get(i);
		}
		rowOffsets[rowsOffsets.size()] = entries;
		
		long del = System.currentTimeMillis() - start0;
		if (del > LOGUPDATE_MS)
			log.info("Finished loading sparse graph matrix "+functor_arg1type_arg2type+" ("+(del/1000.)+" sec)");
	}

	/**
	 * NB: May be slow
	 * @return a list of all source nodes in this graph
	 */
	public List<String> allSrc() {
		final ArrayList<String> ret = new ArrayList<String>();
		this.arg1.forEachEntry(new TObjectIntProcedure<String>() {
			@Override
			public boolean execute(String a, int r) {
				if ((r+1 < rowOffsets.length) && (rowOffsets[r+1]-rowOffsets[r]) > 0) ret.add(a);
				return true;
			}
		});
		return ret;
	}

	/** Given string key='a' such that arg1[i] = key, find all strings
	 * 'b' such that m[i,j] != 0 and arg2[j]==b.
	 **/
	public TObjectDoubleMap<String> near(String key) {
		if (!this.arg1.containsKey(key)) return null;
		int r = this.arg1.get(key);
		if (r >= rows) return null;
		TObjectDoubleMap<String> ret = new TObjectDoubleHashMap<String>(this.rowOffsets[r+1]-this.rowOffsets[r]);
		for (int k=this.rowOffsets[r]; k<this.rowOffsets[r+1]; k++) {
			if (this.arg2[this.colIndices[k]] == null) 
				throw new IllegalStateException("Found null argument in index "+this.name+" arg2[colIndices["+k+"]="+colIndices[k]+"] (arg2.length="+arg2.length+")");
			ret.put(this.arg2[this.colIndices[k]], this.values[k]);
		}
		return ret;
	}

	public boolean contains(String key) {
		return this.arg1.containsKey(key);
	}
	public int degree(String key) {
		if (!this.arg1.containsKey(key)) return 0;
		int r = this.arg1.get(key);
		if (r >= rows) return 0;
		return this.rowOffsets[r+1]-this.rowOffsets[r];
	}
}
