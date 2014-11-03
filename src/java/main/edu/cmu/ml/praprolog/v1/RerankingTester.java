package edu.cmu.ml.praprolog.v1;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.cmu.ml.praprolog.examples.PosNegRWExample;
import edu.cmu.ml.praprolog.graph.v1.GraphWriter;
import edu.cmu.ml.praprolog.learn.SRW;
import edu.cmu.ml.praprolog.learn.tools.WeightingScheme;
import edu.cmu.ml.praprolog.prove.v1.LogicProgram;
import edu.cmu.ml.praprolog.prove.v1.LogicProgramState;
import edu.cmu.ml.praprolog.prove.v1.Prover;
import edu.cmu.ml.praprolog.prove.v1.ThawedPosNegExample;
import edu.cmu.ml.praprolog.util.ParamVector;

public class RerankingTester extends Tester {
	private static final Logger log = Logger.getLogger(RerankingTester.class);
	protected ParamVector params = null;
	protected SRW<PosNegRWExample<String>> srw;
	public RerankingTester(Prover p, LogicProgram lp, SRW<PosNegRWExample<String>> walker) {
		super(p, lp);
		this.srw = walker;
	}
	
	@Override
	public void setParams(ParamVector params, WeightingScheme wScheme) {
		// weightingScheme is only used by the SRW, which got a copy during configuration
		this.params = params;
	}
	
	@Override
	public Map<LogicProgramState,Double> getSolutions(ThawedPosNegExample x,LogicProgram program) {
		GraphWriter writer = new GraphWriter();
		Map<LogicProgramState,Double> ans = this.prover.proveState(program, x.getQueryState(), writer);
		if (this.params == null) return ans;
		
		HashMap<String,LogicProgramState> graphIds = new HashMap<String,LogicProgramState>();
		for (Map.Entry<LogicProgramState,Double> e : ans.entrySet()) {
			if (e.getKey().isSolution()) {
				graphIds.put(writer.getId(e.getKey()),e.getKey());
			}
		}
		
		Map<String,Double> start = new HashMap<String,Double>(); start.put(writer.getId(x.getQueryState()),1.0);
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
