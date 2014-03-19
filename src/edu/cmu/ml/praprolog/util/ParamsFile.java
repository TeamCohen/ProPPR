package edu.cmu.ml.praprolog.util;

import gnu.trove.iterator.TIntDoubleIterator;
import gnu.trove.map.TIntDoubleMap;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.util.Map;
import java.util.Properties;

public class ParamsFile extends ParsedFile {
	public static final String HEADER_PREFIX="#! ";
	private Properties properties;
	
	public ParamsFile(File file) {
		super(file);
	}

	public ParamsFile(String filename) {
		super(filename);
	}
	
	public ParamsFile(StringReader stringReader) {
		super(stringReader);
	}
	
	private Properties getProperties() {
		if (this.properties == null) 
			this.properties = new Properties();
		return this.properties;
	}
	
	@Override
	protected void processComment(String line) {
		if (!line.startsWith(HEADER_PREFIX)) return;
		try {
			getProperties().load(new StringReader(line.substring(3)));
		} catch (IOException e) {
			throw new IllegalArgumentException("Bad params file syntax at line "+this.getAbsoluteLineNumber()+": "+line);
		}
	}
	
	public String getProperty(String name) {
		return getProperties().getProperty(name);
	}
	public Properties getHeader() {
		if (!this.isClosed()) { 
			throw new IllegalStateException("Bad programmer: read the parsedFile to completion and close it before requesting the header!"); 
		}
		return getProperties(); 
	}



	private static void saveParameter(Writer writer, String paramName, Double paramValue) throws IOException {
		writer.write(String.format("%s\t%.6g\n", paramName,paramValue));
	}
	private static void saveHeader(Writer writer, Configuration config) throws IOException {
		writer.write(HEADER_PREFIX);
		writer.write("weightingScheme=");
		writer.write(config.weightingScheme.toString());
		writer.write("\n");
		
		writer.write(HEADER_PREFIX);
		writer.write("programFiles=");
		writer.write(Dictionary.buildString(config.programFiles, new StringBuilder(), ":", true).toString());
		writer.write("\n");
		
		if (config.prover != null) {
			writer.write(HEADER_PREFIX);
			writer.write("prover=");
			writer.write(config.prover.toString());
			writer.write("\n");
		}
		
	}
	public static void save(Map<String,Double> params, File paramsFile, Configuration config) {
		BufferedWriter writer;
		try {
			writer = new BufferedWriter(new FileWriter(paramsFile));
			// write header
			if (config != null) saveHeader(writer,config);
			// write params
			for (Map.Entry<String,Double>e : params.entrySet()) {
				saveParameter(writer,String.valueOf(e.getKey()),e.getValue());
			}
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static void save(TIntDoubleMap params, File paramsFile, Configuration config) {
		BufferedWriter writer;
		try {
			writer = new BufferedWriter(new FileWriter(paramsFile));
			// write header
			if (config != null) saveHeader(writer,config);
			// write params
			for (TIntDoubleIterator e = params.iterator(); e.hasNext(); ) {
				e.advance();
				saveParameter(writer,String.valueOf(e.key()),e.value());
			}
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
