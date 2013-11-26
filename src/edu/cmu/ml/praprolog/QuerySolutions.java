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
import edu.cmu.ml.praprolog.prove.LogicProgram;
import edu.cmu.ml.praprolog.prove.LogicProgramState;
import edu.cmu.ml.praprolog.prove.ProPPRLogicProgramState;
import edu.cmu.ml.praprolog.prove.Prover;
import edu.cmu.ml.praprolog.util.Configuration;
import edu.cmu.ml.praprolog.util.Dictionary;

public class QuerySolutions {
	private static final Logger log = Logger.getLogger(QuerySolutions.class);
	protected Prover prover;
	protected LogicProgram program;

	public QuerySolutions(Prover p, String[] programFiles, double alpha) {
		this.prover = p;
		this.program = new LogicProgram(Component.loadComponents(programFiles,alpha));
	}
	
	public void findSolutions(String queryFile, String outputFile) throws IOException {
		LineNumberReader reader = new LineNumberReader(new FileReader(queryFile));
		BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
		try {
			for (String line; (line=reader.readLine())!= null;) {
				line = line.replaceAll("[(]", ",").replaceAll("\\)","").trim();
				Goal query = Goal.decompile(line);
				log.info("Querying: "+query);
				Map<LogicProgramState,Double> dist = prover.proveState(this.program, new ProPPRLogicProgramState(query));
			    List<Map.Entry<String,Double>> solutionDist = Dictionary.sort(Dictionary.normalize(Prover.filterSolutions(dist)));
//			    List<Map.Entry<String,Double>> solutionDist = Dictionary.sort(Dictionary.normalize(dist));
			    log.info("Writing "+solutionDist.size()+" solutions...");
			    for (Map.Entry<String,Double> soln : solutionDist) {
			    	writer.append(query.toSaveString())
			    		.append("\t").append(soln.getValue().toString())
			    		.append("\t").append(soln.getKey());
			    	writer.newLine();
			    }
			}
		} finally {
			reader.close();
			writer.close();
		}
	}

	public static void main(String[] args) throws IOException {
		Configuration c = new Configuration(args, Configuration.USE_DEFAULTS | Configuration.USE_DATA | Configuration.USE_OUTPUT);
		
		QuerySolutions q = new QuerySolutions(c.prover,c.programFiles,c.alpha);
		q.findSolutions(c.dataFile, c.outputFile);
	}

}
