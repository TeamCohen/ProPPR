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

public class Foo implements Runnable {
	private static final char TAB='\t',
		FEATURE_DELIM=':',
		FEATURE_WT_DELIM='@',
		EDGE_FEATURE_DELIM=',',
		NODE_DELIM=',';
	private static final String 
		SRC_DST_DELIM="->";
	
	String line;
	int id;
	//Bar bar; // Foo L
	//public Foo(String line, int id, Bar bar) {
	public Foo(String line, int id) {
		this.line = line;
		this.id = id;
		//this.bar = bar;
	}
	public void run() {
		try {
			System.out.println(System.currentTimeMillis()+" Job start "+this.id);
			//this.doit();
			//GroundedExampleParser.parse(this.line, new ArrayLearningGraphBuilder()); // Foo Q
			//Foo.parse(this.line, new ArrayLearningGraphBuilder()); // Foo F
			//this.bar.parse(this.line, new ArrayLearningGraphBuilder()); // Foo L
			new Bar().parse(this.line, new ArrayLearningGraphBuilder()); // Foo Li2
			System.out.println(System.currentTimeMillis()+" Job done "+this.id);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static PosNegRWExample parse(String line, LearningGraphBuilder builder) throws GraphFormatException {
		//String[] parts = line.trim().split(MAJOR_DELIM,5);
		// first parse the query metadata
		String[] parts = new String[4];//LearningGraphBuilder.split(line,TAB,4);
		int last = 0,i=0;
		for (int next = last; i<parts.length; last=next+1,i++) {
			if (next == -1) 
				throw new GraphFormatException("Need 8 distinct tsv fields in the grounded example:"+line);
			next=line.indexOf(TAB,last);
			parts[i] = next<0?line.substring(last):line.substring(last,next);
		}

		TIntDoubleMap queryVec = new TIntDoubleHashMap();
		//for(String u : parts[1].split(MINOR_DELIM)) queryVec.put(Integer.parseInt(u), 1.0);
		for(int u : parseNodes(parts[1])) queryVec.put(u, 1.0);

		int[] posList, negList;
		if (parts[2].length()>0) posList = parseNodes(parts[2]); //stringToInt(parts[2].split(MINOR_DELIM));
		else posList = new int[0];
		if (parts[3].length()>0) negList = parseNodes(parts[3]);//stringToInt(parts[3].split(MINOR_DELIM));
		else negList = new int[0];

		LearningGraph g = builder.deserialize(line.substring(last));
		return new PosNegRWExample(parts[0],g,queryVec,posList,negList);
	}
 
	public void doit() {
		// first parse the query and graph metadata
		String[] parts = new String[7];
		int last = 0,i=0;
		for (int next = last; i<parts.length && next!=-1; last=next+1,i++) {
			next=this.line.indexOf(TAB,last);
			parts[i] = next<0?this.line.substring(last):this.line.substring(last,next);
		}
		// query metadata is
		// query
		// query nodes
		// + labels
		// - labels
		TIntDoubleMap queryVec = new TIntDoubleHashMap(); // Foo 6
		for (int q : parseNodes(parts[1])) queryVec.put(q,1.0); // Foo 6
		
		int[] posList = parseNodes(parts[2]); // Foo 6
		int[] negList = parseNodes(parts[3]); // Foo 6

		// graph metadata is
		// #nodes
		// #edges

		int nodeSize = Integer.parseInt(parts[4]);
		int edgeSize = Integer.parseInt(parts[5]);
		ArrayLearningGraphBuilder b = new ArrayLearningGraphBuilder(); // Foo 6
		LearningGraph g = b.create(); // Foo 6
		b.index(1); // Foo 6
		b.setGraphSize(g,nodeSize,edgeSize); // Foo 6

		// now parse the feature library
		String[] features = split(parts[6],FEATURE_DELIM);
		
		// now parse out each edge
		int[] nodes = {-1,-1};
		for (int next=last; next!=-1; last=next+1) {
			next = this.line.indexOf(TAB,last);
			
			int srcDest = this.line.indexOf(SRC_DST_DELIM,last);
			int edgeDelim = this.line.indexOf(FEATURE_DELIM,srcDest);
			String[] edgeFeatures = split(next<0?this.line.substring(edgeDelim):this.line.substring(edgeDelim,next),EDGE_FEATURE_DELIM);
			nodes[0] = Integer.parseInt(this.line.substring(last,srcDest));
			nodes[1] = Integer.parseInt(this.line.substring(srcDest+2,edgeDelim));
			//TObjectDoubleHashMap<String> fd = new TObjectDoubleHashMap<String>(); // Foo 6
			HashMap<String,Double> fd = new HashMap<String,Double>(); // Foo 6.1
			for (String f : edgeFeatures) {
				int wtDelim = f.indexOf(FEATURE_WT_DELIM);
				int featureId = Integer.parseInt(wtDelim<0?f:f.substring(0,wtDelim));
				double featureWt = wtDelim<0?1.0:Double.parseDouble(f.substring(wtDelim+1));
				fd.put(features[featureId],featureWt); // Foo 6
			}
			b.addOutlink(g,nodes[0],new RWOutlink(fd,nodes[1])); // Foo 6.2
		} // Foo 5
		b.freeze(g); // Foo 6.2
		PosNegRWExample ex = new PosNegRWExample(parts[0],g,queryVec,posList,negList);// Foo 6.3
		
	}

	private static int[] parseNodes(String string) { // static: Foo F
		String[] nodeStrings = split(string,NODE_DELIM);
		int[] nodes = new int[nodeStrings.length];
		for (int i=0; i<nodeStrings.length; i++) {
			nodes[i] = Integer.parseInt(nodeStrings[i]);
		}
		return nodes;
	}

	private static String[] split(String string, char delim) { // static: Foo F
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

	private String[] parseFeatures(String string) {
		int nfeatures=0;
		for (int i=0; i<string.length(); i++) if (string.charAt(i) == ':') nfeatures++;
		String[] features = new String[nfeatures];
		int last=0;
		for (int next=last,i=0; i<features.length && next!=-1; last=next+1,i++) {
			next=string.indexOf(FEATURE_DELIM,last);
			features[i]=next<0?string.substring(last):string.substring(last,next);
		}
		return features;
	}

	private String[] parseEdgeFeatures(String string) {
		int nfeatures=0;
		for (int i=0; i<string.length(); i++) if (string.charAt(i) == ',') nfeatures++;
		String[] features = new String[nfeatures];
		int last=0;
		for (int next=last,i=0; i<features.length && next!=-1; last=next+1,i++) {
			next=string.indexOf(EDGE_FEATURE_DELIM,last);
			features[i] = next<0?string.substring(last):string.substring(last,next);
		}
		return features;
	}

	public static void main(String[] args) throws IOException,InterruptedException {
		System.err.println("Reading from "+args[1]+" in "+args[0]+" threads...");
		LineNumberReader reader = new LineNumberReader(new FileReader(args[1]));
		ExecutorService pool = Executors.newFixedThreadPool(Integer.parseInt(args[0]));
		//Bar b = new Bar();
		int i=0;
		for(String line; (line=reader.readLine()) != null;) {
			//pool.submit(new Foo(line, i++, b));
			pool.submit(new Foo(line, i++));
		}
		reader.close();
		pool.shutdown();
		pool.awaitTermination(7,TimeUnit.DAYS);
	}
}