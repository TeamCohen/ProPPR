package edu.cmu.ml.praprolog.v1;

import edu.cmu.ml.praprolog.examples.PosNegRWExample;
import edu.cmu.ml.praprolog.graph.v1.GraphWriter;
import edu.cmu.ml.praprolog.learn.SRW;
import edu.cmu.ml.praprolog.learn.tools.WeightingScheme;
import edu.cmu.ml.praprolog.prove.v1.Goal;
import edu.cmu.ml.praprolog.prove.v1.InnerProductWeighter;
import edu.cmu.ml.praprolog.prove.v1.LogicProgram;
import edu.cmu.ml.praprolog.prove.v1.LogicProgramState;
import edu.cmu.ml.praprolog.prove.v1.ProPPRLogicProgramState;
import edu.cmu.ml.praprolog.prove.v1.Prover;
import edu.cmu.ml.praprolog.util.Dictionary;
import edu.cmu.ml.praprolog.util.ParamVector;

import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class RerankingQueryAnswerer extends QueryAnswerer {
	private static final Logger log = Logger.getLogger(RerankingQueryAnswerer.class);

	protected ParamVector params = null;
	protected SRW<PosNegRWExample<String>> srw;
	public RerankingQueryAnswerer(SRW<PosNegRWExample<String>> walker) {
		this.srw = walker;
	}
	@Override
	public void addParams(LogicProgram program, ParamVector params, WeightingScheme wScheme) {
		// weightingScheme is only used by the SRW, which got a copy during configuration
		this.params = params;
	}
	@Override
	public Map<LogicProgramState,Double> getSolutions(Prover prover,Goal query,LogicProgram program) {
		// FIXME: this is copypasta from RerankingTester
		GraphWriter writer = new GraphWriter();
		Map<LogicProgramState,Double> ans = prover.proveState(program, new ProPPRLogicProgramState(query), writer);
		if (this.params == null) return ans;
		
		HashMap<String,LogicProgramState> graphIds = new HashMap<String,LogicProgramState>();
		for (Map.Entry<LogicProgramState,Double> e : ans.entrySet()) {
			if (e.getKey().isSolution()) {
				graphIds.put(writer.getId(e.getKey()),e.getKey());
			}
		}
		
		Map<String,Double> start = new HashMap<String,Double>(); start.put(writer.getId(new ProPPRLogicProgramState(query)),1.0);
		Map<String,Double> result = this.srw.rwrUsingFeatures(writer.getGraph(), start, this.params);
		for (Map.Entry<String,LogicProgramState> e : graphIds.entrySet()) {
			if (!result.containsKey(e.getKey())) {
				log.warn("RWR did not retrieve baseline solution "+e.getValue()+"; removing");
				ans.remove(e.getValue());
			} else {
				ans.put(e.getValue(), result.get(e.getKey()));
			}
		}
		
		return ans;
	}

}
