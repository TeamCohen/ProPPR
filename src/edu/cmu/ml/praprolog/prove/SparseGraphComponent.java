package edu.cmu.ml.praprolog.prove;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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
	public static final String FUNCTOR_INDEX="functorIndex.txt";

	protected Map<Goal, Double> featureDict;
	protected Map<String,SparseMatrixIndex> index;

	public SparseGraphComponent(String matrixDir) {
		// TODO: Get list of functors from index file
		ArrayList<String> functors = new ArrayList<String>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(new File(matrixDir,FUNCTOR_INDEX)));
			for (String line; (line=reader.readLine())!= null;) { functors.add(line.trim()); }
			reader.close();
		} catch (IOException e) {
			log.error("Problem reading functor index file "+FUNCTOR_INDEX+" in "+matrixDir,e);
			throw new RuntimeException(e);
		}
		// TODO: Initialize sparseMatrixIndex for each functor
		index=new HashMap<String,SparseMatrixIndex>();
		for(String functor: functors) {
			try {
				index.put(functor, new SparseMatrixIndex(matrixDir,functor));
			} catch (IOException e) {
				log.error("Problem reading sparse matrix "+functor+".* in "+matrixDir,e);
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
}
