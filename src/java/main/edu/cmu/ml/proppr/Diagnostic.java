package edu.cmu.ml.proppr;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.graph.ArrayLearningGraph;
import edu.cmu.ml.proppr.graph.ArrayLearningGraph.ArrayLearningGraphBuilder;
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
			int constants = Configuration.USE_THREADS;
			int modules = 0;
			ModuleConfiguration c = new ModuleConfiguration(args,inputFiles,outputFiles,constants,modules);
			log.info(c.toString());

			String groundedFile=c.queryFile.getPath();
			log.info("Parsing "+groundedFile+"...");
			long start = System.currentTimeMillis();
			Multithreading<String, Integer> m = new Multithreading<String, Integer>(log);
			final ArrayLearningGraphBuilder b = new ArrayLearningGraph.ArrayLearningGraphBuilder();
			m.executeJob(c.nthreads, new ParsedFile(groundedFile), 
					new Transformer<String,Integer>() {
						@Override
						public Callable<Integer> transformer(final String in, final int id) {
							return new Callable<Integer>() {
								@Override
								public Integer call() throws Exception {
									log.debug("Job start "+id);
									int ret = GroundedExampleParser.parse(in, b.copy()).length();
									log.debug("Job done "+id);
									return ret;
								}};
						}}, new Cleanup<Integer>() {
							@Override
							public Runnable cleanup(final Future<Integer> in, final int id) {
								return new Runnable(){
									@Override
									public void run() {
										try {
											in.get();
										} catch (InterruptedException e) {} catch (ExecutionException e) {}
										log.debug("Cleanup start "+id);
										log.debug("Cleanup done "+id);
									}};
							}}, Multithreading.DEFAULT_THROTTLE);
			log.info("Finished diagnostic in "+(System.currentTimeMillis()-start)+" ms");
		} catch (Throwable t) {
			t.printStackTrace();
			System.exit(-1);
		}
	}
}
