package edu.cmu.ml.proppr.prove;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cmu.ml.proppr.prove.wam.Argument;
import edu.cmu.ml.proppr.prove.wam.LogicProgramException;
import edu.cmu.ml.proppr.prove.wam.Outlink;
import edu.cmu.ml.proppr.prove.wam.ProofGraph;
import edu.cmu.ml.proppr.prove.wam.Query;
import edu.cmu.ml.proppr.prove.wam.State;
import edu.cmu.ml.proppr.prove.wam.VariableArgument;
import edu.cmu.ml.proppr.util.APROptions;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.SymbolTable;

/**
 * abstract prover class - prove a goal, constructing a proof graph
 * @author "William Cohen <wcohen@cs.cmu.edu>"
 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
 *
 */
public abstract class Prover {
	private static final boolean NORMLX_RESTART = true;
	private static final boolean NORMLX_TRUELOOP = true;
	protected FeatureDictWeighter weighter;
	protected APROptions apr;
	public Prover() {
		this(new APROptions());
	}
	public Prover(APROptions apr) {
		this(new UniformWeighter(), apr);
	}
	public Prover(FeatureDictWeighter w, APROptions apr) {
		this.weighter = w;
		this.apr = apr;
	}
	/** Return unfiltered distribution of state associated with proving the start state. 
	 * @throws LogicProgramException */
	public abstract Map<State,Double> prove(ProofGraph pg) throws LogicProgramException;
	
	/** Return a threadsafe copy of the prover */
	public abstract Prover copy();
	
	public void setWeighter(FeatureDictWeighter w) {
		this.weighter = w;
	}
	protected Map<State,Double> normalizedOutlinks(ProofGraph pg, State s) throws LogicProgramException {
		List<Outlink> outlinks = pg.pgOutlinks(s,NORMLX_RESTART,NORMLX_TRUELOOP);
		Map<State,Double> weightedOutlinks = new HashMap<State,Double>();
		double normalizer = 0;
		for (Outlink o : outlinks) {
			double w = this.weighter.w(o.fd);
			weightedOutlinks.put(o.child, w);
			normalizer += w;
		}
		for (Map.Entry<State,Double>e : weightedOutlinks.entrySet()) {
			e.setValue(e.getValue()/normalizer);
		}
		return weightedOutlinks;
	}
	public Map<Query,Double> solvedQueries(ProofGraph pg) throws LogicProgramException {
		Map<State,Double> ans = prove(pg);
		Map<Query,Double> solved = new HashMap<Query,Double>();
		for (Map.Entry<State,Double> e : ans.entrySet()) {
			if (e.getKey().isCompleted()) solved.put(pg.fill(e.getKey()),e.getValue());
		}
		return solved;
	}
	public Map<String,Double> solutions(ProofGraph pg) throws LogicProgramException {
		Map<State,Double> proveOutput = this.prove(pg);
		Map<String,Double> filtered = new HashMap<String,Double>();
		double normalizer = 0;
		for (Map.Entry<State, Double> e : proveOutput.entrySet()) {
			normalizer += e.getValue();
			if (e.getKey().isCompleted()) {
				Map<Argument,String> d = pg.asDict(e.getKey());
				String dstr = "";
				if (!d.isEmpty()) dstr = Dictionary.buildString(d,new StringBuilder()," ").substring(1);
				filtered.put(dstr, Dictionary.safeGet(filtered,dstr)+e.getValue());
			}
		}
		for (Map.Entry<String,Double> e : filtered.entrySet()) {
			e.setValue(e.getValue()/normalizer);
		}
		return filtered;
	}
}
