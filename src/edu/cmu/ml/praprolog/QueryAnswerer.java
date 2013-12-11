package edu.cmu.ml.praprolog;

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
import edu.cmu.ml.praprolog.util.SymbolTable;

/**
 * Contains a main() which executes a series of queries against a
 * ProPPR and saves the results in an output file. Each query should
 * be a single ProPPR goal.  The format of the output file is one line
 * for each query, in the format '# proved <TAB> QUERY <TAB>
 * TIME-IN-MILLISEC msec', followed by one line for each solution, in
 * the format 'RANK <TAB> SCORE <TAB> VARIABLE-BINDINGS'.
 **/

public class QueryAnswerer extends ExampleThawing {
	private static final Logger log = Logger.getLogger(QueryAnswerer.class);

	public static void main(String[] args) throws IOException {
		Configuration c = new Configuration(args, Configuration.USE_DEFAULTS | Configuration.USE_QUERIES | Configuration.USE_OUTPUT);
		LogicProgram program = new LogicProgram(Component.loadComponents(c.programFiles,c.alpha));
		if (c.paramsFile != null)
			program.setFeatureDictWeighter(InnerProductWeighter.fromParamVec(
					Dictionary.load(c.paramsFile)));
		QueryAnswerer q = new QueryAnswerer(c.prover,program);
		q.findSolutions(c.queryFile, c.outputFile);
	}

	public QueryAnswerer(Prover p, String[] programFiles, double alpha) {
		super.init(p,new LogicProgram(Component.loadComponents(programFiles,alpha)));
	}

	public QueryAnswerer(Prover prover, LogicProgram program) {
		super.init(prover, program);
	}

	public void findSolutions(String queryFile, String outputFile) throws IOException {
		LineNumberReader reader = new LineNumberReader(new FileReader(queryFile));
		BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
		try {
			for (String line; (line=reader.readLine())!= null;) {
				String queryString = line.split("\t")[0];
				queryString = queryString.replaceAll("[(]", ",").replaceAll("\\)","").trim();
				Goal query = Goal.parseGoal(queryString, ",");
				query.compile(this.masterProgram.getSymbolTable());
				log.info("Querying: "+query);

				
				long start = System.currentTimeMillis();
				Map<LogicProgramState,Double> dist = prover.proveState(this.masterProgram, new ProPPRLogicProgramState(query));
				long end = System.currentTimeMillis();
				List<Map.Entry<String,Double>> solutionDist = Dictionary.sort(Dictionary.normalize(Prover.filterSolutions(dist)));
				//			    List<Map.Entry<String,Double>> solutionDist = Dictionary.sort(Dictionary.normalize(dist));
				log.info("Writing "+solutionDist.size()+" solutions...");
				writer.append("# proved").append("\t").append(query.toSaveString()).append("\t").append((end-start) + " msec");
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
