package edu.cmu.ml.praprolog.prove.tune;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.log4j.Logger;

import edu.cmu.ml.praprolog.prove.Component;
import edu.cmu.ml.praprolog.prove.DprProver;
import edu.cmu.ml.praprolog.prove.Goal;
import edu.cmu.ml.praprolog.prove.InnerProductWeighter;
import edu.cmu.ml.praprolog.prove.LogicProgram;
import edu.cmu.ml.praprolog.prove.LogicProgramState;
import edu.cmu.ml.praprolog.prove.MinAlphaException;
import edu.cmu.ml.praprolog.prove.ProPPRLogicProgramState;
import edu.cmu.ml.praprolog.prove.Prover;
import edu.cmu.ml.praprolog.util.Configuration;
import edu.cmu.ml.praprolog.util.CustomConfiguration;
import edu.cmu.ml.praprolog.util.Dictionary;

public class DprMinAlphaTuner {
	private static final Logger log = Logger.getLogger(DprMinAlphaTuner.class);
	private static final int MAX_TRIES = 50;
	private static final double MIN_DELTA = 1e-10;
	protected LogicProgram program;

	public DprMinAlphaTuner(String[] programFiles, double alpha, String paramsFile) {
		this.program = new LogicProgram(Component.loadComponents(programFiles, alpha));
		if (paramsFile != null) {
			log.info("Using parameter weights from file "+paramsFile);
			this.program.setFeatureDictWeighter(
				InnerProductWeighter.fromParamVec(
						Dictionary.load(paramsFile)));
		}
	}
	
	public void tune(String queryFile, double start, double epsilon) {
		double minalpha=start, del=minalpha;//, rat = (DprProver.EPS_DEFAULT / DprProver.MINALPH_DEFAULT);
		int i;
		boolean hasSuccess=false;
		double lastSuccess=-1;
		for (i=0;i<MAX_TRIES; i++) {
			if(hasSuccess && del<MIN_DELTA) {
				log.info("Minimum delta reached.");
				break;
			}
			if (minalpha > DprProver.MINALPH_DEFAULT) {
				log.info("MinAlpha exceeds maximum threshold.");
				break;
			}
			if (minalpha < epsilon) {
				log.info("MinAlpha below epsilon (minimum threshold).");
				break;
			}
			log.info("Trying minalpha = "+minalpha);
//			DprProver p = new DprProver(minalpha * rat, minalpha);
			DprProver p = new DprProver(epsilon, minalpha);
			this.program.setAlpha(minalpha+epsilon);
			del = del/2;
			try {
				if (!query(p,queryFile)) break;
				hasSuccess = true;
				lastSuccess = minalpha;
				log.info("Succeeded. Increasing alpha...");
				minalpha += del;
			} catch (MinAlphaException e) {
				log.info("Failed. Decreasing alpha...");
				minalpha -= del;
			}
		}
		log.info("Reached minalpha "+lastSuccess+" +/- "+del+" in "+i+" iterations");
	}
	
	public boolean query(Prover prover, String queryFile) {
		LineNumberReader reader=null;
		boolean success = true;
		MinAlphaException a = null;
		try {
			long start = System.currentTimeMillis();
			reader = new LineNumberReader(new FileReader(queryFile));
			for (String line; (line=reader.readLine())!= null;) {
				long now = System.currentTimeMillis();
				if ( now-start > 5000) log.info(reader.getLineNumber()+" queries...");
				String queryString = line.split("\t")[0];
				queryString = queryString.replaceAll("[(]", ",").replaceAll("\\)","").trim();
				Goal query = Goal.parseGoal(queryString, ",");
				query.compile(this.program.getSymbolTable());
				try {
					prover.proveState(this.program, new ProPPRLogicProgramState(query));
				} catch (MinAlphaException e) { a = e;  break; }
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			success=false;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			success=false;
		} finally {
			if (reader != null)
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		if (a != null) throw (a);
		return success;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		CustomConfiguration c = new CustomConfiguration(args, 
				(Configuration.USE_DEFAULTS | Configuration.USE_DATA | Configuration.USE_PARAMS) & ~Configuration.USE_PROVER) {
			public double startAlpha;
			public double epsilon;
			@Override
			protected void addCustomOptions(Options options, int flags) {
				options.addOption(
						OptionBuilder
						.withLongOpt("start")
						.hasArg()
						.withArgName("double")
						.withDescription("Starting value for minAlpha (default "+DprProver.MINALPH_DEFAULT+")")
						.create());
				options.addOption(
						OptionBuilder
						.withLongOpt("eps")
						.hasArg()
						.withArgName("double")
						.withDescription("Epsilon value for the DprProver (default "+DprProver.EPS_DEFAULT+")")
						.create());
				}
			@Override
			protected void retrieveCustomSettings(CommandLine line, int flags,
					Options options) {
				if (line.hasOption("start")) this.startAlpha=Double.parseDouble(line.getOptionValue("start"));
				else startAlpha=DprProver.MINALPH_DEFAULT;
				if (line.hasOption("eps")) this.epsilon=Double.parseDouble(line.getOptionValue("eps"));
				else epsilon=DprProver.EPS_DEFAULT;
			}
			@Override
			public Object getCustomSetting(String name) {
				if ("start".equals(name))
					return startAlpha;
				if ("epsilon".equals(name))
					return epsilon;
				return null;
			}
		};
		log.info("Tuning with initial alpha "+(Double) c.getCustomSetting(null));
		DprMinAlphaTuner t = new DprMinAlphaTuner(c.programFiles,c.alpha, c.paramsFile);
		t.tune(c.dataFile,(Double) c.getCustomSetting("start"),(Double) c.getCustomSetting("epsilon"));
	}

}
