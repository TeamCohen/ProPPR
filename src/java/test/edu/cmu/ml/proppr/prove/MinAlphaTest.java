package edu.cmu.ml.proppr.prove;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import edu.cmu.ml.proppr.Grounder.ExampleGrounderConfiguration;
import edu.cmu.ml.proppr.GrounderTest;
import edu.cmu.ml.proppr.prove.wam.ConstantArgument;
import edu.cmu.ml.proppr.prove.wam.Goal;
import edu.cmu.ml.proppr.prove.wam.LogicProgramException;
import edu.cmu.ml.proppr.prove.wam.Outlink;
import edu.cmu.ml.proppr.prove.wam.ProofGraph;
import edu.cmu.ml.proppr.prove.wam.Query;
import edu.cmu.ml.proppr.prove.wam.State;
import edu.cmu.ml.proppr.prove.wam.WamBaseProgram;
import edu.cmu.ml.proppr.prove.wam.WamProgram;
import edu.cmu.ml.proppr.util.APROptions;
import edu.cmu.ml.proppr.util.Configuration;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.ModuleConfiguration;
import edu.cmu.ml.proppr.util.ParamsFile;
import edu.cmu.ml.proppr.util.SimpleParamVector;
import static org.junit.Assert.*;

public class MinAlphaTest {
	static String PLUGIN=GrounderTest.FACTS.replace(".cfacts", "_minAlphaAdditions.cfacts");
	static int inputFiles = 0;
	static int outputFiles = 0;
	static int constants = Configuration.USE_WAM;
	static int modules = Configuration.USE_PROVER | Configuration.USE_WEIGHTINGSCHEME;
	static String argstr = new StringBuilder("--programFiles ")
			.append(GrounderTest.RULES).append(":").append(GrounderTest.FACTS).append(":").append(PLUGIN)
			.append(" --prover dpr").toString();
	Query q = Query.parse("predict(howard,Y)");
	
	@Test
	public void testDefault() throws LogicProgramException, IOException {
		ModuleConfiguration c = new ModuleConfiguration(
				argstr.split(" "), inputFiles, outputFiles, constants, modules);
		dprRescaleTest(c);
	}
	
	@Test
	public void testSigmoid() throws LogicProgramException, IOException {
		ModuleConfiguration c = new ModuleConfiguration(
				(argstr+" --weightingScheme sigmoid").split(" "), inputFiles, outputFiles, constants, modules);
		dprRescaleTest(c);
	}
	
	@Test
	public void testTanh() throws LogicProgramException, IOException {
		ModuleConfiguration c = new ModuleConfiguration(
				(argstr+" --weightingScheme tanh").split(" "), inputFiles, outputFiles, constants, modules);
		dprRescaleTest(c);
	}
	
	@Test
	public void testExp() throws LogicProgramException, IOException {
		ModuleConfiguration c = new ModuleConfiguration(
				(argstr+" --weightingScheme exp").split(" "), inputFiles, outputFiles, constants, modules);
		dprRescaleTest(c);
	}
	
	@Test
	public void testReLU() throws LogicProgramException, IOException {
		ModuleConfiguration c = new ModuleConfiguration(
				(argstr+" --weightingScheme ReLU").split(" "), inputFiles, outputFiles, constants, modules);
		dprRescaleTest(c);
	}
	
	@Test
	public void testLinear() throws LogicProgramException, IOException {
		ModuleConfiguration c = new ModuleConfiguration(
				(argstr+" --weightingScheme linear").split(" "), inputFiles, outputFiles, constants, modules);
		dprRescaleTest(c);
	}
	
	// template for subclasses
	public void init(ModuleConfiguration c) {}
	
	public void dprRescaleTest(ModuleConfiguration c)  throws LogicProgramException, IOException {
		init(c);
		ProofGraph pg = new ProofGraph(q,new APROptions(), c.program, c.plugins);
		DprProver prover = (DprProver) c.prover;
		
		prover.backtrace.start();
		HashMap<State,Double> r = new HashMap<State,Double>();
		r.put(pg.getStartState(), 1.0);
		HashMap<State,Integer> d = new HashMap<State,Integer>();
		d.put(pg.getStartState(), pg.pgDegree(pg.getStartState(), true, true) - 1);
		push(prover, pg, new HashMap<State,Double>(), r, d, pg.getStartState(), 0);
	}
	
	public void rescaleAssertions(double oldAB, double newAB) {
		assertEquals(oldAB, newAB, 1e-10);
	}
	
	public void push(DprProver prover,ProofGraph pg, Map<State,Double> p, Map<State, Double> r,
			Map<State, Integer> deg, State u, int pushCounter) throws LogicProgramException {
		double ru = r.get(u);
		if (ru / deg.get(u) > prover.apr.epsilon) {
			List<Outlink> outs = pg.pgOutlinks(u, true, true);
			Outlink restart = null;
			double z = 0;
			for (Outlink o : outs) {
				if (o.child.equals(pg.getStartState())) {
					restart = o;
				}
				o.wt = prover.weighter.w(o.fd);
				z += o.wt;
			}
			assertTrue(restart != null);
			
			if (restart.fd.containsKey(ProofGraph.ALPHABOOSTER)) {
//				double newAB = prover.computeAlphaBooster(restart.fd.get(ProofGraph.ALPHABOOSTER), z, restart.wt);
//				rescaleAssertions(restart.fd.get(ProofGraph.ALPHABOOSTER), newAB);
//				restart.fd.put(ProofGraph.ALPHABOOSTER,newAB);
//				restart.wt = prover.weighter.w(restart.fd);
				z = prover.rescaleResetLink(restart, z);
			}
			
			double localAlpha = restart.wt / z;
			
			if (localAlpha < prover.apr.alpha) {
				z = prover.rescaleResetLink(restart,z);
			}
			assertFalse("minAlpha exception: "+localAlpha,localAlpha < prover.apr.alpha);
			
			Dictionary.increment(p,u,prover.apr.alpha * ru,"(elided)");
			r.put(u, r.get(u) * prover.stayProbability * (1.0-prover.apr.alpha));

			restart.wt = ( z * (localAlpha - prover.apr.alpha) );
			for (Outlink o : outs) {
				prover.includeState(o,r,deg,z,ru,pg);
			}

			for (Outlink o : outs) {
				if (o.equals(restart)) continue;
				// current pushcounter is passed down, gets incremented and returned, and 
				// on the next for loop iter is passed down again...
				push(prover,pg,p,r,deg,o.child,pushCounter);
			}
		}
	}
}
