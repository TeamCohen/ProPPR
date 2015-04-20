package edu.cmu.ml.proppr;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.graph.ArrayLearningGraphBuilder;
import edu.cmu.ml.proppr.graph.LearningGraphBuilder;
import edu.cmu.ml.proppr.graph.LearningGraph.GraphFormatException;

import edu.cmu.ml.proppr.learn.tools.GroundedExampleParser;
import edu.cmu.ml.proppr.util.Configuration;
import edu.cmu.ml.proppr.util.ModuleConfiguration;
import edu.cmu.ml.proppr.util.ParsedFile;
import edu.cmu.ml.proppr.util.multithreading.Cleanup;
import edu.cmu.ml.proppr.util.multithreading.Multithreading;
import edu.cmu.ml.proppr.util.multithreading.Transformer;
import edu.cmu.ml.proppr.util.SimpleParamVector;
import edu.cmu.ml.proppr.util.ParamVector;
import java.util.concurrent.ConcurrentHashMap;
import edu.cmu.ml.proppr.learn.SRW;

public class Diagnostic {
	private static final Logger log = Logger.getLogger(Diagnostic.class);

	public static void main(String[] args) {
		try {
			int inputFiles = Configuration.USE_TRAIN;
			int outputFiles = 0;
			int constants = Configuration.USE_THREADS | Configuration.USE_THROTTLE;
			int modules = Configuration.USE_SRW;
			ModuleConfiguration c = new ModuleConfiguration(args,inputFiles,outputFiles,constants,modules);
			log.info(c.toString());

			String groundedFile=c.queryFile.getPath();
			log.info("Parsing "+groundedFile+"...");
			long start = System.currentTimeMillis();
			
			final ArrayLearningGraphBuilder b = new ArrayLearningGraphBuilder();
			final SRW srw = c.srw;
			final ParamVector params = srw.setupParams(new SimpleParamVector<String>(new ConcurrentHashMap<String,Double>(16,(float) 0.75,24)));
			for (String f : srw.untrainedFeatures()) params.put(f,srw.getWeightingScheme().defaultWeight());
			srw.setEpoch(1);
			srw.clearLoss();
			srw.untrainedFeatures().add("id(restart)");
			srw.untrainedFeatures().add("id(trueLoop)");
			srw.untrainedFeatures().add("id(trueLoopRestart)");
			srw.untrainedFeatures().add("fixedWeight");
			/* all diag tasks up to Srw: */
			Multithreading<String,PosNegRWExample> m = new Multithreading<String,PosNegRWExample>(log);
			m.executeJob(c.nthreads, new ParsedFile(groundedFile), 
					new Transformer<String,PosNegRWExample>() {
						@Override
						public Callable<PosNegRWExample> transformer(final String in, final int id) {
							return new Callable<PosNegRWExample>() {
								@Override
								public PosNegRWExample call() throws Exception {
									try {
									//log.debug("Job start "+id);
									//PosNegRWExample ret = parser.parse(in, b.copy());
									log.debug("Parsing start "+id);
									PosNegRWExample ret = new GroundedExampleParser().parse(in, b.copy());
									log.debug("Parsing done "+id);
									log.debug("Training start "+id);
									srw.trainOnExample(params,ret);
									log.debug("Training done "+id);
									//log.debug("Job done "+id);
									return ret;
									} catch (IllegalArgumentException e) {
										System.err.println("Problem with #"+id);
										e.printStackTrace();
									}
									return null;
								}};
						}}, new Cleanup<PosNegRWExample>() {
							@Override
							public Runnable cleanup(final Future<PosNegRWExample> in, final int id) {
								return new Runnable(){
									//ArrayList<PosNegRWExample> done = new ArrayList<PosNegRWExample>();
									@Override
									public void run() {
										try {
											//done.add(in.get());
											in.get();
										} catch (InterruptedException e) {
										    e.printStackTrace(); 
										} catch (ExecutionException e) {
										    e.printStackTrace();
										}
										log.debug("Cleanup start "+id);
										log.debug("Cleanup done "+id);
									}};
							}}, c.throttle);
			/**/

			/* SrwO:
			   Multithreading<PosNegRWExample,Integer> m = new Multithreading<PosNegRWExample,Integer>(log);
			m.executeJob(c.nthreads, new PosNegRWExampleStreamer(new ParsedFile(groundedFile),new ArrayLearningGraphBuilder()), 
						 new Transformer<PosNegRWExample,Integer>() {
						@Override
						public Callable<Integer> transformer(final PosNegRWExample in, final int id) {
							return new Callable<Integer>() {
								@Override
								public Integer call() throws Exception {
									try {
									//log.debug("Job start "+id);
									//PosNegRWExample ret = parser.parse(in, b.copy());
									log.debug("Training start "+id);
									srw.trainOnExample(params,in);
									log.debug("Training done "+id);
									//log.debug("Job done "+id);
									} catch (IllegalArgumentException e) {
										System.err.println("Problem with #"+id);
										e.printStackTrace();
									}
									return in.length();
								}};
						}}, new Cleanup<Integer>() {
							@Override
							public Runnable cleanup(final Future<Integer> in, final int id) {
								return new Runnable(){
									//ArrayList<PosNegRWExample> done = new ArrayList<PosNegRWExample>();
									@Override
									public void run() {
										try {
											//done.add(in.get());
											in.get();
										} catch (InterruptedException e) {
										    e.printStackTrace(); 
										} catch (ExecutionException e) {
										    e.printStackTrace();
										}
										log.debug("Cleanup start "+id);
										log.debug("Cleanup done "+id);
									}};
							}}, c.throttle);
			*/

			srw.cleanupParams(params,params);
			log.info("Finished diagnostic in "+(System.currentTimeMillis()-start)+" ms");
		} catch (Throwable t) {
			t.printStackTrace();
			System.exit(-1);
		}
	}

	private static class PosNegRWExampleStreamer implements Iterable<PosNegRWExample>,Iterator<PosNegRWExample> {
		Iterator<String> examples;
		ParamVector paramVec;
		LearningGraphBuilder builder;
		int id=0;
		public PosNegRWExampleStreamer(Iterable<String> examples, LearningGraphBuilder builder) {
			this.examples = examples.iterator();
			this.builder = builder;
		}
		@Override
		public Iterator<PosNegRWExample> iterator() {
			return this;
		}

		@Override
		public boolean hasNext() {
			return examples.hasNext();
		}

		@Override
		public PosNegRWExample next() {
			String example = examples.next(); id++;
			try {
			log.debug("Parsing start "+id);
			PosNegRWExample ret = new GroundedExampleParser().parse(example,builder);
			log.debug("Parsing done "+id);
			return ret;
			} catch (GraphFormatException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("No removal of examples permitted during training!");
		}
	}

}
