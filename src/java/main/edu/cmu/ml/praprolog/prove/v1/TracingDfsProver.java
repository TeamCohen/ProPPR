package edu.cmu.ml.praprolog.prove.v1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import edu.cmu.ml.praprolog.graph.GraphWriter;
import edu.cmu.ml.praprolog.prove.v1.LogicProgram.LogicProgramOutlink;

/**
 * A prover with scores based on simple depth-first-search, which
    additional prints out a detailed trace.
 * @author wcohen,krivard
 *
 */
public class TracingDfsProver extends Prover {
	private static final Logger log = Logger.getLogger(TracingDfsProver.class);
	public static final int DEFAULT_MAXDEPTH = 5;
	protected int maxDepth;
	private Backtrace backtrace = new Backtrace(log);
	public TracingDfsProver() {
		this(DEFAULT_MAXDEPTH);
	}
	public TracingDfsProver(int maxDepth) {
		this.maxDepth=maxDepth;
	}
	
	@Override
	public String toString() { return "tr:"+this.maxDepth; }
	
	public Prover copy() {
		return new TracingDfsProver(this.maxDepth);
	}
	
	@Override
	public Map<LogicProgramState, Double> proveState(LogicProgram lp,
			LogicProgramState state0, GraphWriter w) {
		HashMap<LogicProgramState,Double> result = new HashMap<LogicProgramState,Double>();
		backtrace.start();
		List<WeightedLogicProgramState> states = this.dfs(lp,state0, w);
		System.out.println();
		int i=0;
		for (WeightedLogicProgramState s : states) {
			showState(s);
			result.put(s.s,1.0/(++i));
		}
		return result;
	}
	/**
	 * Print this state-weight pair to stdout
	 * @param s
	 */
	private void showState(WeightedLogicProgramState s) {
		System.out.println(buildState(s).toString());
	}
	private StringBuilder buildState(WeightedLogicProgramState s) {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<s.s.getDepth(); i++) sb.append(" | ");
		if (s.s.isSolution()) {
			sb.append(s.s.description()).append(" using ");
		}
		sb.append(s.s.toString());
		return sb;
	}
	/**
	 * Do depth first search from a state, yielding all states in the tree.
	 * @param lp
	 * @param state0
	 * @return
	 */
	protected List<WeightedLogicProgramState> dfs(LogicProgram lp, LogicProgramState state0, GraphWriter gw) {
		return dfs(lp,state0,gw,1.0);
	}
	protected List<WeightedLogicProgramState> dfs(LogicProgram lp,
			LogicProgramState state00, GraphWriter gw, double incomingEdgeWeight) {
		if (! (state00 instanceof ProPPRLogicProgramState))
			throw new UnsupportedOperationException("TracingDfsProver can't handle prolog states yet");
		ProPPRLogicProgramState state0 = (ProPPRLogicProgramState) state00;
		List<WeightedLogicProgramState> result = new ArrayList<WeightedLogicProgramState>();
		backtrace.push(state0);
		result.add(new WeightedLogicProgramState(state0, incomingEdgeWeight));
		log.info(buildState(result.get(result.size()-1)).toString());
		if (!state0.isSolution() && state0.getDepth() < this.maxDepth) {
			try {
				for(LogicProgramOutlink w : lp.lpOutlinks(state0, LogicProgram.DEFAULT_TRUELOOP, LogicProgram.DEFAULT_RESTART)){ // trueloop, restart
					log.debug("@"+state0.getDepth()+" "+state0+" -> "+w.getState());
					if (gw != null) gw.writeEdge(state0, w.getState(), w.getFeatureList());
					result.addAll(this.dfs(lp,w.getState(),gw,w.getWeight()));
				}
			} catch (LogicProgramException e) {
				backtrace.print(e);
			}
		}
		backtrace.pop(state0);
		return result;
	}
	
	private class WeightedLogicProgramState {
		ProPPRLogicProgramState s;
		double w;
		public WeightedLogicProgramState(ProPPRLogicProgramState state, double weight) {
			this.s = state;
			this.w = weight;
		}
	}
}
