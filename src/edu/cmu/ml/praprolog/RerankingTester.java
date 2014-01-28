package edu.cmu.ml.praprolog;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.cmu.ml.praprolog.graph.GraphWriter;
import edu.cmu.ml.praprolog.learn.PosNegRWExample;
import edu.cmu.ml.praprolog.learn.SRW;
import edu.cmu.ml.praprolog.prove.LogicProgram;
import edu.cmu.ml.praprolog.prove.LogicProgramState;
import edu.cmu.ml.praprolog.prove.Prover;
import edu.cmu.ml.praprolog.prove.ThawedPosNegExample;

public class RerankingTester extends Tester {
	private static final Logger log = Logger.getLogger(RerankingTester.class);
	protected Map<String,Double> params = null;
	protected SRW<PosNegRWExample<String>> srw;
	public RerankingTester(Prover p, LogicProgram lp, SRW<PosNegRWExample<String>> walker) {
		super(p, lp);
		this.srw = walker;
	}
	
	@Override
	public void setParams(Map<String,Double> params) {
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
