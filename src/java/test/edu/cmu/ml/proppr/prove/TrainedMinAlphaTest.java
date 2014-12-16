package edu.cmu.ml.proppr.prove;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.cmu.ml.proppr.GrounderTest;
import edu.cmu.ml.proppr.examples.GroundedExample;
import edu.cmu.ml.proppr.examples.InferenceExample;
import edu.cmu.ml.proppr.graph.SimpleLearningGraph.SLGBuilder;
import edu.cmu.ml.proppr.learn.tools.GroundedExampleStreamer;
import edu.cmu.ml.proppr.prove.wam.LogicProgramException;
import edu.cmu.ml.proppr.prove.wam.ProofGraph;
import edu.cmu.ml.proppr.prove.wam.Query;
import edu.cmu.ml.proppr.util.Configuration;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.ModuleConfiguration;
import edu.cmu.ml.proppr.util.ParamVector;
import edu.cmu.ml.proppr.util.ParamsFile;
import edu.cmu.ml.proppr.util.ParsedFile;
import edu.cmu.ml.proppr.util.SimpleParamVector;

public class TrainedMinAlphaTest extends MinAlphaTest {
	public static final String PARAMS=GrounderTest.FACTS.replace(".cfacts", ".wts");
	public static final String GROUNDED=GrounderTest.FACTS.replace(".cfacts", ".grounded");
	public static final double ALPHA=0.2;

	@BeforeClass
	public static void train() throws LogicProgramException, IOException {
		ModuleConfiguration c = new ModuleConfiguration(
				(argstr+" --params "+PARAMS+" --grounded "+GROUNDED+" --apr alpha="+ALPHA).split(" "), 
				inputFiles, 
				outputFiles | Configuration.USE_GROUNDED | Configuration.USE_PARAMS, 
				constants, 
				modules | Configuration.USE_GROUNDER | Configuration.USE_TRAINER);

		InferenceExample[] examples = {
				new InferenceExample(Query.parse("predict(dundee,Y)"), 
						new Query[] {Query.parse("predict(dundee,mammal)")}, 
						new Query[] {Query.parse("predict(dundee,bird)")}),
				new InferenceExample(Query.parse("predict(elsie,Y)"), 
						new Query[] {Query.parse("predict(elsie,mammal)")}, 
						new Query[] {Query.parse("predict(elsie,bird)")}),
				new InferenceExample(Query.parse("predict(tweetie,Y)"), 
						new Query[] {Query.parse("predict(tweetie,bird)")}, 
						new Query[] {Query.parse("predict(tweetie,mammal)")}),
				new InferenceExample(Query.parse("predict(foghorn,Y)"), 
						new Query[] {Query.parse("predict(foghorn,bird)")}, 
						new Query[] {Query.parse("predict(foghorn,mammal)")})
		};

		BufferedWriter writer = new BufferedWriter(new FileWriter(c.groundedFile));
		for (InferenceExample ex : examples) {
			ProofGraph pg = new ProofGraph(ex,c.apr,c.program,c.plugins);
			GroundedExample g = c.grounder.groundExample(c.prover, pg);
			writer.write(c.grounder.serializeGroundedExample(pg, g));
		}
		writer.close();

		ParamVector params = c.trainer.train(new GroundedExampleStreamer(new ParsedFile(c.groundedFile), new SLGBuilder()), 
				c.epochs, 
				c.traceLosses);

		ParamsFile.save(params,c.paramsFile, c);
	}
	
	@AfterClass
	public static void cleanup() {
		new File(PARAMS).delete();
		new File(GROUNDED).delete();
	}

	@Override
	public void init(ModuleConfiguration c) {
		c.prover.setWeighter(
				InnerProductWeighter.fromParamVec(
						new SimpleParamVector<String>(Dictionary.load(new ParamsFile(PARAMS))), 
						c.weightingScheme));
		c.apr.alpha=ALPHA;
		
	}

	@Override
	public void rescaleAssertions(double oldAB, double newAB) {
		double foo = 0;
	}
}
