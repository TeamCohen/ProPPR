package edu.cmu.ml.praprolog.trove;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.cmu.ml.praprolog.Tester;
import edu.cmu.ml.praprolog.graph.GraphWriter;
import edu.cmu.ml.praprolog.learn.tools.WeightingScheme;
import edu.cmu.ml.praprolog.prove.v1.LogicProgram;
import edu.cmu.ml.praprolog.prove.v1.LogicProgramState;
import edu.cmu.ml.praprolog.prove.v1.Prover;
import edu.cmu.ml.praprolog.prove.v1.ThawedPosNegExample;
import edu.cmu.ml.praprolog.trove.graph.AnnotatedTroveGraph;
import edu.cmu.ml.praprolog.trove.graph.AnnotatedTroveGraph.GraphFormatException;
import edu.cmu.ml.praprolog.trove.learn.SRW;
import edu.cmu.ml.praprolog.trove.learn.tools.PosNegRWExample;
import edu.cmu.ml.praprolog.util.ParamVector;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;

public class RerankingTester extends Tester {
	private static final Logger log = Logger.getLogger(RerankingTester.class);
	protected ParamVector params = null;
	protected SRW<PosNegRWExample> srw;
	public RerankingTester(Prover p, LogicProgram lp, SRW<PosNegRWExample> walker) {
		super(p, lp);
		this.srw = walker;
	}
	
	@Override
	public void setParams(ParamVector params, WeightingScheme wScheme) {
		this.params = params;
	}
	
	@Override
	public Map<LogicProgramState,Double> getSolutions(ThawedPosNegExample x,LogicProgram program) {
		GraphWriter writer = new GraphWriter();
		Map<LogicProgramState,Double> ans = this.prover.proveState(program, x.getQueryState(), writer);
		if (this.params == null) return ans;
		
		// Get all the GraphWriter ids of the solutions
		HashMap<String,LogicProgramState> graphIds = new HashMap<String,LogicProgramState>();
		for (Map.Entry<LogicProgramState,Double> e : ans.entrySet()) {
			if (e.getKey().isSolution()) {
				graphIds.put(writer.getId(e.getKey()),e.getKey());
			}
		}
		
		// Convert the GraphWriter AnnotatedGraph<String> to AnnotatedTroveGraph,
		// which keeps its own separate node index
		AnnotatedTroveGraph graph = new AnnotatedTroveGraph();
		try {
			AnnotatedTroveGraph.fromStringParts(writer.getGraph().toString(), graph);
		} catch (GraphFormatException e1) {
			throw new IllegalStateException("Programmer error? Parsing should work here.");
		}
		
		// first dereference to the GraphWriter index, then dereference to the Trove index
		TIntDoubleMap start = new TIntDoubleHashMap(); start.put(graph.keyToId(writer.getId(x.getQueryState())),1.0);
		
		// do the RWR, and record the new weights of the solutions we found before
		TIntDoubleMap result = this.srw.rwrUsingFeatures(graph, start, this.params);
		for (Map.Entry<String,LogicProgramState> e : graphIds.entrySet()) {
			if (!result.containsKey(graph.keyToId(e.getKey()))) {
				log.warn("RWR did not retrieve baseline solution "+e.getValue()+"; removing");
				ans.remove(e.getValue());
			} else {
				ans.put(e.getValue(), result.get(graph.keyToId(e.getKey())));
			}
		}
		
		return ans;
	}

}
