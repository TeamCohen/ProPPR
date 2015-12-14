package edu.cmu.ml.proppr;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.Trainer.ExampleStats;
import edu.cmu.ml.proppr.Trainer.Parse;
import edu.cmu.ml.proppr.Trainer.TraceLosses;
import edu.cmu.ml.proppr.Trainer.Train;
import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.examples.PprExample;
import edu.cmu.ml.proppr.graph.ArrayLearningGraphBuilder;
import edu.cmu.ml.proppr.graph.LearningGraphBuilder;
import edu.cmu.ml.proppr.learn.SRW;
import edu.cmu.ml.proppr.util.Configuration;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.ModuleConfiguration;
import edu.cmu.ml.proppr.util.ParamsFile;
import edu.cmu.ml.proppr.util.ParsedFile;
import edu.cmu.ml.proppr.util.SimpleSymbolTable;
import edu.cmu.ml.proppr.util.SymbolTable;
import edu.cmu.ml.proppr.util.math.ParamVector;
import edu.cmu.ml.proppr.util.math.SimpleParamVector;
import edu.cmu.ml.proppr.util.multithreading.Multithreading;
import edu.cmu.ml.proppr.util.multithreading.NamedThreadFactory;
import edu.cmu.ml.proppr.util.multithreading.WritingCleanup;

public class TransitionGenerator extends Trainer {
	private static final Logger log = Logger.getLogger(TransitionGenerator.class);
	
	public TransitionGenerator(SRW learner, int th) {
		super(learner, th, Multithreading.DEFAULT_THROTTLE);
	}
	
	public void generate(SymbolTable<String> masterFeatures,
			Iterable<String> examples,
			LearningGraphBuilder builder,
			File initialParamVecFile,
			File transitionFile) {
		ParamVector<String,?> initParams = null;
		if (initialParamVecFile != null) {
			log.info("loading initial params from "+initialParamVecFile);
			initParams = new SimpleParamVector<String>(Dictionary.load(new ParsedFile(initialParamVecFile), new ConcurrentHashMap<String,Double>()));
		} else {
			initParams = createParamVector();
		}
		ParamVector<String,?> paramVec = this.masterLearner.setupParams(initParams);
		if (masterFeatures.size()>0) LearningGraphBuilder.setFeatures(masterFeatures);
		
		NamedThreadFactory workingThreads = new NamedThreadFactory("work-");
		NamedThreadFactory cleaningThreads = new NamedThreadFactory("cleanup-");
		ThreadPoolExecutor workingPool;
		ExecutorService cleanPool;
		BufferedWriter w = null;
		try {
			w = new BufferedWriter(new FileWriter(transitionFile));
		} catch (IOException e) {
			throw new IllegalArgumentException("Trouble opening output file "+transitionFile.getAbsolutePath(),e);
		}
		WritingCleanup cleanupGenerator = new WritingCleanup(w,log);
		
		workingPool = new ThreadPoolExecutor(this.nthreads,Integer.MAX_VALUE,10,TimeUnit.SECONDS,new LinkedBlockingQueue<Runnable>(),workingThreads);
		cleanPool = Executors.newSingleThreadExecutor(cleaningThreads);

		// run examples
		int id=1;
		long start = System.currentTimeMillis();
		int countdown=-1; TransitionGenerator notify = null;
		for (String s : examples) {
			if (log.isDebugEnabled()) log.debug("Queue size "+(workingPool.getTaskCount()-workingPool.getCompletedTaskCount()));
			/*
			 * Throttling behavior: as in Trainer
			 */
			if (countdown>0) {
				if (log.isDebugEnabled()) log.debug("Countdown "+countdown);
				countdown--;
			} else if (countdown == 0) {
				if (log.isDebugEnabled()) log.debug("Countdown "+countdown +"; throttling:");
				countdown--;
				notify = null;
				try {
					synchronized(this) {
						if (log.isDebugEnabled()) log.debug("Clearing training queue...");
						while(workingPool.getTaskCount()-workingPool.getCompletedTaskCount() > this.nthreads)
							this.wait();
						if (log.isDebugEnabled()) log.debug("Queue cleared.");
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} else if (workingPool.getTaskCount()-workingPool.getCompletedTaskCount() > 1.5*this.nthreads) {
				if (log.isDebugEnabled()) log.debug("Starting countdown");
				countdown=this.nthreads;
				notify = this;
			}
			Future<PosNegRWExample> parsed = workingPool.submit(new Parse(s, builder, id));
			Future<String> transitionMatrix = workingPool.submit(new Transition(parsed, paramVec, id, notify));
			cleanPool.submit(cleanupGenerator.cleanup(transitionMatrix, id));
			id++;
			start = System.currentTimeMillis();
		}
		
		workingPool.shutdown();
		try {
			workingPool.awaitTermination(7, TimeUnit.DAYS);
			cleanPool.shutdown();
			cleanPool.awaitTermination(7, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		try {
			w.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Transforms from inputs to outputs
	 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
	 *
	 */
	protected class Transition implements Callable<String> {
		Future<PosNegRWExample> in;
		ParamVector<String,?> paramVec;
		int id;
		TransitionGenerator notify;
		public Transition(Future<PosNegRWExample> parsed, ParamVector<String,?> paramVec, int id, TransitionGenerator notify) {
			this.in = parsed;
			this.id = id;
			this.paramVec = paramVec;
			this.notify = notify;
		}
		@Override
		public String call() throws Exception {
			PosNegRWExample defaultEx = in.get();
			if (! (defaultEx instanceof PprExample)) throw new IllegalArgumentException("Transition matrix only valid for PPR-type SRWs");
			PprExample ex = ((PprExample) defaultEx);
			SRW learner = learners.get(Thread.currentThread().getName());
			if (notify != null) synchronized(notify) { notify.notify(); }
			if (log.isDebugEnabled()) log.debug("Transition matrix loading start "+this.id);
//			long start = System.currentTimeMillis();
			learner.initializeFeatures(paramVec, ex.getGraph());
			learner.load(paramVec, ex);
			if (log.isDebugEnabled()) log.debug("Transition matrix loading done "+this.id);
			StringBuilder sb = new StringBuilder();
			sb.append(id);
			sb.append('\t').append(ex.M.length-1);
			for (int uid=1; uid<ex.M.length; uid++) {
				double[] row = ex.M[uid];
				sb.append("\t").append(row.length);
				for(int eid = ex.getGraph().node_near_lo[uid], xvi = 0; eid < ex.getGraph().node_near_hi[uid]; eid++, xvi++) {
					double d = row[xvi];
					int vid = ex.getGraph().edge_dest[eid];
					sb.append("\t").append(vid).append(" ").append(d);
				}
			}
			sb.append("\n");
			return sb.toString();
		}
	}

	public static void main(String[] args) {
		try {
			int inputFiles = Configuration.USE_GROUNDED | Configuration.USE_INIT_PARAMS;
			int outputFiles = Configuration.USE_TRANSITION;
			int constants = Configuration.USE_FORCE | Configuration.USE_THREADS | Configuration.USE_FIXEDWEIGHTS;
			int modules = Configuration.USE_SRW | Configuration.USE_SQUASHFUNCTION;
			ModuleConfiguration c = new ModuleConfiguration(args,inputFiles,outputFiles,constants,modules);
			log.info(c.toString());

			String groundedFile=c.groundedFile.getPath();
			SymbolTable<String> masterFeatures = new SimpleSymbolTable<String>();
			File featureIndex = new File(groundedFile+Grounder.FEATURE_INDEX_EXTENSION);
			if (featureIndex.exists()) {
				log.info("Reading feature index from "+featureIndex.getName()+"...");
				for (String line : new ParsedFile(featureIndex)) {
					masterFeatures.insert(line.trim());
				}
			}
			log.info("Computing transition matrices for "+groundedFile+"...");
			long start = System.currentTimeMillis();
			TransitionGenerator tg = new TransitionGenerator(c.srw,c.nthreads);
			tg.generate(
					masterFeatures,
					new ParsedFile(groundedFile), 
					new ArrayLearningGraphBuilder(), 
					c.initParamsFile,
					c.transitionFile);
			System.out.println("M-computation time: "+(System.currentTimeMillis()-start));
		} catch (Throwable t) {
			t.printStackTrace();
			System.exit(-1);
		}
	}

}
