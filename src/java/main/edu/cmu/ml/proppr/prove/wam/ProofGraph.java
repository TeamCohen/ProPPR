package edu.cmu.ml.proppr.prove.wam;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.examples.GroundedExample;
import edu.cmu.ml.proppr.examples.InferenceExample;
import edu.cmu.ml.proppr.graph.InferenceGraph;
import edu.cmu.ml.proppr.graph.LightweightStateGraph;
import edu.cmu.ml.proppr.prove.wam.plugins.WamPlugin;
import edu.cmu.ml.proppr.prove.wam.plugins.builtin.FilterPluginCollection;
import edu.cmu.ml.proppr.prove.wam.plugins.builtin.PluginFunction;
import edu.cmu.ml.proppr.util.APROptions;
import edu.cmu.ml.proppr.util.SimpleSymbolTable;
import edu.cmu.ml.proppr.util.SymbolTable;
import gnu.trove.strategy.HashingStrategy;
import edu.cmu.ml.proppr.util.ConcurrentSymbolTable;
import edu.cmu.ml.proppr.util.math.LongDense;
import edu.cmu.ml.proppr.util.math.SimpleSparse;
import edu.cmu.ml.proppr.util.math.SmoothFunction;

/**
 * # Creates the graph defined by a query, a wam program, and a list of
# WamPlugins.
 * @author "William Cohen <wcohen@cs.cmu.edu>"
 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
 *
 */
public abstract class ProofGraph {
	private static final Logger log = Logger.getLogger(ProofGraph.class);
	public static final boolean DEFAULT_RESTART = false;
	public static final boolean DEFAULT_TRUELOOP = true;
	public static final Feature TRUELOOP = new Feature("id(trueLoop)");
	public static final Feature RESTART = new Feature("id(restart)");
	
	private InferenceExample example;
	private WamProgram program;
	protected final WamInterpreter interpreter;
	private int queryStartAddress;
	protected final ImmutableState startState;
	private int[] variableIds;
	private Map<Feature,Double> trueLoopFD;
	private Feature restartFeature;
	private APROptions apr;
	public ProofGraph(Query query, APROptions apr, WamProgram program, WamPlugin ... plugins) throws LogicProgramException {
		this(query,apr,new SimpleSymbolTable<Feature>(),program,plugins);
	}
	public ProofGraph(Query query, APROptions apr, SymbolTable<Feature> featureTab, WamProgram program, WamPlugin ... plugins) throws LogicProgramException {
		this(new InferenceExample(query,null,null),apr,featureTab,program,plugins);
	}
	public ProofGraph(InferenceExample ex, APROptions apr, SymbolTable<Feature> featureTab, WamProgram program, WamPlugin ... plugins) throws LogicProgramException {
		this.example = ex; 
		this.apr = apr;
		this.program = new WamQueryProgram(program);
		WamPlugin[] fullPluginList = addBuiltinPlugins(plugins);
		this.interpreter = new WamInterpreter(this.program, fullPluginList);
		this.startState = this.createStartState();
		
		this.trueLoopFD = new HashMap<Feature,Double>(); this.trueLoopFD.put(TRUELOOP,1.0);
		this.restartFeature = RESTART;
	}
	private ImmutableState createStartState() throws LogicProgramException {
		// execute to the first call
		this.example.getQuery().variabilize();
		// discard any compiled code added by previous queries
		this.program.revert();
		this.queryStartAddress = program.size();
		// add the query on to the end of the program
		this.program.append(this.example.getQuery());
		// execute querycode to get start state
		Map<Feature,Double> features = this.interpreter.executeWithoutBranching(queryStartAddress);
		if (!features.isEmpty()) throw new LogicProgramException("query should be a call (no features allowed)");
		if (interpreter.getState().isFailed()) throw new LogicProgramException("query shouldn't have failed");
		// remember variable IDs
		State s = interpreter.saveState();
		this.variableIds = new int[s.getHeapSize()];
		int v=1;
		for (int i=0; i<variableIds.length; i++) {
			if (s.hasConstantAt(i)) variableIds[i] = 0;
			else variableIds[i] = -v++;
		}
		ImmutableState result = interpreter.saveState();
		result.setCanonicalHash(this.interpreter, result);
		return result;
	}
	private WamPlugin[] addBuiltinPlugins(WamPlugin ... plugins) {
		WamPlugin[] result = Arrays.copyOf(plugins,plugins.length+1);
		FilterPluginCollection filters = new FilterPluginCollection(this.apr);
		result[plugins.length] = filters;
		filters.register("neq/2", new PluginFunction(){
			@Override
			public boolean run(WamInterpreter wamInterp) throws LogicProgramException {
				String arg1 = wamInterp.getConstantArg(2,1);
				String arg2 = wamInterp.getConstantArg(2,2);
				if (arg1==null || arg2==null) throw new LogicProgramException("cannot call neq/2 unless both variables are bound");
				return arg1!=arg2;
			}});
		return result;
	}
	
	
	
	/* **************** proving ****************** */
	

	protected List<Outlink> computeOutlinks(State state, boolean trueLoop) throws LogicProgramException {
		List<Outlink> result = new ArrayList<Outlink>();
		if (state.isCompleted()) {
			if (trueLoop) {
				result.add(new Outlink(this.trueLoopFD, state));
			}
		} else if (!state.isFailed()) {
			result = this.interpreter.wamOutlinks(state);
		}
		
		// add restart
		Map<Feature,Double> restartFD = new HashMap<Feature,Double>();
		restartFD.put(this.restartFeature,1.0);
		result.add(new Outlink(restartFD, this.startState));
		
		// generate canonical versions of each state
		for (Outlink o : result) {
			o.child.setCanonicalHash(this.interpreter, this.startState);
		}
		return result;
	}
	
	/* ***************************** grounding ******************* */
	
	public abstract int getId(State s);

	public Map<Argument,String> asDict(State s) {
		Map<Argument,String> result = new HashMap<Argument,String>();
//		List<String> constants = this.interpreter.getConstantTable().getSymbolList();
		for (int k : s.getRegisters()) {
			int j = s.dereference(k);
			if (s.hasConstantAt(j)) {
				int varid = k;
				if (j<this.variableIds.length) varid=this.variableIds[j];
				result.put(new VariableArgument(varid), 
						this.interpreter.getConstantTable().getSymbol(s.getIdOfConstantAt(j)));
			}
			else result.put(new VariableArgument(-k), "X"+j);
		}
		return result;
	}
	
	/** Get a copy of the query represented by this proof using the variable bindings from
	 * the specified state.
	 * @param state
	 * @return
	 */
	public Query fill(State state) {
		Goal[] oldRhs = this.example.getQuery().getRhs();
		Goal[] newRhs = new Goal[oldRhs.length];
		Map<Argument,String> values = asDict(state);
		for (int i=0; i<oldRhs.length; i++) {
			newRhs[i] = fillGoal(oldRhs[i], values);
		}
		return new Query(newRhs);
	}
	
	private Goal fillGoal(Goal g, Map<Argument,String> values) {
		return new Goal(g.getFunctor(), fillArgs(g.getArgs(), values));
	}
	private Argument[] fillArgs(Argument[] args, Map<Argument,String> values) {
		Argument[] ret = new Argument[args.length];
		for(int i=0; i<args.length; i++) {
			if (values.containsKey(args[i])) ret[i] = new ConstantArgument(values.get(args[i]));
			else ret[i] = args[i];
		}
		return ret;
	}

	public GroundedExample makeRWExample(Map<State,Double> ans)  {
		List<State> posIds = new ArrayList<State>();
		List<State> negIds = new ArrayList<State>();
		for (Map.Entry<State,Double> soln : ans.entrySet()) {
			if (soln.getKey().isCompleted()) {
				Query ground = fill(soln.getKey());
				// FIXME: slow?
				if (Arrays.binarySearch(this.getExample().getPosSet(), ground) >= 0) posIds.add(soln.getKey());
				if (Arrays.binarySearch(this.getExample().getNegSet(), ground) >= 0) negIds.add(soln.getKey());
			}
		}
		Map<State,Double> queryVector = new HashMap<State,Double>();
		queryVector.put(this.getStartState(), 1.0);
		return new GroundedExample(this._getGraph(), queryVector, posIds, negIds);
	}
	protected abstract InferenceGraph _getGraph();
	
	/* ************************** de/serialization *********************** */
	
	public String serialize(GroundedExample x) {
		StringBuilder line = new StringBuilder();
		line.append(this.example.getQuery().toString())
		.append("\t");
		appendNodes(x.getQueryVec().keySet(), line);
		line.append("\t");
		appendNodes(x.getPosList(), line);
		line.append("\t");
		appendNodes(x.getNegList(), line);
		line.append("\t")
		.append(x.getGraph().serialize())
		.append("\n");
		return line.toString();
	}
	
	private void appendNodes(Iterable<State> group, StringBuilder line) {
		boolean first=true;
		for (State q : group) {
			if (first) first=false;
			else line.append(",");
			line.append(this.getId(q));
		}
	}
	

	/* ************************ getters ****************************** */

	public State getStartState() { return this.startState; }
	public WamInterpreter getInterpreter() { return this.interpreter; }
	public InferenceExample getExample() {
		return this.example;
	}

}
