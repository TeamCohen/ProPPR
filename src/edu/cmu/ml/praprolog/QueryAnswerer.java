package edu.cmu.ml.praprolog;

import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.cmu.ml.praprolog.prove.Component;
import edu.cmu.ml.praprolog.prove.Goal;
import edu.cmu.ml.praprolog.prove.InnerProductWeighter;
import edu.cmu.ml.praprolog.prove.LogicProgram;
import edu.cmu.ml.praprolog.prove.LogicProgramState;
import edu.cmu.ml.praprolog.prove.ProPPRLogicProgramState;
import edu.cmu.ml.praprolog.prove.Prover;
import edu.cmu.ml.praprolog.util.Configuration;
import edu.cmu.ml.praprolog.util.Dictionary;
import edu.cmu.ml.praprolog.util.ParsedFile;
import edu.cmu.ml.praprolog.util.SymbolTable;


/**
 * Contains a main() which executes a series of queries against a
 * ProPPR and saves the results in an output file. Each query should
 * be a single ProPPR goal.  The format of the output file is one line
 * for each query, in the format '# proved <TAB> QUERY <TAB>
 * TIME-IN-MILLISEC msec', followed by one line for each solution, in
 * the format 'RANK <TAB> SCORE <TAB> VARIABLE-BINDINGS'.
 **/

public class QueryAnswerer {
    // extends ExampleThawing to use prover and masterProgram,
    // really should be a separate main
    private static final Logger log = Logger.getLogger(QueryAnswerer.class);

    static class QueryAnswererConfiguration extends Configuration {
	boolean normalize;
	public QueryAnswererConfiguration(String[] args, int flags) {
	    super(args, flags);
	}
	@Override 
	protected void addOptions(Options options, int flags) {
	    super.addOptions(options,flags);
	    options.addOption(
			      OptionBuilder
			      .withLongOpt("unnormalized")
			      .withDescription("Show unnormalized scores for answers")
			      .create());
	}
	@Override
	protected void retrieveSettings(CommandLine line, int flags, Options options) {
		super.retrieveSettings(line, flags, options);
		this.normalize = true;
		if (line.hasOption("unnormalized")) this.normalize = false;
	}
    }

    public static void main(String[] args) throws IOException {
	QueryAnswererConfiguration c = 
	    new QueryAnswererConfiguration(args, 
					   Configuration.USE_DEFAULTS|Configuration.USE_QUERIES|Configuration.USE_OUTPUT|Configuration.USE_PARAMS);
	LogicProgram program = new LogicProgram(Component.loadComponents(c.programFiles,c.alpha));
	if (c.paramsFile != null)
	    program.setFeatureDictWeighter(InnerProductWeighter.fromParamVec(Dictionary.load(c.paramsFile)));
	//QueryAnswerer q = new QueryAnswerer(c.prover,program);


	log.info("Running queries from "+c.queryFile+"; saving results to "+c.outputFile);
	findSolutions(program, c.prover, c.queryFile, c.outputFile, c.normalize);
    }

    public static void findSolutions(LogicProgram program, Prover prover, String queryFile, String outputFile, boolean normalize) throws IOException {
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
		Map<LogicProgramState,Double> dist = prover.proveState(program, new ProPPRLogicProgramState(query));
		long end = System.currentTimeMillis();
		Map<String,Double> solutions = Prover.filterSolutions(dist);
		if (normalize) {
		    System.out.println("normalizing");
		    solutions = Dictionary.normalize(solutions);
		} else {
		    System.out.println("not normalizing");
		}
		List<Map.Entry<String,Double>> solutionDist = Dictionary.sort(solutions);
		//			    List<Map.Entry<String,Double>> solutionDist = Dictionary.sort(Dictionary.normalize(dist));
		log.info("Writing "+solutionDist.size()+" solutions...");

		writer.append("# proved ").append(String.valueOf(querynum)).append("\t").append(query.toSaveString()).append("\t").append((end-start) + " msec");

		writer.newLine();
		int rank = 0;
		for (Map.Entry<String,Double> soln : solutionDist) {
		    ++rank;
		    writer.append(rank+"\t").append(soln.getValue().toString()).append("\t").append(soln.getKey());
		    writer.newLine();
		}
	    }
	} finally {
	    reader.close();
	    writer.close();
	}
    }


}
