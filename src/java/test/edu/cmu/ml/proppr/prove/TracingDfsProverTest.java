package edu.cmu.ml.proppr.prove;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

import edu.cmu.ml.proppr.prove.wam.LogicProgramException;
import edu.cmu.ml.proppr.util.APROptions;

public class TracingDfsProverTest extends ProverTestTemplate {

	@Override
	public void setup() throws IOException {
		super.setup();
		TracingDfsProver p = new TracingDfsProver(new APROptions());
		this.prover = p;
	}
}
