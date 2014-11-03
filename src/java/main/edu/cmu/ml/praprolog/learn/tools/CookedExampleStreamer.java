package edu.cmu.ml.praprolog.learn.tools;

import java.util.Iterator;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import edu.cmu.ml.praprolog.examples.PosNegRWExample;
import edu.cmu.ml.praprolog.util.FileBackedIterable;
import edu.cmu.ml.praprolog.util.ParsedFile;

public class CookedExampleStreamer<T> implements Iterable<PosNegRWExample<T>>, Iterator<PosNegRWExample<T>>, FileBackedIterable {
	private static final Logger log = Logger.getLogger(CookedExampleStreamer.class);
	public static final String MAJOR_DELIM="\t";
	public static final String MINOR_DELIM=",";
	private ParsedFile file;
	private AnnotatedGraphFactory<T> factory;
 	public CookedExampleStreamer(String cookedExamplesFile, AnnotatedGraphFactory<T> factory) {
 		this(new ParsedFile(cookedExamplesFile), factory);
 	}
	public CookedExampleStreamer(ParsedFile cookedExamplesFile, AnnotatedGraphFactory<T> factory) {
		log.info("Importing cooked examples from "+cookedExamplesFile.getFileName());
		this.file = cookedExamplesFile;
		this.factory = factory;		
	}

	@Override
	public boolean hasNext() {
		return this.file.hasNext();
	}

	@Override
	public PosNegRWExample<T> next() {
		String line = this.file.next();
		log.debug("Imporing example from line "+file.getLineNumber());
		
		AnnotatedGraph<T> g = factory.create();

		String[] parts = line.trim().split(MAJOR_DELIM,5);

		TreeMap<T, Double> queryVec = new TreeMap<T,Double>();
		for(String u : parts[1].split(MINOR_DELIM)) queryVec.put(g.keyToId(u), 1.0);

		String[] rawPosList, rawNegList;
		if (parts[2].length()>0) rawPosList = parts[2].split(MINOR_DELIM);
		else rawPosList = new String[0];
		if (parts[3].length()>0) rawNegList = parts[3].split(MINOR_DELIM);
		else rawNegList = new String[0];
		if (rawPosList.length + rawNegList.length == 0) {
			log.warn("no labeled solutions for example on line "+file.getAbsoluteLineNumber()+"; skipping");
			if (this.hasNext()) return next();
			else return null;
		}
		T[] posList = g.keyToId(rawPosList);
		T[] negList = g.keyToId(rawNegList);
		try {
			g = AnnotatedGraph.fromStringParts(parts[4],g);
			return new PosNegRWExample<T>(g,queryVec,posList,negList);
		} catch (GraphFormatException e) {
			file.parseError("["+e.getMessage()+"]");
			if (this.hasNext()) return next();
			else return null;
		}
	}

	@Override
	public void remove() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Iterator<PosNegRWExample<T>> iterator() {
		return this;
	}
	
	
	@Override
	public void wrap() {
		if (this.hasNext()) return;
		this.file.reset();
	}

}
