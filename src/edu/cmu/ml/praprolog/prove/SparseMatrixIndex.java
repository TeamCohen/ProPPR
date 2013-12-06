package edu.cmu.ml.praprolog.prove;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;


/**
 * Reads in 5 files to produce a CRS-type sparse matrix:
 *  - [functor]_[arg1type]_[arg2type].rce: [int]\n[int]\n[int] for the matrix dimensions: rows, columns, entries
 *  - [functor]_[arg1type]_[arg2type].colIndes: [int]-per-line, in row order. The kth line stores the column index (j) of the kth value.
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
	private static final int LOGUPDATE_MS=5000;
	// counts of each
	int rows, cols, entries;
	// names of rows, cols
	ConstantArgument[] arg2;
	HashMap<String,Integer> arg1;
	int[] rowOffsets, colIndices;
	float[] values;

	public SparseMatrixIndex() {}
	public SparseMatrixIndex(String dir, String functor_arg1type_arg2type, final HashMap<String,Integer> arg1, final ConstantArgument[] arg2) throws IOException {
		this.arg1=arg1;
		this.arg2=arg2;
		this.load(dir,functor_arg1type_arg2type);
	}
	public void load(String dir, String functor_arg1type_arg2type) throws IOException {
		log.info("Loading matrix "+functor_arg1type_arg2type+" from "+dir+"...");
		long start0 = System.currentTimeMillis();
		/* Read the number of rows, columns, and entries - entry is a triple (i,j,m[i,j])
		 * except I don't store m[i,j] since it's always 1.0 for me. */
		LineNumberReader reader = new LineNumberReader(new FileReader(new File(dir,functor_arg1type_arg2type+".rce")));
		{
			String line=reader.readLine(); if (line==null) throw new IllegalArgumentException("Bad format for "+functor_arg1type_arg2type+".rce: line 1 must list #rows");
			this.rows = Integer.parseInt(line.trim());
			line=reader.readLine(); if (line==null) throw new IllegalArgumentException("Bad format for "+functor_arg1type_arg2type+".rce: line 2 must list #cols");
			this.cols = Integer.parseInt(line.trim());
			line=reader.readLine(); if (line==null) throw new IllegalArgumentException("Bad format for "+functor_arg1type_arg2type+".rce: line 3 must list #entries");
			this.entries = Integer.parseInt(line.trim());
			reader.close();
		}
		
		/* Data is stored like this: colIndices[] is one long
		 * array, and values is a parallel array.  rowsOffsets is another array so that 
		 * rowOffsets[i] is where the column indices for row i start. Thus
		 *
		 * (for k=rowOffsets[i]; k<rowOffsets[i+1]; k++) {
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
		reader = new LineNumberReader(new FileReader(new File(dir,functor_arg1type_arg2type+".rowOffset")));
		int lineNum = 0;
		for(String line; (line=reader.readLine()) != null; lineNum++) {
			rowsOffsets.add(Integer.parseInt(line.trim()));
			if (log.isInfoEnabled()) {
				long now = System.currentTimeMillis();
				if ( (now-last) > LOGUPDATE_MS) {
					log.info("rowOffset: "+reader.getLineNumber()+" lines ("+(reader.getLineNumber()/(now-start))+" klps)");
					last = now;
				}
			}
		}
		reader.close();
		
		start = System.currentTimeMillis(); last=start;
		reader = new LineNumberReader(new FileReader(new File(dir,functor_arg1type_arg2type+".colIndex")));
		lineNum = 0;
		for(String line; (line=reader.readLine()) != null; lineNum++) {
			colIndices[lineNum] = Integer.parseInt(line.trim());
			values[lineNum] = (float) 1.0;
			if (log.isInfoEnabled()) {
				long now = System.currentTimeMillis();
				if ( (now-last) > LOGUPDATE_MS) {
					log.info("colIndex: "+reader.getLineNumber()+" lines ("+(reader.getLineNumber()/(now-start))+" klps)");
					last = now;
				}
			}
		}
		reader.close();
		this.rowOffsets = new int[rowsOffsets.size()+1];
		for (int i=0; i<rowsOffsets.size(); i++) {
			rowOffsets[i] = rowsOffsets.get(i);
		}
		rowOffsets[rowsOffsets.size()] = entries;
		

		long del = System.currentTimeMillis() - start0;
		if (del > LOGUPDATE_MS)
			log.info("Finished loading sparse graph matrix "+functor_arg1type_arg2type+" ("+(del/1000.)+" sec)");
	}


	/** Given string key='a' such that arg1[i] = key, find all strings
	 * 'b' such that m[i,j] != 0 and arg2[j]==b.
	 **/
	public List<Argument> near(Argument key) {
		Integer r = this.arg1.get(key.getName());
		if (r==null || r >= rows) {
			return null;
		}
		else {
			ArrayList<Argument> ret = new ArrayList<Argument>();
			for (int k=this.rowOffsets[r]; k<this.rowOffsets[r+1]; k++) {
				ret.add(this.arg2[this.colIndices[k]]);
			}
			return ret;
		}
	}
	/**
	 * NB: log-n. If greater speed is needed, add a bloom filter of arg1 values.
	 * 
	 * (uh -- this is never called)
	 * @param key
	 * @return
	 */
	public boolean contains(Argument key) {
		return this.arg1.get(key.getName()) != null;
	}
	/**
	 * NB: Only works because all our nonzero weights are 1.0.
	 * @param key
	 * @return
	 */
	public int degree(Argument key) {
		Integer r = this.arg1.get(key.getName());
		if (r==null || r >= rows) {
			return 0;
		}
		else {
			return this.rowOffsets[r+1]-this.rowOffsets[r];
		}
	}
}
