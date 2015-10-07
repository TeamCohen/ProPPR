package edu.cmu.ml.proppr.prove;

import static org.junit.Assert.*;

import java.io.IOException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;

import edu.cmu.ml.proppr.prove.wam.LogicProgramException;
import edu.cmu.ml.proppr.util.APROptions;

public class IdDprProverTest extends ProverTestTemplate {
	@Override
	public void setup() throws IOException {
		super.setup();
		this.prover = new IdDprProver(new APROptions(new String[] {"eps=.00001","alph=.03"}));
	}
	
	@Override @Test
	public void testProveState() throws LogicProgramException {
		super.testProveState();
	}

}
