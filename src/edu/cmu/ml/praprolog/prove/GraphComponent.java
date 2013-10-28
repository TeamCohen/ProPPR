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
public class GraphComponent extends Component {
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
	/**
	 * An an arc to a graph-based EDB.
	 * @param functor
	 * @param src
	 * @param dst
	 */
	protected void addEdge(String functor, Argument src, Argument dst) {
		Dictionary.safeAppend(this.index,functor,src,dst);
	}

	@Override
	public boolean claim(LogicProgramState state) {
		return !state.isSolution() && this.contains(state.getHeadGoal());
	}

	protected boolean contains(Goal goal) {
		return goal.getArity()==2 && this.index.containsKey(goal.getFunctor());// && goal.getArg(1).isVariable();
	}
	
	@Override
	public List<Outlink> outlinks(LogicProgramState state0) {
		if (! (state0 instanceof ProPPRLogicProgramState))
			throw new UnsupportedOperationException("GraphComponents can't handle prolog states yet");
		ProPPRLogicProgramState state = (ProPPRLogicProgramState) state0;
		Goal g = state.getHeadGoal();
		Argument srcConst = convertConst(0,state);
		Argument dstVar   = convertVar(1,state);
		
		List<Argument> values = Dictionary.safeGet(this.index, g.getFunctor(),srcConst,DEFAULT_INDEX);
		
		List<Outlink> result = new ArrayList<Outlink>();
		if (values.size() > 0) {
			double w = 1.0/values.size();
			for (Argument v : values) {
				RenamingSubstitution thnew = new RenamingSubstitution(state.getTheta().offset);//state.getTheta().copy();
				thnew.put(dstVar,v); 
				result.add(new Outlink(this.featureDict, state.child(new Goal[0],thnew)));
			}
		}
		return result;
	}
	
	protected Argument convertConst(int i, LogicProgramState state) {
		Argument result = state.getHeadGoal().getArg(i);//state.getTheta().valueOf(state.getHeadGoal().getArg(i).getRenamed(state.getVarSketchSize()));
		if (!result.isConstant()) throw new IllegalStateException("Argument "+(i+1)+" of "+state.getHeadGoal()+" should be bound in theta; was "+result);
		return result;
	}
	
	protected Argument convertVar(int i, LogicProgramState state) {
		Argument result = state.getHeadGoal().getArg(i);//state.getTheta().valueOf(state.getHeadGoal().getArg(i).getRenamed(state.getVarSketchSize()));
		if (!result.isVariable()) 
			throw new IllegalStateException("Argument "+(i+1)+" of "+state.getHeadGoal()+" should be unbound in theta; was "+result);
		return result;
	}

	@Override
	public void compile() { 
		// pass
	}

	@Override
	public void compile(SymbolTable variableSymTab) {
		// pass
	}

	@Override
	public int degree(LogicProgramState state) {
		if (state.isSolution()) return 0;
		Goal g = state.getHeadGoal();
		Argument srcConst = convertConst(0,state);
		Argument dstVar   = convertVar(1,state);
		return Dictionary.safeGet(this.index, g.getFunctor(), srcConst, DEFAULT_INDEX).size();
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
