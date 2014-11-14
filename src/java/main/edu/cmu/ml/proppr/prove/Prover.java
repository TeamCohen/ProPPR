package edu.cmu.ml.proppr.prove;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cmu.ml.proppr.prove.wam.Argument;
import edu.cmu.ml.proppr.prove.wam.LogicProgramException;
import edu.cmu.ml.proppr.prove.wam.Outlink;
import edu.cmu.ml.proppr.prove.wam.ProofGraph;
import edu.cmu.ml.proppr.prove.wam.State;
import edu.cmu.ml.proppr.prove.wam.VariableArgument;
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
	public Prover() {
		this(new UniformWeighter());
	}
	public Prover(FeatureDictWeighter w) {
		this.weighter = w;
	}
	/** Return unfiltered distribution of state associated with proving the start state. 
	 * @throws LogicProgramException */
	public abstract Map<State,Double> prove(ProofGraph pg) throws LogicProgramException;
	/** Return a threadsafe copy of the prover */
	public abstract Prover copy();
	
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
	public Map<String,Double> solutions(ProofGraph pg) throws LogicProgramException {
		return solutions(pg.getInterpreter().getConstantTable(), this.prove(pg));
	}
	public Map<String,Double> solutions(SymbolTable<String> constants, Map<State,Double> proveOutput) {
		Map<String,Double> filtered = new HashMap<String,Double>();
		double normalizer = 0;
		for (Map.Entry<State, Double> e : proveOutput.entrySet()) {
			normalizer += e.getValue();
			if (e.getKey().isCompleted()) {
				Map<Argument,String> d = asDict(constants, e.getKey());
				String dstr = Dictionary.buildString(d,new StringBuilder()," ").substring(1);
				filtered.put(dstr, Dictionary.safeGet(filtered,dstr)+e.getValue());
			}
		}
		for (Map.Entry<String,Double> e : filtered.entrySet()) {
			e.setValue(e.getValue()/normalizer);
		}
		return filtered;
	}
	public static Map<Argument,String> asDict(SymbolTable<String> constantTable, State s) {
		Map<Argument,String> result = new HashMap<Argument,String>();
		List<String> constants = constantTable.getSymbolList();
		for (int k : s.getRegisters()) {
			int j = s.dereference(k);
			if (s.hasConstantAt(j)) result.put(new VariableArgument(-k), constants.get(s.getIdOfConstantAt(j)-1));
			else result.put(new VariableArgument(-k), "X"+j);
		}
		return result;
	}
}
