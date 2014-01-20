package edu.cmu.ml.praprolog.prove;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.cmu.ml.praprolog.util.Dictionary;
import edu.cmu.ml.praprolog.util.ParsedFile;

/**
 * A 'extensional database' - restricted to be a labeled directed
    graph, or equivalently, a set of f(+X,-Y) unit predicates.
 * @author wcohen,krivard
 *
 */
public class GraphComponent extends GraphlikeComponent {
	private static final Logger log = Logger.getLogger(GraphComponent.class);

	public static final String FILE_EXTENSION = ".graph";
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
		this.featureDict.put(new Goal("id",Component.cleanLabel(label)),1.0);
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
		ParsedFile file = new ParsedFile(fileName);
		for (String line : file) {
			if(file.getLineNumber() % 10000 == 0) log.info("Read "+file.getLineNumber()+" lines");
			String[] parts = line.split("\t");
			if (parts.length != 3) file.parseError("must be $edge\t$src\t$dst");
			String edgeLabel = parts[0].trim();
			String src = parts[1].trim();
			String dst = parts[2].trim();
			result.addEdge(edgeLabel, Argument.fromString(src), Argument.fromString(dst));
		}
		file.close();
		return result;
	}
}
