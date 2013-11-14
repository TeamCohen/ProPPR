package edu.cmu.ml.praprolog.prove;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * GraphlikeComponent backed by a sparse matrix.
 * 
 * The constructor argument matrixDir contains a file functorIndex.txt which
 * lists each matrix functor, 1-per-line.
 * @author krivard
 *
 */
public class SparseGraphComponent extends GraphlikeComponent {
	private static final Logger log = Logger.getLogger(SparseGraphComponent.class);
	public static final String FILE_EXTENSION = "sparse"; 
	public static final String MANIFEST="sparseIndex.txt";

	protected Map<Goal, Double> featureDict;
	protected Map<String,SparseMatrixIndex> index;
	protected Map<String,HashMap<String,Integer>> arg1s;
	protected Map<String,ConstantArgument[]> arg2s;

	public SparseGraphComponent(String matrixDir) {
		arg1s = new HashMap<String,HashMap<String,Integer>>();
		arg2s = new HashMap<String,ConstantArgument[]>();

		// TODO: Get list of functors from index file
		ArrayList<String> matrices = new ArrayList<String>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(new File(matrixDir,MANIFEST)));
			for (String line; (line=reader.readLine())!= null;) { matrices.add(line.trim()); }
			reader.close();
		} catch (IOException e) {
			log.error("Problem reading index file "+MANIFEST+" in "+matrixDir,e);
			throw new RuntimeException(e);
		}
		// TODO: Initialize sparseMatrixIndex for each functor
		index=new HashMap<String,SparseMatrixIndex>();
		for(String matrix: matrices) {
			String[] parts = matrix.split("_");
			if (index.containsKey(parts[0])) {
				throw new IllegalArgumentException("Only one matrix allowed per functor. You've already used '"+parts[0]+"'");
			}
			if (!arg1s.containsKey(parts[1])) { // read arg1.i 
				arg1s.put(parts[1], new HashMap<String,Integer>());
				try {
					loadArgs(arg1s.get(parts[1]),new File(matrixDir,parts[1]+".i"));
				} catch (IOException e) {
					log.error("Problem reading arg1 file "+parts[1]+".i in "+matrixDir,e);
					throw new RuntimeException(e);
				}
			}
			if (!arg2s.containsKey(parts[2])) {
				String line;
				try {
					BufferedReader reader = new BufferedReader(new FileReader(new File(matrixDir,matrix+".rce")));
					reader.readLine(); line = reader.readLine();
					reader.close();
				} catch (IOException e) {
					log.error("Problem reading dimension file "+matrix+".rce in "+matrixDir,e);
					throw new RuntimeException(e);
				}
				if (line==null) throw new IllegalArgumentException("Bad format for "+matrix+".rce: line 2 must list #cols");
				int ncols = Integer.parseInt(line.trim());
				arg2s.put(parts[2], new ConstantArgument[ncols]);

				try {
					loadArgs(arg2s.get(parts[2]),new File(matrixDir,parts[2]+".i"));
				} catch (IOException e) {
					log.error("Problem reading arg2 file "+parts[2]+".i in "+matrixDir,e);
					throw new RuntimeException(e);
				}
			}
			try {
				index.put(parts[0], new SparseMatrixIndex(matrixDir,matrix,arg1s.get(parts[1]),arg2s.get(parts[2])));
			} catch (Exception e) {
				log.error("Problem reading sparse matrix "+matrix+".* in "+matrixDir,e);
				throw new RuntimeException(e);
			}
		}
		this.featureDict = new HashMap<Goal,Double>();
		this.featureDict.put(new Goal("id",matrixDir),1.0);
	}

	@Override
	protected void _indexAppend(String functor, Argument src, Argument dst) {
		throw new UnsupportedOperationException("Sparse matrix components are immutable");
	}

	@Override
	protected boolean _indexContains(String functor) {
		return this.index.containsKey(functor);
	}

	@Override
	protected List<Argument> _indexGet(String functor, Argument srcConst) {
		return index.get(functor).near(srcConst);
	}

	@Override
	protected int _indexGetDegree(String functor, Argument srcConst) {
		return index.get(functor).degree(srcConst);
	}

	@Override
	protected Map<Goal, Double> getFeatureDict() {
		return this.featureDict;
	}

	public static SparseGraphComponent load(String matrixDir) {
		return new SparseGraphComponent(matrixDir);
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
	
	public static void main(String[] args) {
		if (args.length < 1) {
			System.err.println("Usage:\n\tgraphname.sparse\n");
			System.exit(0);
		}
		
		long start = System.currentTimeMillis();
		SparseGraphComponent g = new SparseGraphComponent(args[0]);
		System.err.println("Loaded "+args[0]+" in "+(System.currentTimeMillis()-start)+" ms.");
		
		
		for(String cmd; !"quit".equals(cmd = System.console().readLine("> "));) {
			if (cmd.startsWith("outlinks")) {
				String[] parts = cmd.split(" ");
				
				Goal from = Goal.decompile(parts[1]);
				LogicProgramState lpState = new ProPPRLogicProgramState(from);
				for (Outlink o : g.outlinks(lpState)) {
					System.out.println(o.getState());
				}
			} else {
				System.out.println("Unknown command '"+cmd+"'. Try 'outlinks functor,arg1,-1' or 'quit'");
			}
		}
		
	}
}
