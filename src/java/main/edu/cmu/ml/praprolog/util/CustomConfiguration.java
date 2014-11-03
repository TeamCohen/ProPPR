package edu.cmu.ml.praprolog.util;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import edu.cmu.ml.praprolog.prove.Prover;

public abstract class CustomConfiguration extends Configuration {

	public CustomConfiguration(String[] args) {
		super(args);
	}

	public CustomConfiguration(String[] args, int flags) {
		super(args, flags);
	}

	public CustomConfiguration(String[] args, Prover dflt) {
		super(args, dflt);
	}

	public CustomConfiguration(String[] args, Prover dflt, int flags) {
		super(args, dflt, flags);
	}
	
	@Override
	protected void addOptions(Options options, int flags) {
		super.addOptions(options,flags);
		this.addCustomOptions(options,flags);
	}

	protected abstract void addCustomOptions(Options options, int flags);
	
	@Override
	protected void retrieveSettings(CommandLine line, int flags, Options options) throws IOException {
		super.retrieveSettings(line, flags, options);
		this.retrieveCustomSettings(line,flags,options);
	}
	
	protected abstract void retrieveCustomSettings(CommandLine line, int flags, Options options);

	public abstract Object getCustomSetting(String name);
}
