package edu.cmu.ml.proppr.v1;

import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.learn.SRW;
import edu.cmu.ml.proppr.learn.tools.WeightingScheme;
import edu.cmu.ml.proppr.prove.*;
import edu.cmu.ml.proppr.prove.feat.ComplexFeatureLibrary;
import edu.cmu.ml.proppr.prove.v1.Goal;
import edu.cmu.ml.proppr.prove.v1.InnerProductWeighter;
import edu.cmu.ml.proppr.prove.v1.LogicProgram;
import edu.cmu.ml.proppr.prove.v1.LogicProgramState;
import edu.cmu.ml.proppr.prove.v1.ProPPRLogicProgramState;
import edu.cmu.ml.proppr.prove.v1.Prover;
import edu.cmu.ml.proppr.util.Configuration;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.ExperimentConfiguration;
import edu.cmu.ml.proppr.util.ParamVector;
import edu.cmu.ml.proppr.util.ParamsFile;
import edu.cmu.ml.proppr.util.ParsedFile;
import edu.cmu.ml.proppr.util.SimpleParamVector;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;


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
		try {
			int querynum=0;
			for (String line : reader) {
				querynum++;
				String queryString = line.split("\t")[0];
				queryString = queryString.replaceAll("[(]", ",").replaceAll("\\)","").trim();
				Goal query = Goal.parseGoal(queryString, ",");
				query.compile(program.getSymbolTable());
				log.info("Querying: "+query);
				long start = System.currentTimeMillis();
				Map<LogicProgramState,Double> dist = getSolutions(prover,query,program);
				long end = System.currentTimeMillis();
				Map<String,Double> solutions = Prover.filterSolutions(dist);
				if (normalize) {
					log.debug("normalizing");
					solutions = Dictionary.normalize(solutions);
				} else {
					log.debug("not normalizing");
				}
				List<Map.Entry<String,Double>> solutionDist = Dictionary.sort(solutions);
				//			    List<Map.Entry<String,Double>> solutionDist = Dictionary.sort(Dictionary.normalize(dist));
				log.info("Writing "+solutionDist.size()+" solutions...");

                writer.append("# proved ").append(String.valueOf(querynum)).append("\t").append(query.toSaveString())
                      .append("\t").append((end - start) + " msec");

                writer.newLine();
                int rank = 0;
                for (Map.Entry<String, Double> soln : solutionDist) {
                    ++rank;
                    writer.append(rank + "\t").append(soln.getValue().toString()).append("\t").append(soln.getKey());
                    writer.newLine();
                }
                writer.flush();
            }
        } finally {
            reader.close();
            writer.close();
        }
    }

    public static void main(String[] args) throws IOException {
        QueryAnswererConfiguration c = new QueryAnswererConfiguration(
                args,
                Configuration.USE_DEFAULTS | Configuration.USE_QUERIES | Configuration.USE_OUTPUT |
                Configuration.USE_PARAMS | Configuration.USE_COMPLEX_FEATURES | Configuration.USE_MAXT);

        QueryAnswerer qa = c.rerank ?
                           new RerankingQueryAnswerer((SRW<PosNegRWExample<String>>) c.srw) :
                           new QueryAnswerer();
        log.info("Running queries from " + c.queryFile + "; saving results to " + c.outputFile);
        if (c.paramsFile != null) {
        	ParamsFile file = new ParamsFile(c.paramsFile);
            qa.addParams(c.program, new SimpleParamVector(Dictionary.load(file)), c.weightingScheme);
            file.check(c);
        }
        qa.findSolutions(c.program, c.prover, c.queryFile, c.outputFile, c.normalize);
    }
}
