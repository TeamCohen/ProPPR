package edu.cmu.ml.praprolog.prove.v1;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.cmu.ml.praprolog.graph.v1.GraphWriter;
import edu.cmu.ml.praprolog.prove.v1.Prover;
import edu.cmu.ml.praprolog.prove.v1.LogicProgram.LogicProgramOutlink;
import edu.cmu.ml.praprolog.util.Dictionary;

public class PprProver extends Prover {
	private static final Logger log = Logger.getLogger(PprProver.class);
	private static final boolean RESTART = true;
	private static final boolean TRUELOOP = true;
	public static final int DEFAULT_MAXDEPTH=5;
	protected int maxDepth;
	protected boolean trace;
	
	public PprProver() { this(DEFAULT_MAXDEPTH); }
	public PprProver(int md) {
		this(md,false);
	}
	public PprProver(int md, boolean tr) {
		this.maxDepth=md;
		trace=tr;
	}
	
	@Override
	public String toString() { return "ppr:"+this.maxDepth; }
	
	public Prover copy() {
		return new PprProver(this.maxDepth, this.trace);
	}
	
	public void setMaxDepth(int i) {
		this.maxDepth = i;
	}
	public void setTrace(boolean b) {
		this.trace = b;
	}
	public Map<LogicProgramState, Double> proveState(LogicProgram lp,
			LogicProgramState state0, GraphWriter gw) {
		Map<LogicProgramState,Double> startVec = new HashMap<LogicProgramState,Double>();
		startVec.put(state0,1.0);
		Map<LogicProgramState,Double> vec = startVec;
		
		for (int i=0; i<this.maxDepth; i++) {
			vec = walkOnce(lp, vec, gw);
			if (log.isInfoEnabled()) log.info("iteration/descent "+(i-1)+" complete");
			if(log.isDebugEnabled()) log.debug("after iteration "+(i+1)+" :"+
					Dictionary.buildString(vec,new StringBuilder(),"\n\t").toString());
		}
		
		return vec;
	}
	protected Map<LogicProgramState, Double> walkOnce(LogicProgram lp,
			Map<LogicProgramState, Double> vec, GraphWriter gw) {
		
		Map<LogicProgramState, Double> nextVec = new HashMap<LogicProgramState, Double>();
		int i=1,n=vec.size();
		for (Map.Entry<LogicProgramState, Double> s : vec.entrySet()) {
			log.info("state "+(i++)+" of "+n);
			try {
				for (LogicProgramOutlink o : lp.lpNormalizedOutlinks(s.getKey(), TRUELOOP, RESTART)) {
					if (gw != null) gw.writeEdge(s.getKey(), o.getState(), o.getFeatureList());
					if (log.isTraceEnabled()) log.trace("walkonce normalizedOutlinks "+s.getKey()+" "+o.getWeight()+" "+o.getState());
					Dictionary.increment(nextVec, o.getState(), o.getWeight() * s.getValue(),"(elided)");
				}
			} catch (LogicProgramException e) {
				throw new IllegalStateException(e);
			}
		}
		return nextVec;
	}

}
