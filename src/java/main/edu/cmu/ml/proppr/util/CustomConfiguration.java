package edu.cmu.ml.proppr.util;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import edu.cmu.ml.proppr.prove.Prover;

public abstract class CustomConfiguration extends ModuleConfiguration {

	public CustomConfiguration(String[] args, int inputFiles, int outputFiles, int constants, int modules) {
		super(args, inputFiles, outputFiles, constants, modules);
	}

	@Override
	protected void addOptions(Options options, int[] flags) {
		super.addOptions(options,flags);
		this.addCustomOptions(options,flags);
	}

	protected abstract void addCustomOptions(Options options, int[] flags);
	
	@Override
	protected void retrieveSettings(CommandLine line, int[] flags, Options options) throws IOException {
		super.retrieveSettings(line, flags, options);
		this.retrieveCustomSettings(line,flags,options);
	}
	
	protected abstract void retrieveCustomSettings(CommandLine line, int[] flags, Options options);

	public abstract Object getCustomSetting(String name);
}
