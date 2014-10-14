package edu.cmu.ml.praprolog.prove.v1;

import org.junit.Test;

import edu.cmu.ml.praprolog.prove.v1.DprProver;

public class DprProverTest extends ProverTestTemplate {
	@Override
	public void setup() {
		super.setup();
		this.prover = new DprProver(0.00001, .03);
//		for (Component c : this.lpMilk.components) {
//			c.setAlpha(0.03);
//		}
	}
	
	@Override @Test
	public void testProveState() {
		// answers are
		// start state   0.53564
		// most features 0.03571
		// milk features 0.07142
		super.testProveState();
	}
	
	@Override
	public void setProveStateAnswers() {
		proveStateAnswers[0] = 0.07142; // milk
		proveStateAnswers[1] = 0.03571; // most
		proveStateAnswers[2] = 0.53564; // start
	}

}
