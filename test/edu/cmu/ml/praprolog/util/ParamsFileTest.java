package edu.cmu.ml.praprolog.util;

import static org.junit.Assert.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

public class ParamsFileTest {
	Map<String,Double> params;
	
	@Before
	public void setup() {
		params = new HashMap<String,Double>();
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
		File paramsFile=new File("testcases/paramsFileTest.wts");

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

		Configuration c = new Configuration("--programFiles testcases/family.crules:testcases/family.cfacts:testcases/family.graph --prover dpr:1e-5:.02".split(" "));
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
		Configuration c = new Configuration("--programFiles testcases/family.crules:testcases/family.cfacts:testcases/family.graph --prover dpr:1e-5:.02".split(" "));
		File paramsFile=new File("testcases/paramsFileTest.wts");
		ParamsFile.save(params,paramsFile,c);
		
		ParamsFile file = new ParamsFile(paramsFile);
		Map<String,Double> loadedParams = Dictionary.load(file);
		file.check(c);
		Configuration c2 = new Configuration("--programFiles testcases/family.crules:testcases/family.cfacts:testcases/family.graph --prover dpr:1e-5:2e-2".split(" "));
		file.check(c2);
	}

}
