package edu.cmu.ml.praprolog.prove;

import org.junit.Test;

public class PprProverTest extends ProverTest {
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
}
