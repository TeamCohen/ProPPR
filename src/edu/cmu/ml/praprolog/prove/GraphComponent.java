package edu.cmu.ml.praprolog.prove;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.cmu.ml.praprolog.util.Dictionary;
import edu.cmu.ml.praprolog.util.SymbolTable;

/**
 * A 'extensional database' - restricted to be a labeled directed
    graph, or equivalently, a set of f(+X,-Y) unit predicates.
 * @author wcohen,krivard
 *
 */
public class GraphComponent extends GraphlikeComponent {
	private static final Logger log = Logger.getLogger(GraphComponent.class);

	public static final String FILE_EXTENSION = "graph";
	private static final List<Argument> DEFAULT_INDEX = Collections.emptyList();

	// ewww
	protected Map<String,Map<Argument,List<Argument>>> index;
	protected Map<Goal, Double> featureDict;


	public GraphComponent() {
		this("graphEDB");
	}
	public GraphComponent(String label) {

		this.index = new HashMap<String,Map<Argument,List<Argument>>>();
		this.featureDict = new HashMap<Goal,Double>();
		this.featureDict.put(new Goal("id",label),1.0);
	}
	@Override
	protected void _indexAppend(String functor, Argument src, Argument dst) {
		Dictionary.safeAppend(this.index,functor,src,dst);
	}
	@Override
	protected boolean _indexContains(String functor) {
		// TODO Auto-generated method stub
		return this.index.containsKey(functor);
	}
	@Override
	protected List<Argument> _indexGet(String functor, Argument srcConst) {
		// TODO Auto-generated method stub
		return Dictionary.safeGet(this.index, functor,srcConst,DEFAULT_INDEX);
	}
	@Override
	protected int _indexGetDegree(String functor, Argument srcConst) {
		// TODO Auto-generated method stub
		return Dictionary.safeGet(this.index, functor, srcConst, DEFAULT_INDEX).size();
	}
	@Override
	protected Map<Goal, Double> getFeatureDict() {
		// TODO Auto-generated method stub
		return this.featureDict;
	}

	/**
	 * Return a simpleGraphComponent with all the components loaded from
        a file.  The format of the file is that each line is a tab-separated 
        triple of edgelabel, sourceNode, destNode.
	 * @param fileName
	 * @return
	 */
	public static GraphComponent load(String fileName) {
		GraphComponent result = new GraphComponent(fileName);
		try {
			LineNumberReader reader = new LineNumberReader(new FileReader(fileName));
			String line;
			while ((line=reader.readLine())!= null) {
				if(reader.getLineNumber() % 10000 == 0) log.info("Read "+reader.getLineNumber()+" lines");
				line=line.trim();
				if (line.startsWith("#") || line.length()==0) continue;
				String[] parts = line.split("\t");
				if (parts.length != 3) throw new IllegalStateException("Bad line "+reader.getLineNumber()+" (must be $edge\t$src\t$dst): "+line);
				String edgeLabel = parts[0].trim();
				String src = parts[1].trim();
				String dst = parts[2].trim();
				result.addEdge(edgeLabel, Argument.fromString(src), Argument.fromString(dst));
			}
			reader.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}
}
