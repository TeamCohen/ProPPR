package edu.cmu.ml.proppr;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.graph.ArrayLearningGraphBuilder;
import edu.cmu.ml.proppr.learn.tools.GroundedExampleParser;
import edu.cmu.ml.proppr.util.Configuration;
import edu.cmu.ml.proppr.util.ModuleConfiguration;
import edu.cmu.ml.proppr.util.ParsedFile;
import edu.cmu.ml.proppr.util.multithreading.Cleanup;
import edu.cmu.ml.proppr.util.multithreading.Multithreading;
import edu.cmu.ml.proppr.util.multithreading.Transformer;

public class Diagnostic {
	private static final Logger log = Logger.getLogger(Diagnostic.class);

	public static void main(String[] args) {
		try {
			int inputFiles = Configuration.USE_TRAIN;
			int outputFiles = 0;
			int constants = Configuration.USE_THREADS | Configuration.USE_THROTTLE;
			int modules = 0;
			ModuleConfiguration c = new ModuleConfiguration(args,inputFiles,outputFiles,constants,modules);
			log.info(c.toString());

			String groundedFile=c.queryFile.getPath();
			log.info("Parsing "+groundedFile+"...");
			long start = System.currentTimeMillis();
			Multithreading<String, PosNegRWExample> m = new Multithreading<String, PosNegRWExample>(log);
			final ArrayLearningGraphBuilder b = new ArrayLearningGraphBuilder();
			m.executeJob(c.nthreads, new ParsedFile(groundedFile), 
					new Transformer<String,PosNegRWExample>() {
						@Override
						public Callable<PosNegRWExample> transformer(final String in, final int id) {
							return new Callable<PosNegRWExample>() {
								@Override
								public PosNegRWExample call() throws Exception {
									log.debug("Job start "+id);
									PosNegRWExample ret = GroundedExampleParser.parse(in, b.copy());
									log.debug("Job done "+id);
									return ret;
								}};
						}}, new Cleanup<PosNegRWExample>() {
							@Override
							public Runnable cleanup(final Future<PosNegRWExample> in, final int id) {
								return new Runnable(){
									ArrayList<PosNegRWExample> done = new ArrayList<PosNegRWExample>();
									@Override
									public void run() {
										try {
											done.add(in.get());
										} catch (InterruptedException e) {} catch (ExecutionException e) {}
										log.debug("Cleanup start "+id);
										log.debug("Cleanup done "+id);
									}};
							}}, c.throttle);
			log.info("Finished diagnostic in "+(System.currentTimeMillis()-start)+" ms");
		} catch (Throwable t) {
			t.printStackTrace();
			System.exit(-1);
		}
	}
}
