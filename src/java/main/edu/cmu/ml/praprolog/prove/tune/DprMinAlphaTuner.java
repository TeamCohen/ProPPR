package edu.cmu.ml.praprolog.prove.tune;


import java.io.File;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.log4j.Logger;

import edu.cmu.ml.praprolog.learn.tools.WeightingScheme;
import edu.cmu.ml.praprolog.prove.v1.Component;
import edu.cmu.ml.praprolog.prove.v1.DprProver;
import edu.cmu.ml.praprolog.prove.v1.Goal;
import edu.cmu.ml.praprolog.prove.v1.InnerProductWeighter;
import edu.cmu.ml.praprolog.prove.v1.LogicProgram;
import edu.cmu.ml.praprolog.prove.v1.MinAlphaException;
import edu.cmu.ml.praprolog.prove.v1.ProPPRLogicProgramState;
import edu.cmu.ml.praprolog.prove.v1.Prover;
import edu.cmu.ml.praprolog.util.Configuration;
import edu.cmu.ml.praprolog.util.CustomConfiguration;
import edu.cmu.ml.praprolog.util.Dictionary;
import edu.cmu.ml.praprolog.util.ParamsFile;
import edu.cmu.ml.praprolog.util.ParsedFile;

public class DprMinAlphaTuner {
	private static final Logger log = Logger.getLogger(DprMinAlphaTuner.class);
	private static final int MAX_TRIES = 50;
	private static final double MIN_DELTA = 1e-10;
	protected LogicProgram program;

	public DprMinAlphaTuner(String[] programFiles, double alpha, Map<String,Double> params, WeightingScheme wScheme) {
		this.program = new LogicProgram(Component.loadComponents(programFiles, alpha, null));
		if (params != null) {
			this.program.setFeatureDictWeighter(
					InnerProductWeighter.fromParamVec(
							params, wScheme));
		}
	}

	public void tune(File dataFile, double start, double epsilon) {
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
				if (!query(p,dataFile)) break;
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

	public boolean query(Prover prover, File queryFile) {
		boolean success = true;
		MinAlphaException a = null;
		ParsedFile reader = null;
		try {
			long start = System.currentTimeMillis();
			reader = new ParsedFile(queryFile);
			for (String line : reader) {
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
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			success=false;
		} finally {
			if (reader != null)
				reader.close();
		}
		if (a != null) throw (a);
		return success;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int flags = (Configuration.USE_DEFAULTS | Configuration.USE_DATA | Configuration.USE_PARAMS) & ~Configuration.USE_PROVER;
		CustomConfiguration c = new CustomConfiguration(args, 
				flags) {
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
		if (c.programFiles == null) Configuration.missing(Configuration.USE_PROGRAMFILES,flags);
		log.info("Tuning with initial alpha "+(Double) c.getCustomSetting(null));

		Map<String,Double> params = null;
		if (c.paramsFile != null) {
			log.info("Using parameter weights from file "+c.paramsFile);
			ParamsFile file = new ParamsFile(c.paramsFile);
			params = Dictionary.load(file);
			file.check(c);
		}
		DprMinAlphaTuner t = new DprMinAlphaTuner(c.programFiles, c.alpha, params, c.weightingScheme);
		t.tune(c.dataFile,(Double) c.getCustomSetting("start"),(Double) c.getCustomSetting("epsilon"));
	}

}
