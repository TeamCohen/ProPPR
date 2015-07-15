package edu.cmu.ml.proppr.prove;

import java.io.IOException;

import org.junit.Test;

import edu.cmu.ml.proppr.prove.wam.LogicProgramException;

public class IdPprProverTest extends ProverTestTemplate {
	@Override
	public void setup() throws IOException {
		super.setup();
		IdPprProver ppr = new IdPprProver();
		ppr.setTrace(true);
		this.prover = ppr;
	}
	
	@Override @Test
	public void testProveState() throws LogicProgramException {
		((IdPprProver)prover).setMaxDepth(1);
		super.testProveState();
	}
}
