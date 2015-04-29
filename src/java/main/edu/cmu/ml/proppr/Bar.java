package edu.cmu.ml.proppr;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.LineNumberReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.Runnable;
import java.lang.InterruptedException;
import java.lang.Exception;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import edu.cmu.ml.proppr.graph.ArrayLearningGraphBuilder;
import edu.cmu.ml.proppr.graph.LearningGraphBuilder;
import edu.cmu.ml.proppr.graph.LearningGraph;
import edu.cmu.ml.proppr.graph.RWOutlink;
import edu.cmu.ml.proppr.learn.tools.GroundedExampleParser;
import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.graph.LearningGraph.GraphFormatException;


public class Bar {
	private static final char TAB='\t',
		FEATURE_DELIM=':',
		FEATURE_WT_DELIM='@',
		EDGE_FEATURE_DELIM=',',
		NODE_DELIM=',';
	public PosNegRWExample parse(String line, LearningGraphBuilder builder) throws GraphFormatException {

		// first parse the query metadata
		String[] parts = new String[4];
		int last = 0,i=0;
		for (int next = last; i<parts.length; last=next+1,i++) {
			if (next == -1) 
				throw new GraphFormatException("Need 8 distinct tsv fields in the grounded example:"+line);
			next=line.indexOf(TAB,last);
			parts[i] = next<0?line.substring(last):line.substring(last,next);
		}

		TIntDoubleMap queryVec = new TIntDoubleHashMap();
		for(int u : parseNodes(parts[1])) queryVec.put(u, 1.0);

		int[] posList, negList;
		if (parts[2].length()>0) posList = parseNodes(parts[2]);
		else posList = new int[0];
		if (parts[3].length()>0) negList = parseNodes(parts[3]);
		else negList = new int[0];

		LearningGraph g = builder.deserialize(line.substring(last));
		return new PosNegRWExample(parts[0],g,queryVec,posList,negList);
	}
	private int[] parseNodes(String string) { // static: Foo F
		String[] nodeStrings = split(string,NODE_DELIM);
		int[] nodes = new int[nodeStrings.length];
		for (int i=0; i<nodeStrings.length; i++) {
			nodes[i] = Integer.parseInt(nodeStrings[i]);
		}
		return nodes;
	}

	private String[] split(String string, char delim) { // static: Foo F
		int nitems=0;
		for (int i=0; i<string.length(); i++) if (string.charAt(i) == delim) nitems++;
		String[] items = new String[nitems];
		int last=0;
		for (int next=last,i=0; i<items.length && next!=-1; last=next+1,i++) {
			next=string.indexOf(delim,last);
			items[i]=next<0?string.substring(last):string.substring(last,next);
		}
		return items;		
	}

}