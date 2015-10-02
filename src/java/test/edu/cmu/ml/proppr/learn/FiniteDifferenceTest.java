package edu.cmu.ml.proppr.learn;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.junit.Test;

import edu.cmu.ml.proppr.RedBlueGraph;
import edu.cmu.ml.proppr.examples.DprExample;
import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.examples.PprExample;
import edu.cmu.ml.proppr.examples.RWExample;
import edu.cmu.ml.proppr.graph.LearningGraph;
import edu.cmu.ml.proppr.graph.LearningGraphBuilder;
import edu.cmu.ml.proppr.learn.ExampleFactory.DprExampleFactory;
import edu.cmu.ml.proppr.learn.ExampleFactory.PprExampleFactory;
import edu.cmu.ml.proppr.learn.tools.Exp;
import edu.cmu.ml.proppr.util.math.MuParamVector;
import edu.cmu.ml.proppr.util.math.ParamVector;
import edu.cmu.ml.proppr.util.math.SimpleParamVector;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;

/**
 * This test is known to fail and we're not sure why. :(
 * 
 * @author krivard
 *
 */
public class FiniteDifferenceTest extends RedBlueGraph {
	private static final Logger log = Logger.getLogger(FiniteDifferenceTest.class);
	protected TIntDoubleMap startVec;

	@Override
	public void moreSetup(LearningGraphBuilder lgb) {
		startVec = new TIntDoubleHashMap();
		startVec.put(nodes.getId("r0"),1.0);
	}
	public double makeLoss(SRW srw, ParamVector<String,?> paramVec, TIntDoubleMap query, int[] pos, int[] neg, ExampleFactory f) {
		srw.clearLoss();
		srw.accumulateGradient(paramVec, f.makeExample("gradient", brGraph, query, pos, neg), new SimpleParamVector<String>());
		return srw.cumulativeLoss().total();
	}
	public ParamVector<String,?> makeGradient(SRW srw, ParamVector<String,?> paramVec, TIntDoubleMap query, int[] pos, int[] neg, ExampleFactory f) {
		ParamVector<String,?> grad = new SimpleParamVector<String>();
		srw.accumulateGradient(paramVec, f.makeExample("gradient", brGraph, query, pos,neg), grad);
		return grad;
	}
	public void test(SRW srw, ParamVector<String,?> uniformParams, ExampleFactory f) {

		int[] pos = new int[blues.size()]; { int i=0; for (String k : blues) pos[i++] = nodes.getId(k); }
		int[] neg = new int[reds.size()];  { int i=0; for (String k : reds)  neg[i++] = nodes.getId(k); }

		double baselineLoss = makeLoss(srw, uniformParams, startVec, pos, neg, f);
		ParamVector<String,?> baselineGrad = makeGradient(srw, uniformParams, startVec, pos, neg, f);

		double perturb_epsilon = 1e-10;
		for (String feature : new String[]{"tob","fromb","tor","fromr"}) {
			
			ParamVector<String,?> pert = uniformParams.copy();
			pert.put(feature, pert.get(feature)+perturb_epsilon);
			
			srw.clearLoss();
			ParamVector<String,?> epsGrad = makeGradient(srw, pert, startVec, pos, neg, f);
			double newLoss = srw.cumulativeLoss().total();
			

			double truediff = (newLoss-baselineLoss);
			double approxdiff = (perturb_epsilon*baselineGrad.get(feature));
			double percdiff = (truediff) != 0 ? Math.abs(((approxdiff) / (truediff))-1)*100 : 0;
			System.err.println(String.format("%5s  true: %+1.8e  approx: %+1.8e  %%diff: %3.2f%%",
					feature,
					truediff,
					approxdiff, 
					percdiff));
			
			assertEquals(srw.getClass().getName()+" finite difference approximation on "+feature,
					0,
					percdiff,
					10);
		}
	}
	public void setupSrw(SRW srw) {
		srw.setMu(0);
		srw.getOptions().set("apr","alpha","0.1");
		srw.c.apr.maxDepth=10;
		srw.setSquashingFunction(new Exp());
	}
	public ParamVector<String,?> defaultParams() {
		return new SimpleParamVector(new ConcurrentHashMap<String,Double>());
	}
	public void fillParams(SRW srw, ParamVector<String,?> uniformParams) {
		for (String n : new String[] {"fromb","tob","fromr","tor"}) uniformParams.put(n,srw.getSquashingFunction().defaultValue());
	}
	
	@Test
	public void testDprSRW() {
		SRW srw = new DprSRW();
		setupSrw(srw);
		srw.getOptions().set("apr","epsilon","1e-7");
		ParamVector<String,?> p = defaultParams();
		fillParams(srw,p);
		test(srw,p, new DprExampleFactory());
	}
	
	@Test
	public void testL2PosNegLossSRW() {
		SRW 
		srw = new L2SRW();
		setupSrw(srw);
		ParamVector<String,?> p = defaultParams();
		fillParams(srw,p);
		test(srw,p, new PprExampleFactory());
	}
	@Test
	public void testLocalL2PosNegLossSRW() {
		SRW srw = new LocalL2SRW();
		setupSrw(srw);
		ParamVector<String,?> p = new MuParamVector(new ConcurrentHashMap<String,Double>());
		fillParams(srw,p);
		test(srw,p, new PprExampleFactory());
	}
}
