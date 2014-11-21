package edu.cmu.ml.proppr.prove.wam;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import edu.cmu.ml.proppr.examples.GroundedExample;
import edu.cmu.ml.proppr.examples.InferenceExample;
import edu.cmu.ml.proppr.graph.InferenceGraph;
import edu.cmu.ml.proppr.graph.LightweightStateGraph;
import edu.cmu.ml.proppr.prove.Prover;
import edu.cmu.ml.proppr.prove.wam.plugins.WamPlugin;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.SymbolTable;

/**
 * # Creates the graph defined by a query, a wam program, and a list of
# WamPlugins.
 * @author "William Cohen <wcohen@cs.cmu.edu>"
 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
 *
 */
public class ProofGraph {
	public static final boolean DEFAULT_RESTART = false;
	public static final boolean DEFAULT_TRUELOOP = true;
	private static final Goal TRUELOOP = new Goal("id",new ConstantArgument("trueLoop"));
	private static final Goal TRUELOOP_RESTART = new Goal("id",new ConstantArgument("trueLoopRestart"));
	private static final Goal RESTART = new Goal("id",new ConstantArgument("restart"));
	private static final Goal ALPHABOOSTER = new Goal("id",new ConstantArgument("alphaBooster"));
	
	private InferenceExample example;
	private AWamProgram program;
	private WamInterpreter interpreter;
	private int queryStartAddress;
	private ImmutableState startState;
	private int[] variableIds;
	private LightweightStateGraph graph;
	private Map<Goal,Double> trueLoopFD;
	private Map<Goal,Double> trueLoopRestartFD;
	private Goal restartFeature;
	private Goal restartBoosterFeature;
	public ProofGraph(Query query, AWamProgram program, WamPlugin ... plugins) throws LogicProgramException {
		this(new InferenceExample(query,null,null),program,plugins);
	}
	public ProofGraph(InferenceExample ex, AWamProgram program, WamPlugin ... plugins) throws LogicProgramException {
		this.example = ex; 
		this.program = new WamQueryProgram(program);
		// TODO: builtin plugins
		this.interpreter = new WamInterpreter(this.program, plugins);
		this.startState = this.createStartState();
		
		this.trueLoopFD = new HashMap<Goal,Double>(); this.trueLoopFD.put(TRUELOOP,1.0);
		this.trueLoopRestartFD = new HashMap<Goal,Double>(); this.trueLoopRestartFD.put(TRUELOOP_RESTART,1.0);
		this.restartFeature = RESTART;
		this.restartBoosterFeature = ALPHABOOSTER;
		this.graph = new LightweightStateGraph();
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
		Map<Goal,Double> features = this.interpreter.executeWithoutBranching(queryStartAddress);
		if (!features.isEmpty()) throw new LogicProgramException("should be a call");
		if (interpreter.getState().isFailed()) throw new LogicProgramException("query shouldn't have failed");
		// remember variable IDs
		State s = interpreter.saveState();
		this.variableIds = new int[s.getHeapSize()];
		int v=1;
		for (int i=0; i<variableIds.length; i++) {
			if (s.hasConstantAt(i)) variableIds[i] = 0;
			else variableIds[i] = -v++;
		}
		return interpreter.saveState();
	}
	
	/* **************** proving ****************** */
	public List<Outlink> pgOutlinks(State state) throws LogicProgramException {
		return pgOutlinks(state,DEFAULT_TRUELOOP,DEFAULT_RESTART);
	}
	public List<Outlink> pgOutlinks(State state, boolean trueLoop, boolean restart) throws LogicProgramException {
		if (!this.graph.outlinksDefined(state)) {
			List<Outlink> outlinks = this.computeOutlinks(state,trueLoop,restart);
			if (restart) this.graph.setOutlinks(state,outlinks);
			return outlinks;
		}
		return this.graph.getOutlinks(state);
	}
	private List<Outlink> computeOutlinks(State state, boolean trueLoop, boolean restart) throws LogicProgramException {
		List<Outlink> result = new ArrayList<Outlink>();
		if (state.isCompleted()) {
			if (trueLoop) {
				result.add(new Outlink(this.trueLoopFD, state));
			}
			if (restart) {
				result.add(new Outlink(this.trueLoopRestartFD, this.startState));
			}
		} else if (state.isFailed()) {
			Map<Goal,Double> restartFD = new HashMap<Goal,Double>();
			restartFD.put(this.restartFeature,1.0);
			result.add(new Outlink(restartFD, this.startState));
		} else {
			result = this.interpreter.wamOutlinks(state);
			if (restart) {
				int n = this.pgDegree(state);
				Map<Goal,Double> restartFD = new HashMap<Goal,Double>();
				restartFD.put(this.restartFeature,1.0);
				restartFD.put(this.restartBoosterFeature,(double) n);
				result.add(new Outlink(restartFD,this.startState));
			}
		}
		return result;
	}
	/** The number of outlinks for a state. 
	 * @throws LogicProgramException */
	public int pgDegree(State state) throws LogicProgramException {
		return this.pgDegree(state, true, false);
	}
	
	public int pgDegree(State state, boolean trueLoop, boolean restart) throws LogicProgramException {
		return this.pgOutlinks(state, trueLoop, restart).size();
	}
	
	/* ***************************** grounding ******************* */
	

	public Map<Argument,String> asDict(State s) {
		Map<Argument,String> result = new HashMap<Argument,String>();
		List<String> constants = this.interpreter.getConstantTable().getSymbolList();
		for (int k : s.getRegisters()) {
			int j = s.dereference(k);
			if (s.hasConstantAt(j)) result.put(new VariableArgument(this.variableIds[j]), constants.get(s.getIdOfConstantAt(j)-1));
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

	public GroundedExample makeRWExample(Map<State, Double> ans) {
		List<State> posIds = new ArrayList<State>();
		List<State> negIds = new ArrayList<State>();
		for (Map.Entry<State,Double> soln : ans.entrySet()) {
			if (soln.getKey().isCompleted()) {
				Query ground = fill(soln.getKey());
				// FIXME: slow?
				if (Arrays.binarySearch(example.getPosSet(), ground) >= 0) posIds.add(soln.getKey());
				if (Arrays.binarySearch(example.getNegSet(), ground) >= 0) negIds.add(soln.getKey());
			}
		}
		Map<State,Double> queryVector = new HashMap<State,Double>();
		queryVector.put(this.startState, 1.0);
		return new GroundedExample(this.graph, queryVector, posIds, negIds);
	}
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
		.append(x.getGraph().toString())
		.append("\n");
		return line.toString();
	}
	
	private void appendNodes(Iterable<State> group, StringBuilder line) {
		boolean first=true;
		for (State q : group) {
			if (first) first=false;
			else line.append(",");
			line.append(this.graph.getId(q));
		}
	}
	
	/* ************************ getters ****************************** */
	public State getStartState() { return this.startState; }
	public WamInterpreter getInterpreter() { return this.interpreter; }
	public InferenceExample getExample() {
		return this.example;
	}
}
