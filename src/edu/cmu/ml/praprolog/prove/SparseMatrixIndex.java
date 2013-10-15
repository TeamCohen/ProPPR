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

import org.la4j.matrix.sparse.CRSMatrix;
import org.la4j.vector.functor.VectorProcedure;

/**
 * Reads in 5 files to produce a CRS-type sparse matrix:
 *  - *.rce: [int]\t[int]\t[int] for the matrix dimensions: rows, columns, entries
 *  - *.arg1: Sorted in lex order. The ith line stores the name of row i.
 *  - *.arg2: The jth line stores the name of column j.
 *  - *.colIndices: [int]-per-line, in row order. The kth line stores the column index (j) of the kth value.
 *  - *.rowOffsets: [int]-per-line. The ith line stores the line number (k) in colIndices where row i begins.
 * All nonzero matrix values are set to 1.0.
 * 
 * The lengths of files are thus constrained:
 *  - *.arg1 has #rows lines
 *  - *.arg2 has #columns lines
 *  - *.rowOffsets has #rows lines
 *  - *.colIndex has #entries lines
 * @author wcohen,krivard
 */
public class SparseMatrixIndex {
	// counts of each
	int rows, cols, entries;
	// names of rows, cols
//	ConstantArgument[] arg1, arg2;
	ConstantArgument[] arg2;
	HashMap<String,Integer> arg1;
	CRSMatrix mat;

	public SparseMatrixIndex() {}
	public SparseMatrixIndex(String dir, String prefix) throws IOException {
		this.load(dir,prefix);
	}
	public void load(String dir, String prefix) throws IOException {
		/* Read the number of rows, columns, and entries - entry is a triple (i,j,m[i,j])
		 * except I don't store m[i,j] since it's always 1.0 for me. */
		BufferedReader reader = new LineNumberReader(new FileReader(new File(dir,prefix+".rce")));
		for(String line; (line=reader.readLine()) != null; ){
			String[] parts = line.trim().split("\t");
			this.rows = Integer.parseInt(parts[0]);
			this.cols = Integer.parseInt(parts[1]);
			this.entries = Integer.parseInt(parts[2]);
//			System.out.println(dirName + " sizes " + this.rows + " " + this.cols + " " + this.entries);
			break;
		}
		reader.close();
		this.arg1 = new HashMap<String,Integer>();    /* For the strings with indices i=0,1,.... */
		this.arg2 = new ConstantArgument[cols];    /* and with indices j=0,1,.... */
		loadArgs(this.arg1,new File(dir,prefix+".arg1"));
		loadArgs(this.arg2,new File(dir,prefix+".arg2"));
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
		int[] colIndices = new int[entries];
		double[] values = new double[entries];

		reader = new LineNumberReader(new FileReader(new File(dir,prefix+".rowOffsets")));
		int lineNum = 0;
		for(String line; (line=reader.readLine()) != null; lineNum++) {
			rowsOffsets.add(Integer.parseInt(line.trim()));
		}
		reader.close();
		reader = new LineNumberReader(new FileReader(new File(dir,prefix+".colIndices")));
		lineNum = 0;
		for(String line; (line=reader.readLine()) != null; lineNum++) {
			colIndices[lineNum] = Integer.parseInt(line.trim());
			values[lineNum] = 1.0;
		}
		reader.close();
		int[] rowOffsetsArr = new int[rowsOffsets.size()+1];
		for (int i=0; i<rowsOffsets.size(); i++) {
			rowOffsetsArr[i] = rowsOffsets.get(i);
		}
		rowOffsetsArr[rowsOffsets.size()] = entries;
		/* Create the data sparse matrix */
		this.mat = new CRSMatrix(this.rows, this.cols, entries, values, colIndices, rowOffsetsArr);
	}
	/** subroutine - populates an array of strings from a file **/
	private void loadArgs(ConstantArgument[] args,File file) throws IOException {
		BufferedReader reader = new LineNumberReader(new FileReader(file));
		int lineNum = 0;
		for(String line; (line=reader.readLine()) != null; lineNum++) {
			args[lineNum] = new ConstantArgument(line.trim());
		}
		reader.close();
	}
	private void loadArgs(HashMap<String,Integer> args,File file) throws IOException {
		BufferedReader reader = new LineNumberReader(new FileReader(file));
		int lineNum = 0;
		for(String line; (line=reader.readLine()) != null; lineNum++) {
			args.put((line.trim()),lineNum);
		}
		reader.close();
	}

	/** Given string key='a' such that arg1[i] = key, find all strings
	 * 'b' such that m[i,j] != 0 and arg2[j]==b.
	 **/
	public List<Argument> near(Argument key) {
		Integer r = this.arg1.get(key.getName());
		if (r==null) {
			return null;
		}
		else {
			final ArrayList<Argument> accumForClosure = new ArrayList<Argument>();
			final ConstantArgument[] arg2PtrForClosure = this.arg2;
			this.mat.getRow(r).eachNonZero(new VectorProcedure() {
				public void apply(int c,double val) {
					accumForClosure.add(arg2PtrForClosure[c]);
				}
			});
			return accumForClosure;
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
		Integer r = this.arg1.get(key.getName());//Arrays.binarySearch(this.arg1,key);
		if (r!=null) {//(r<0) {
			return 0;
		}
		else {
			return (int) Math.floor(this.mat.getRow(r).sum());
		}
	}
}
