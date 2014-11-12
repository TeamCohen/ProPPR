package edu.cmu.ml.proppr.learn.tools;

import java.util.Iterator;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.graph.LearningGraph;
import edu.cmu.ml.proppr.graph.LearningGraph.GraphFormatException;
import edu.cmu.ml.proppr.graph.LearningGraphBuilder;
import edu.cmu.ml.proppr.graph.SimpleLearningGraph;
import edu.cmu.ml.proppr.util.FileBackedIterable;
import edu.cmu.ml.proppr.util.ParsedFile;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;

public class GroundedExampleStreamer<F> implements Iterable<PosNegRWExample<F>>, Iterator<PosNegRWExample<F>>, FileBackedIterable {
	private static final Logger log = Logger.getLogger(GroundedExampleStreamer.class);
	public static final String MAJOR_DELIM="\t";
	public static final String MINOR_DELIM=",";
	private ParsedFile file;
	private LearningGraphBuilder<F> builder;
 	public GroundedExampleStreamer(String cookedExamplesFile, LearningGraphBuilder<F> builder) {
 		this(new ParsedFile(cookedExamplesFile), builder);
 	}
	public GroundedExampleStreamer(ParsedFile cookedExamplesFile, LearningGraphBuilder<F> builder) {
		log.info("Importing cooked examples from "+cookedExamplesFile.getFileName());
		this.file = cookedExamplesFile;
		this.builder = builder;
	}

	@Override
	public boolean hasNext() {
		return this.file.hasNext();
	}
	
	private int[] stringToInt(String[] raw) {
		int[] ret = new int[raw.length];
		for (int i=0; i<raw.length; i++) { ret[i] = Integer.parseInt(raw[i]); }
		return ret;
	}

	@Override
	public PosNegRWExample<F> next() {
		String line = this.file.next();
		log.debug("Imporing example from line "+file.getLineNumber());

		String[] parts = line.trim().split(MAJOR_DELIM,5);

		TIntDoubleMap queryVec = new TIntDoubleHashMap();
		for(String u : parts[1].split(MINOR_DELIM)) queryVec.put(Integer.parseInt(u), 1.0);

		int[] posList, negList;
		if (parts[2].length()>0) posList = stringToInt(parts[2].split(MINOR_DELIM));
		else posList = new int[0];
		if (parts[3].length()>0) negList = stringToInt(parts[3].split(MINOR_DELIM));
		else negList = new int[0];
		if (posList.length + negList.length == 0) {
			log.warn("no labeled solutions for example on line "+file.getAbsoluteLineNumber()+"; skipping");
			if (this.hasNext()) return next();
			else return null;
		}
		try {
			LearningGraph<F> g = builder.deserialize(parts[4]);
			return new PosNegRWExample<F>(g,queryVec,posList,negList);
		} catch (GraphFormatException e) {
			file.parseError("["+e.getMessage()+"]");
			if (this.hasNext()) return next();
			else return null;
		}
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("Can't remove from a grounded example stream");
	}

	@Override
	public Iterator<PosNegRWExample<F>> iterator() {
		return this;
	}
	
	
	@Override
	public void wrap() {
		if (this.hasNext()) return;
		this.file.reset();
	}

}
