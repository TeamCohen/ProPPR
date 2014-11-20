package edu.cmu.ml.proppr.prove;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.prove.wam.LogicProgramException;
import edu.cmu.ml.proppr.prove.wam.Outlink;
import edu.cmu.ml.proppr.prove.wam.ProofGraph;
import edu.cmu.ml.proppr.prove.wam.State;
import edu.cmu.ml.proppr.util.Dictionary;
/**
 * prover using power iteration
 * @author "William Cohen <wcohen@cs.cmu.edu>"
 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
 *
 */
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
		init(md,tr);
	}
	public PprProver(FeatureDictWeighter w, int md, boolean tr) {
		super(w);
		init(md,tr);
	}
	private void init(int md, boolean tr) {
		this.maxDepth=md;
		trace=tr;
	}
	
	@Override
	public String toString() { return "ppr:"+this.maxDepth; }
	
	public Prover copy() {
		Prover copy = new PprProver(this.maxDepth, this.trace);
		copy.setWeighter(weighter);
		return copy;
	}
	
	public void setMaxDepth(int i) {
		this.maxDepth = i;
	}
	public void setTrace(boolean b) {
		this.trace = b;
	}
	@Override
	public Map<State, Double> prove(ProofGraph pg) {
		Map<State,Double> startVec = new HashMap<State,Double>();
		startVec.put(pg.getStartState(),1.0);
		Map<State,Double> vec = startVec;
		
		for (int i=0; i<this.maxDepth; i++) {
			vec = walkOnce(pg,vec);
			if (log.isInfoEnabled()) log.info("iteration/descent "+(i-1)+" complete");
			if(log.isDebugEnabled()) log.debug("after iteration "+(i+1)+" :"+
					Dictionary.buildString(vec,new StringBuilder(),"\n\t").toString());
		}
		
		return vec;
	}
	protected Map<State, Double> walkOnce(ProofGraph pg, Map<State, Double> vec) {
		Map<State, Double> nextVec = new HashMap<State, Double>();
		int i=1,n=vec.size();
		for (Map.Entry<State, Double> s : vec.entrySet()) {
			log.info("state "+(i++)+" of "+n);
			try {
				for (Map.Entry<State,Double> e : this.normalizedOutlinks(pg, s.getKey()).entrySet()) {
					if (log.isTraceEnabled()) log.trace("walkonce normalizedOutlinks "+s.getKey()+" "+e.getValue()+" "+e.getKey());
					Dictionary.increment(nextVec, e.getKey(), e.getValue() * s.getValue(),"(elided)");
				}
			} catch (LogicProgramException e) {
				throw new IllegalStateException(e);
			}
		}
		return nextVec;
	}

}
