package edu.cmu.ml.proppr.prove;

import java.io.IOException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;

import edu.cmu.ml.proppr.prove.DprProver;
import edu.cmu.ml.proppr.prove.wam.LogicProgramException;
import edu.cmu.ml.proppr.util.APROptions;


public class DprProverTest extends ProverTestTemplate {
	@Override
	public void setup() throws IOException {
		super.setup();
		this.prover = new DprProver(new APROptions(new String[] {"eps=.00001","alph=.03"}));
//		Logger.getLogger(DprProver.class).setLevel(Level.DEBUG);
	}
	
	@Override @Test
	public void testProveState() throws LogicProgramException {
		// answers are
		// start state   0.53564
		// most features 0.03571
		// milk features 0.07142
		super.testProveState();
	}

}
