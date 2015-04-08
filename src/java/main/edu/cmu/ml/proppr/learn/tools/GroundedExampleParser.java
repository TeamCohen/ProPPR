package edu.cmu.ml.proppr.learn.tools;

import java.util.Iterator;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.graph.LearningGraph;
import edu.cmu.ml.proppr.graph.LearningGraph.GraphFormatException;
import edu.cmu.ml.proppr.graph.LearningGraphBuilder;
import edu.cmu.ml.proppr.util.FileBackedIterable;
import edu.cmu.ml.proppr.util.ParsedFile;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;

public class GroundedExampleParser implements Iterable<PosNegRWExample>, Iterator<PosNegRWExample>, FileBackedIterable {
	private static final Logger log = Logger.getLogger(GroundedExampleParser.class);
	public static final String MAJOR_DELIM="\t";
	public static final String MINOR_DELIM=",";
	private ParsedFile file;
	private LearningGraphBuilder builder;
 	public GroundedExampleParser(String cookedExamplesFile, LearningGraphBuilder builder) {
 		this(new ParsedFile(cookedExamplesFile), builder);
 	}
	public GroundedExampleParser(ParsedFile cookedExamplesFile, LearningGraphBuilder builder) {
		log.info("Importing cooked examples from "+cookedExamplesFile.getFileName());
		this.file = cookedExamplesFile;
		this.builder = builder;
	}

	private static int[] stringToInt(String[] raw) {
		int[] ret = new int[raw.length];
		for (int i=0; i<raw.length; i++) { ret[i] = Integer.parseInt(raw[i]); }
		return ret;
	}
	public static PosNegRWExample parse(String line, LearningGraphBuilder builder) throws GraphFormatException {
		String[] parts = line.trim().split(MAJOR_DELIM,5);

		TIntDoubleMap queryVec = new TIntDoubleHashMap();
		for(String u : parts[1].split(MINOR_DELIM)) queryVec.put(Integer.parseInt(u), 1.0);

		int[] posList, negList;
		if (parts[2].length()>0) posList = stringToInt(parts[2].split(MINOR_DELIM));
		else posList = new int[0];
		if (parts[3].length()>0) negList = stringToInt(parts[3].split(MINOR_DELIM));
		else negList = new int[0];

		LearningGraph g = builder.deserialize(parts[4]);
		return new PosNegRWExample(parts[0],g,queryVec,posList,negList);
	}

	@Override
	public boolean hasNext() {
		return this.file.hasNext();
	}
	

	@Override
	public PosNegRWExample next() {
		String line = this.file.next();
		if (log.isDebugEnabled()) log.debug("Importing example from line "+file.getLineNumber());
		try {
			PosNegRWExample ret = parse(line, builder);
			if (ret == null) {
				log.warn("no labeled solutions for example on line "+file.getAbsoluteLineNumber()+"; skipping");
				if (this.hasNext()) return next();
				else return null;
			}
		} catch (GraphFormatException e) {
			file.parseError("["+e.getMessage()+"]");
			if (this.hasNext()) return next();
			else return null;
		}
		return null;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("Can't remove from a grounded example stream");
	}

	@Override
	public Iterator<PosNegRWExample> iterator() {
		return this;
	}
	
	
	@Override
	public void wrap() {
		if (this.hasNext()) return;
		this.file.reset();
	}
}
