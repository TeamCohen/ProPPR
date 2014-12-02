package edu.cmu.ml.proppr;

import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.learn.SRW;
import edu.cmu.ml.proppr.learn.tools.WeightingScheme;
import edu.cmu.ml.proppr.prove.*;
import edu.cmu.ml.proppr.prove.wam.WamProgram;
import edu.cmu.ml.proppr.prove.wam.Goal;
import edu.cmu.ml.proppr.prove.wam.LogicProgramException;
import edu.cmu.ml.proppr.prove.wam.ProofGraph;
import edu.cmu.ml.proppr.prove.wam.Query;
import edu.cmu.ml.proppr.prove.wam.State;
import edu.cmu.ml.proppr.prove.wam.WamBaseProgram;
import edu.cmu.ml.proppr.prove.wam.WamQueryProgram;
import edu.cmu.ml.proppr.prove.wam.plugins.WamPlugin;
import edu.cmu.ml.proppr.util.Configuration;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.ModuleConfiguration;
import edu.cmu.ml.proppr.util.ParamVector;
import edu.cmu.ml.proppr.util.ParamsFile;
import edu.cmu.ml.proppr.util.ParsedFile;
import edu.cmu.ml.proppr.util.SimpleParamVector;
import edu.cmu.ml.proppr.util.multithreading.Multithreading;
import edu.cmu.ml.proppr.util.multithreading.Transformer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;


/**
 * Contains a main() which executes a series of queries against a
 * ProPPR and saves the results in an output file. Each query should
 * be a single ProPPR goal, but may include other content after a
 * <TAB> character (as in a training file).  The format of the output
 * file is one line for each query, in the format '# proved Q# <TAB>
 * QUERY <TAB> TIME-IN-MILLISEC msec', followed by one line for each
 * solution, in the format 'RANK <TAB> SCORE <TAB> VARIABLE-BINDINGS'.
 */

public class QueryAnswerer {
	private static final Logger log = Logger.getLogger(QueryAnswerer.class);
	protected WamProgram program;
	protected WamPlugin[] plugins;
	protected Prover prover;
	protected boolean normalize;
	protected int nthreads;
	public QueryAnswerer(WamProgram program, WamPlugin[] plugins, Prover prover, boolean normalize, int threads) {
		this.program = program;
		this.plugins = plugins;
		this.prover = prover;
		this.normalize = normalize;
		this.nthreads = Math.max(1, threads);
	}
	
	static class QueryAnswererConfiguration extends ModuleConfiguration {
		boolean normalize;
		//        boolean rerank;

		public QueryAnswererConfiguration(String[] args, int inputFiles, int outputFiles, int constants, int modules) {
			super(args,  inputFiles,  outputFiles,  constants,  modules);
		}

		@Override
		protected void addOptions(Options options, int[] flags) {
			super.addOptions(options, flags);
			options.addOption(
					OptionBuilder
					.withLongOpt("unnormalized")
					.withDescription("Show unnormalized scores for answers")
					.create());
			//            options.addOption(
			//                    OptionBuilder
			//                            .withLongOpt("reranked")
			//                            .withDescription("Cook with unit weights and rerank solutions, instead of cooking with trained weights")
			//                            .create());
		}

		@Override
		protected void retrieveSettings(CommandLine line, int[] flags, Options options) throws IOException {
			super.retrieveSettings(line, flags, options);
			this.normalize = true;
			if (line.hasOption("unnormalized")) this.normalize = false;
			//            this.rerank = false;
			//            if (line.hasOption("reranked")) this.rerank = true;
			if (!line.hasOption("queries")) {
				usageOptions(options, flags,"Missing required option: queries");
			}
		}
	}

	public Map<State,Double> getSolutions(Prover prover, ProofGraph pg) throws LogicProgramException {
		return prover.prove(pg);
	}
	public void addParams(Prover prover, ParamVector<String,?> params, WeightingScheme<Goal> wScheme) {
		prover.setWeighter(InnerProductWeighter.fromParamVec(params, wScheme));
	}

	public String findSolutions(WamProgram program, WamPlugin[] plugins, Prover prover, Query query, boolean normalize, int id) throws LogicProgramException {
		ProofGraph pg = new ProofGraph(query, program, plugins);
		log.info("Querying: "+query);
		long start = System.currentTimeMillis();
		Map<State,Double> dist = getSolutions(prover,pg);
		long end = System.currentTimeMillis();
		// START HERE
		// foreach state if completed do proofgraph.fill()
		Map<Query,Double> solutions = new TreeMap<Query,Double>();
		for (Map.Entry<State, Double> s : dist.entrySet()) {
			if (s.getKey().isCompleted()) {
				solutions.put(pg.fill(s.getKey()), s.getValue());
			}
		}
		if (normalize) {
			log.debug("normalizing");
			solutions = Dictionary.normalize(solutions);
		} else {
			log.debug("not normalizing");
		}
		List<Map.Entry<Query,Double>> solutionDist = Dictionary.sort(solutions);
		//			    List<Map.Entry<String,Double>> solutionDist = Dictionary.sort(Dictionary.normalize(dist));
		log.info("Writing "+solutionDist.size()+" solutions...");
		StringBuilder sb = new StringBuilder("# proved ").append(String.valueOf(id)).append("\t").append(query.toString())
				.append("\t").append((end - start) + " msec\n");
		int rank = 0;
		for (Map.Entry<Query, Double> soln : solutionDist) {
			++rank;
			sb.append(rank + "\t").append(soln.getValue().toString()).append("\t").append(soln.getKey().toString()).append("\n");
		}
		return sb.toString();
	}

	public void findSolutions(File queryFile, File outputFile) throws IOException {
		Multithreading<Query,String> m = new Multithreading<Query,String>(log);
		m.executeJob(
				this.nthreads, 
				new QueryStreamer(queryFile), 
				new Transformer<Query,String>(){
					@Override
					public Callable<String> transformer(Query in, int id) {
						return new Answer(in,id);
					}}, 
				outputFile, 
				Multithreading.DEFAULT_THROTTLE);
	}
	
	//////////////////////// Multithreading scaffold ///////////////////////////
	

	/** Transforms from inputs to outputs
	 * 
	 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
	 */
	private class Answer implements Callable<String> {
		Query query;
		int id;
		public Answer(Query query, int id) {
			this.query = query;
			this.id = id;
		}
		@Override
		public String call() throws Exception {
			try {
				return findSolutions(program, plugins, prover.copy(), query, normalize, id);
			} catch (LogicProgramException e) {
				throw new LogicProgramException("on query "+id,e);
			}
		}
	}
	
	private class QueryStreamer implements Iterable<Query>, Iterator<Query> {
		ParsedFile reader;
		public QueryStreamer(File queryFile) {
			reader = new ParsedFile(queryFile);
		}
		@Override
		public boolean hasNext() {
			return reader.hasNext();
		}

		@Override
		public Query next() {
			String queryString = reader.next().split("\t")[0];
			Query query = Query.parse(queryString);
			return query;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("Can't remove from a file");
		}

		@Override
		public Iterator<Query> iterator() {
			return this;
		}
		
	}

	public static void main(String[] args) throws IOException {
		int inputFiles = Configuration.USE_QUERIES | Configuration.USE_PARAMS;
		int outputFiles = Configuration.USE_ANSWERS;
		int modules = Configuration.USE_PROVER | Configuration.USE_WEIGHTINGSCHEME;
		int constants = Configuration.USE_WAM | Configuration.USE_THREADS;
		QueryAnswererConfiguration c = new QueryAnswererConfiguration(
				args,
				inputFiles, outputFiles, constants, modules);
		System.out.println(c.toString());

		//        QueryAnswerer qa = c.rerank ?
		//                           new RerankingQueryAnswerer((SRW<PosNegRWExample<String>>) c.srw) :
		//                           new QueryAnswerer();
		QueryAnswerer qa = new QueryAnswerer(c.program, c.plugins, c.prover, c.normalize, c.nthreads);
		log.info("Running queries from " + c.queryFile + "; saving results to " + c.solutionsFile);
		if (c.paramsFile != null) {
			ParamsFile file = new ParamsFile(c.paramsFile);
			qa.addParams(c.prover, new SimpleParamVector<String>(Dictionary.load(file)), c.weightingScheme);
			file.check(c);
		}
		qa.findSolutions(c.queryFile, c.solutionsFile);
	}
}
