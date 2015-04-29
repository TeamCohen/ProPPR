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
import java.util.List;
import java.util.regex.Pattern;

public class Foo4 implements Runnable {
   	/*
	  private static final Pattern TAB=Pattern.compile("\t"),
		FEATURE_DELIM=Pattern.compile(":"),
		SRC_DST_DELIM=Pattern.compile("->"),
		FEATURE_WT_DELIM=Pattern.compile("@"),
		EDGE_FEATURE_DELIM=Pattern.compile(",");
	*/
	private static final String TAB="\t",
		FEATURE_DELIM=":",
		SRC_DST_DELIM="->",
		FEATURE_WT_DELIM="@",
		EDGE_FEATURE_DELIM=",";
	
	

	String line;
	int id;
	public Foo4(String line, int id) {
		this.line = line;
		this.id = id;
	}
	public void run() {
		System.out.println(System.currentTimeMillis()+" Job start "+this.id);
		String[] parts = new String[7];
		int last = 0,i=0;
		for (int next = last; i<parts.length && next!=-1; last=next+1,i++) {
			next=this.line.indexOf(TAB,last);
			parts[i] = next<0?this.line.substring(last):this.line.substring(last,next);
		}
		String[] features = parseFeatures(parts[6]);
		
		//int[] nodes = {-1,-1};
		for (int next=last; next!=-1; last=next+1) {
			next = this.line.indexOf(TAB,last);
			
			int srcDest = this.line.indexOf(SRC_DST_DELIM,last);
			int edgeDelim = this.line.indexOf(FEATURE_DELIM,srcDest);
			String[] edgeFeatures = parseEdgeFeatures(next<0?this.line.substring(edgeDelim):this.line.substring(edgeDelim,next));
			//nodes[0] = Integer.parseInt(this.line.substring(last,srcDest));
			//nodes[1] = Integer.parseInt(this.line.substring(srcDest+2,edgeDelim));
			String node0 = this.line.substring(last,srcDest);
			String node1 = this.line.substring(srcDest+2,edgeDelim);
			for (String f : edgeFeatures) {
				int wtDelim = f.indexOf(FEATURE_WT_DELIM);
				String featureName = f.substring(0,wtDelim);
				//double featureWt = Double.parseDouble(f.substring(wtDelim+1));
				String featureWt = f.substring(wtDelim+1);
			}
		} // Foo 4
		System.out.println(System.currentTimeMillis()+" Job done "+this.id);


		// line format: tab delimited
		// x[4]
		// graph[2]
		// features[m]      <-- ':' delimited
		// edges[n]

		/*
		String[] parts = TAB.split(this.line,8); // Foo 3
		String[] features = FEATURE_DELIM.split(parts[6]); // Foo 3
		String[] edges = TAB.split(parts[7]); // Foo 3
		for (String e : edges) {
			String[] edgeParts = FEATURE_DELIM.split(e,2); // Foo 3
			String[] srcdst = SRC_DST_DELIM.split(edgeParts[0],2); // Foo 3
			for (String f : EDGE_FEATURE_DELIM.split(edgeParts[1])) { // Foo 3
				String[] fparts = FEATURE_WT_DELIM.split(f,2); // Foo 3
			}
		}
		*/

		/*
		String[] parts = this.line.split("\t",8); // Foo
		String[] features = parts[6].split(":");
		String[] edges = parts[7].split("\t");
		for (String e : edges) {
			String[] edgeParts = e.split(":",2);
			edgeParts[0].split("->",2);
			for (String f : edgeParts[1].split(",")) {
				String[] fparts = f.split("@",2);
			}
		} // Foo 1
		*/
		
		/*
		String[] parts = this.line.split("\t",8); // Foo
		String[] features = parts[6].split(":");
		String[] edges = parts[7].split("\t");
		for (String e : edges) {
			int srcDest = e.indexOf("->");
			int edgeDelim = e.indexOf(":",srcDest);
			e.substring(0,srcDest);
			e.substring(srcDest+2,edgeDelim);
			for (String f : e.substring(edgeDelim+1).split(",")) {
				int fDelim = f.indexOf("@");
				f.substring(0,fDelim);
				f.substring(fDelim+1);
			}
		} // Foo 1indexOf
		*/

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
		List<String> file = new ArrayList<String>(); // Foo 2
		int i=0;
		for(String line; (line=reader.readLine()) != null;) {
			/*
			file.add(line); // Foo 2
		} // Foo 2
		for (String line : file) { // Foo 2 */
			pool.submit(new Foo4(line, i++));
		}
		reader.close();
		pool.shutdown();
		pool.awaitTermination(7,TimeUnit.DAYS);
	}
}