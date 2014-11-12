package edu.cmu.ml.proppr.prove.v1;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.util.ParsedFile;

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
	public static final String FILE_EXTENSION = ".sparse"; 
	public static final String INDEX_EXTENSION = ".i";
	public static final String MANIFEST="sparseIndex.txt";
	private static final long LOGUPDATE_MS = 10000;

	protected Map<Goal, Double> featureDict;
	protected Map<String,SparseMatrixIndex> index;
	protected Map<String,HashMap<String,Integer>> arg1s;
	protected Map<String,ConstantArgument[]> arg2s;

	public SparseGraphComponent(String matrixDir) {
		log.info("Loading sparse graph component "+matrixDir);
		long start = System.currentTimeMillis();
		arg1s = new HashMap<String,HashMap<String,Integer>>();
		arg2s = new HashMap<String,ConstantArgument[]>();

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
				arg1s.put(parts[1], new HashMap<String,Integer>());
				loadArgs(arg1s.get(parts[1]),new File(matrixDir,parts[1]+INDEX_EXTENSION));
			}
			if (!arg2s.containsKey(parts[2])) {
				ParsedFile rce = new ParsedFile(new File(matrixDir,matrix+".rce"));
				Iterator<String> rceit = rce.iterator(); rceit.next();
				String line = rceit.next();
				rce.close();
				if (line==null) throw new IllegalArgumentException("Bad format for "+matrix+".rce: line 2 must list #cols");
				int ncols = Integer.parseInt(line.trim());
				arg2s.put(parts[2], new ConstantArgument[ncols]);

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
		this.featureDict.put(new Goal("id",matrixDir),1.0);
		
		long del = System.currentTimeMillis() - start;
		if (del > LOGUPDATE_MS)
			log.info("Finished loading sparse graph component "+matrixDir+" ("+(del/1000.)+" sec)");
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
		List<Argument> ret = index.get(functor).near(srcConst);
		if (ret == null) log.debug("No argument "+srcConst+" in this sparsegraph for functor "+functor);
		return ret;
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
	private void loadArgs(ConstantArgument[] args,File file) {
		log.info("Loading args file "+file.getName()+" in ConstantArgument...");
		ParsedFile parsed = new ParsedFile(file);
		for (String line : parsed)
			args[parsed.getLineNumber()] = new ConstantArgument(line);
		parsed.close();
	}
	private void loadArgs(HashMap<String,Integer> args,File file) {
		log.info("Loading args file "+file.getName()+" in String...");
		ParsedFile parsed = new ParsedFile(file);
		for (String line : parsed)
			args.put((line.trim()),parsed.getLineNumber());
		parsed.close();
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
