package edu.cmu.ml.proppr.util;

import static org.junit.Assert.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import edu.cmu.ml.proppr.util.Configuration;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.ParamsFile;

public class ParamsFileTest {
	private static final String PARAMSFILE = "src/testcases/paramsFileTest.wts";
	static final String[] CMDLINE = "--programFiles src/testcases/classifyPredict.wam:src/testcases/classifyPredict.cfacts --prover dpr:1e-5:.02".split(" ");
	Map<String,Double> params;
	
	@Before
	public void setup() {
		params = new SimpleParamVector<String>();
		params.put("a", 0.0);
		params.put("ca", .06);
		params.put("fe",.06);
		params.put("e",0.2);
		params.put("b2",.15);
		params.put("b6",.02);
		params.put("p",.1);
		params.put("zn",.04);
	}
	
	@Test
	public void test() {
		File paramsFile=new File(PARAMSFILE);

		ParamsFile.save(params, paramsFile, null);
		{
			ParamsFile file = new ParamsFile(paramsFile);
			Map<String,Double> loadedParams = Dictionary.load(file);
			Properties props = file.getHeader();
			assertEquals(0,props.stringPropertyNames().size());
			for (String key : loadedParams.keySet()) {
				assertEquals(key+" mismatch",params.get(key), loadedParams.get(key));
			}
			paramsFile.delete();
		}

		ModuleConfiguration c = new ModuleConfiguration(CMDLINE,
				0,0,Configuration.USE_WAM,Configuration.USE_PROVER);
		ParamsFile.save(params, paramsFile, c);
		{
			ParamsFile file = new ParamsFile(paramsFile);
			Map<String,Double> loadedParams = Dictionary.load(file);
			Properties props = file.getHeader();
			props.list(System.out);
			assertEquals("Should have saved header properties this time",3,props.stringPropertyNames().size());
			for (String key : loadedParams.keySet()) {
				assertEquals(key+" mismatch",params.get(key), loadedParams.get(key));
			}
			paramsFile.delete();
		}
	}
	
	@Test
	public void testValidation() {
		ModuleConfiguration c = new ModuleConfiguration(CMDLINE,0,0,Configuration.USE_WAM,Configuration.USE_PROVER);
		File paramsFile=new File(PARAMSFILE);
		ParamsFile.save(params,paramsFile,c);
		
		ParamsFile file = new ParamsFile(paramsFile);
		Map<String,Double> loadedParams = Dictionary.load(file);
		file.check(c);
		ModuleConfiguration c2 = new ModuleConfiguration(new String[]{CMDLINE[0],CMDLINE[1],"--prover","dpr:1e-5:2e-2"},0,0,Configuration.USE_WAM,Configuration.USE_PROVER);
		file.check(c2);
	}

}
