package edu.cmu.ml.proppr.prove;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.prove.wam.LogicProgramException;
import edu.cmu.ml.proppr.prove.wam.Outlink;
import edu.cmu.ml.proppr.prove.wam.ProofGraph;
import edu.cmu.ml.proppr.prove.wam.State;
import edu.cmu.ml.proppr.prove.wam.WamInterpreter;
import edu.cmu.ml.proppr.util.Dictionary;
/**
 * A prover with scores based on simple depth-first-search
 * @author "William Cohen <wcohen@cs.cmu.edu>"
 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
 *
 */
public class DfsProver extends Prover {
	private static final Logger log = Logger.getLogger(DfsProver.class);
	public static final int DEFAULT_MAXDEPTH=6;
	private Backtrace<State> backtrace = new Backtrace<State>(log);
	protected int maxDepth;
	protected boolean trueLoop;
	protected boolean restart;

	
	public DfsProver() {
		init();
	}
	public DfsProver(FeatureDictWeighter w, int maxDepth) {
		super(w);
		init(maxDepth);
	}
	public DfsProver(FeatureDictWeighter w, int maxDepth, boolean trueLoop, boolean restart) {
		super(w);
		init(maxDepth, trueLoop, restart);
	}
	private void init() { init(DEFAULT_MAXDEPTH); }
	protected void init(int maxDepth) {
		init(maxDepth,ProofGraph.DEFAULT_TRUELOOP,ProofGraph.DEFAULT_RESTART);
	}
	private void init(int maxDepth, boolean trueLoop, boolean restart) {
		this.maxDepth = maxDepth;
		this.trueLoop = trueLoop;
		this.restart = restart;
	}
	public Prover copy() {
		return new DfsProver(this.weighter, this.maxDepth, this.trueLoop, this.restart);
	}

	protected class Entry {
		public State state;
		public double w;
		public Entry(State s, Double d) {this.state = s; this.w = d;}
	}
	protected List<Entry> dfs(ProofGraph pg, State s, int depth) throws LogicProgramException {
		List<Entry> result = new LinkedList<DfsProver.Entry>();
		dfs(pg,s,depth,1.0,result);
		return result;
	}
	/**
	 * Do depth first search from a state, yielding all states in the tree,
        together with the incoming weights. 
	 * @throws LogicProgramException */
	protected void dfs(ProofGraph pg, State s, int depth, double incomingEdgeWeight, List<Entry> tail) throws LogicProgramException {
		beforeDfs(s, pg, depth);
		tail.add(new Entry(s,incomingEdgeWeight));
		if (!s.isCompleted() && depth < this.maxDepth) {
			backtrace.push(s);
			List<Outlink> outlinks = pg.pgOutlinks(s, trueLoop, restart);
			if (outlinks.size() == 0) log.debug("exit dfs: no outlinks for "+s);
			for (Outlink o : outlinks) {
				double w = this.weighter.w(o.fd);
				dfs(pg, o.child, depth+1, w, tail);
			}
			backtrace.pop(s);
		} else log.debug("exit dfs: "+ (s.isCompleted() ? "state completed" : ("depth "+depth+">"+this.maxDepth)));
	}

	/** 
	 * Template for subclasses
	 * @param depth 
	 * @param s 
	 * @param interp 
	 * @throws LogicProgramException 
	 */
	protected void beforeDfs(State s, ProofGraph pg, int depth) throws LogicProgramException {}
	@Override
	public Map<State, Double> prove(ProofGraph pg) {
		Map<State,Double>vec = new HashMap<State,Double>();
		int i=0;
		backtrace.start();
		try {
			for (Entry e : dfs(pg,pg.getStartState(),0)) {
				vec.put(e.state, 1.0/(i+1));
				i++;
			}
		} catch (LogicProgramException e) {
			backtrace.print(e);
		}
		return vec;
	}

}
