package edu.cmu.ml.praprolog.prove;

import org.junit.Test;

public class PprProverTest extends ProverTestTemplate {
	@Override
	public void setup() {
		super.setup();
		PprProver ppr = new PprProver();
		ppr.setTrace(true);
		this.prover = ppr;
	}
	
	@Override @Test
	public void testProveState() {
		((PprProver)prover).setMaxDepth(1);
		super.testProveState();
	}
	
	@Override
	public void setProveStateAnswers() {
		proveStateAnswers[0] = 0.13333; // milk
		proveStateAnswers[1] = 0.06666; // most
		proveStateAnswers[2] = 0.13333; // start
	}
}
