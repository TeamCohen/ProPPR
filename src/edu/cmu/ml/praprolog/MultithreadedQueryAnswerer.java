package edu.cmu.ml.praprolog;

import edu.cmu.ml.praprolog.learn.SRW;
import edu.cmu.ml.praprolog.learn.tools.PosNegRWExample;
import edu.cmu.ml.praprolog.learn.tools.WeightingScheme;
import edu.cmu.ml.praprolog.prove.*;
import edu.cmu.ml.praprolog.prove.feat.ComplexFeatureLibrary;
import edu.cmu.ml.praprolog.util.Configuration;
import edu.cmu.ml.praprolog.util.Dictionary;
import edu.cmu.ml.praprolog.util.ExperimentConfiguration;
import edu.cmu.ml.praprolog.util.ParamVector;
import edu.cmu.ml.praprolog.util.ParamsFile;
import edu.cmu.ml.praprolog.util.ParsedFile;
import edu.cmu.ml.praprolog.util.SimpleParamVector;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.log4j.Logger;

import edu.cmu.ml.praprolog.util.multithreading.Cleanup;
import edu.cmu.ml.praprolog.util.multithreading.Multithreading;
import edu.cmu.ml.praprolog.util.multithreading.Transformer;
import edu.cmu.ml.praprolog.util.multithreading.WritingCleanup;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


	/**
	 * This is the multithreaded version of QueryAnswerer.
	 * @author William Wang
	 * ww@cmu.edu
	 * 
	 * Contains a main() which executes a series of queries against a
	 * ProPPR and saves the results in an output file. Each query should
	 * be a single ProPPR goal, but may include other content after a
	 * <TAB> character (as in a training file).  The format of the output
	 * file is one line for each query, in the format '# proved Q# <TAB>
	 * QUERY <TAB> TIME-IN-MILLISEC msec', followed by one line for each
	 * solution, in the format 'RANK <TAB> SCORE <TAB> VARIABLE-BINDINGS'.
	 */

public class MultithreadedQueryAnswerer {
	public static final int DEFAULT_CAPACITY = 32;
	public static final float DEFAULT_LOAD = (float) 0.75;
	protected int nthreads = 32;
	protected int throttle;
	private static final Logger log = Logger.getLogger(MultithreadedQueryAnswerer.class);
	    
	static class QueryAnswererConfiguration extends ExperimentConfiguration {
	    boolean normalize;
	    boolean rerank;

	    public QueryAnswererConfiguration(String[] args, int flags) {
	        super(args, flags);
	    }

	    @Override
	    protected void addOptions(Options options, int flags) {
	        super.addOptions(options, flags);
	        options.addOption(
	                OptionBuilder
	                        .withLongOpt("unnormalized")
	                        .withDescription("Show unnormalized scores for answers")
	                        .create());
	        options.addOption(
	                OptionBuilder
	                        .withLongOpt("reranked")
	                        .withDescription("Cook with unit weights and rerank solutions, instead of cooking with trained weights")
	                        .create());
	    }

	        @Override
	    protected void retrieveSettings(CommandLine line, int flags, Options options) {
	        super.retrieveSettings(line, flags, options);
	        this.normalize = true;
	        if (line.hasOption("unnormalized")) this.normalize = false;
	        this.rerank = false;
	        if (line.hasOption("reranked")) this.rerank = true;
	        if (!line.hasOption("queries")) {
	        	usageOptions(options, flags,"Missing required option: queries");
	        }
	    }
	}

	public Map<LogicProgramState,Double> getSolutions(Prover prover,Goal query,LogicProgram program) {
		return prover.proveState(program, new ProPPRLogicProgramState(query));
	}
	
	public void addParams(LogicProgram program, ParamVector params, WeightingScheme wScheme) {
		program.setFeatureDictWeighter(InnerProductWeighter.fromParamVec(params, wScheme));
	}

	public void findSolutions(LogicProgram program, Prover prover, File queryFile, String outputFile, boolean normalize) throws IOException {
		ParsedFile reader = new ParsedFile(queryFile);
		BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
		List<String> queryStrings = new ArrayList<String>();
		
		try {
                    for (String line : reader) {
				String queryString = line.split("\t")[0];
				queryString = queryString.replaceAll("[(]", ",").replaceAll("\\)","").trim();
				queryStrings.add(queryString);
	             }
	       } finally {
	           reader.close();
	       }
	    
		Multithreading<QueryExample,String> m = new Multithreading<QueryExample,String>(log);

	    // run examples through Multithreading
		m.executeJob(
			this.nthreads, 
			new QueryExampleStreamer(queryStrings, prover, program, normalize), 
			new Transformer<QueryExample, String>() {
				@Override
				public Callable<String> transformer(
					QueryExample in, int id) {
					return new QA(in,id);
				}
			}, 
			new WritingCleanup(writer, log) {
			//	@Override
			//	public Runnable cleanup(Future<String> in, int id) {
			//		return new writeSolutions(in,id);
			//	}
			}, 
			this.throttle);
		
		try {
			int querynum=0;
			for (String line : reader) {
				querynum++;
				String queryString = line.split("\t")[0];
				queryString = queryString.replaceAll("[(]", ",").replaceAll("\\)","").trim();


	        }
	    } finally {
	        reader.close();
	        writer.close();
	    }
}

		
		/**
		 * Stream over queries of this class
		 * @author William Wang
		 * ww@cmu.edu
		 *
		 */
		private class QueryExample {
			Prover prover;
			LogicProgram program;
			String query;
			Boolean normalize;
			
			public QueryExample(String query, Prover prover, LogicProgram program, Boolean normalize) {
				this.prover = prover;
				this.program = program;
				this.query = query;
				this.normalize = normalize;
			}
		}


		/**
		 * Transforms from inputs to outputs
		 * @author William Wang ww@cmu.edu
		 *
		 */
		
		private class QA implements Callable<String> {
			QueryExample in;
			int id;
			public QA(QueryExample in, int id) {
				this.in = in;
				this.id = id;
			}
			@Override
			public String call() throws Exception {
				log.debug("Querying on query "+this.id);
				Goal query = Goal.parseGoal(in.query, ",");
				query.compile(in.program.getSymbolTable());
				log.info("Querying: "+query);
				long start = System.currentTimeMillis();
				Map<LogicProgramState,Double> dist = getSolutions(in.prover,query,in.program);
				long end = System.currentTimeMillis();
				Map<String,Double> solutions = Prover.filterSolutions(dist);
				if (in.normalize) {
					log.debug("normalizing");
					solutions = Dictionary.normalize(solutions);
				} else {
					log.debug("not normalizing");
				}
				List<Map.Entry<String,Double>> solutionDist = Dictionary.sort(solutions);
				StringBuilder sb = new StringBuilder();
	            sb.append("# proved ").append("1").append("\t").append(query.toSaveString()).append("\t").append((end - start) + " msec");
	            sb.append(System.getProperty("line.separator"));
                int rank = 0;
                for (Map.Entry<String, Double> soln : solutionDist) {
                    ++rank;
                    sb.append(rank + "\t").append(soln.getValue().toString()).append("\t").append(soln.getKey());
    	            sb.append(System.getProperty("line.separator"));
                }				
				return sb.toString();
			}
		}
		
		/**
		 * Write the solutions to disk
		 * @author William Wang
		 * ww@cmu.edu
		 *
		 */
		private class writeSolutions implements Runnable {
			Future<String> in;
			int id;
			public writeSolutions(Future<String> in, int id) {
				this.in = in;
				this.id = id;
			}
			@Override
			public void run() {
				log.debug("Writing the solution: "+this.id);
			}
		}

		/**
		 * Builds the streamer of all query inputs from the streamer. 
		 * @author William Wang
		 * ww@cmu.edu
		 *
		 */
		private class QueryExampleStreamer implements Iterable<QueryExample>,Iterator<QueryExample> {
			Iterator<String> goals;
			Prover prover;
			LogicProgram program;
			Boolean normalize;

			public QueryExampleStreamer(Iterable<String> goals, Prover prover, LogicProgram program, Boolean normalize) {
				this.goals = goals.iterator();
				this.prover = prover;
				this.program = program;
				this.normalize = normalize;
			}
			@Override
			public Iterator<QueryExample> iterator() {
				return this;
			}

			@Override
			public boolean hasNext() {
				return goals.hasNext();
			}

			@Override
			public QueryExample next() {
				String goal = goals.next();
				return new QueryExample(goal, prover, program, normalize);
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException("No removal of queries during QA!");
			}
			
		}
		

		public void setThreads(int nthreads) {
			this.nthreads = nthreads;
		}
		public void setThrottle(int throttle) {
			this.throttle = throttle;
		}
	    
		

	    public static void main(String[] args) throws IOException {
	        QueryAnswererConfiguration c = new QueryAnswererConfiguration(
	                args,
	                Configuration.USE_DEFAULTS | Configuration.USE_QUERIES | Configuration.USE_OUTPUT |
	                Configuration.USE_PARAMS | Configuration.USE_COMPLEX_FEATURES);

	        MultithreadedQueryAnswerer qa = new MultithreadedQueryAnswerer();
	        log.info("Running queries from " + c.queryFile + "; saving results to " + c.outputFile);
	        if (c.paramsFile != null) {
	        	ParamsFile file = new ParamsFile(c.paramsFile);
	            qa.addParams(c.program, new SimpleParamVector(Dictionary.load(file)), c.weightingScheme);
	            file.check(c);
	        }
	        qa.findSolutions(c.program, c.prover, c.queryFile, c.outputFile, c.normalize);
	    }
	}

