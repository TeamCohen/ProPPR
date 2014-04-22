package edu.cmu.ml.praprolog.trove.learn;

import java.util.Iterator;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import edu.cmu.ml.praprolog.trove.graph.AnnotatedTroveGraph;
import edu.cmu.ml.praprolog.trove.graph.AnnotatedTroveGraph.GraphFormatException;
import edu.cmu.ml.praprolog.util.ParsedFile;

public class CookedExampleStreamer implements Iterable<PosNegRWExample>,
		Iterator<PosNegRWExample> {
	private static final Logger log = Logger.getLogger(CookedExampleStreamer.class);
	private ParsedFile file;
	public CookedExampleStreamer(String cookedExamplesFile) {
		this(new ParsedFile(cookedExamplesFile));
	}
	
	public CookedExampleStreamer(ParsedFile cookedExamplesFile) {
		log.info("Importing cooked examples from "+cookedExamplesFile.getFileName());
		this.file = cookedExamplesFile;
	}

	@Override
	public boolean hasNext() {
		return this.file.hasNext();
	}

	@Override
	public PosNegRWExample next() {
		String line = this.file.next();
		log.debug("Importing example from line "+file.getLineNumber());
		
		AnnotatedTroveGraph g = new AnnotatedTroveGraph();
		
		String[] parts = line.trim().split(edu.cmu.ml.praprolog.learn.CookedExampleStreamer.MAJOR_DELIM, 5);
		
		TreeMap<String, Double> queryVec = new TreeMap<String,Double>();
		for(String u : parts[1].split(edu.cmu.ml.praprolog.learn.CookedExampleStreamer.MINOR_DELIM)) queryVec.put(u, 1.0);

		String[] rawPosList, rawNegList;
		if (parts[2].length()>0) rawPosList = parts[2].split(edu.cmu.ml.praprolog.learn.CookedExampleStreamer.MINOR_DELIM);
		else rawPosList = new String[0];
		if (parts[3].length()>0) rawNegList = parts[3].split(edu.cmu.ml.praprolog.learn.CookedExampleStreamer.MINOR_DELIM);
		else rawNegList = new String[0];
		if (rawPosList.length + rawNegList.length == 0) {
			log.warn("no labeled solutions for example on line "+file.getAbsoluteLineNumber()+"; skipping");
			if (this.hasNext()) return next();
			else return null;
		}
		try {
			g = AnnotatedTroveGraph.fromStringParts(parts[4],g);
			return new PosNegRWExample(g,queryVec,rawPosList,rawNegList);
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
	public Iterator<PosNegRWExample> iterator() {
		return this;
	}

}
