package edu.cmu.ml.proppr.prove.feat;

import edu.cmu.ml.proppr.prove.feat.ComplexFeature;
import edu.cmu.ml.proppr.prove.feat.ComplexFeatureLibrary;
import edu.cmu.ml.proppr.prove.v1.Component;
import edu.cmu.ml.proppr.prove.v1.Goal;
import edu.cmu.ml.proppr.prove.v1.LogicProgram;
import junit.framework.Assert;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;

/**
 * @author Malcolm Greaves
 */
public class ComplexFeatureLibraryTest {
	private static final Logger log = Logger.getLogger(ComplexFeatureLibraryTest.class);

	@Before
	public void setup() {
		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.WARN);
	}

	@Test
	public void testSimpleLoad() throws Exception {
		final String functor = "TestLoad";
		BufferedReader r = new BufferedReader(new StringReader(
				functor + "=edu.cmu.ml.praprolog.prove.feat.ComplexFeatureLibraryTest$TestLoad"
				));
		LogicProgram lp = new LogicProgram(new Component[0]);

		ComplexFeatureLibrary.init(lp, r);

		r.close();
		Assert.assertNotNull(ComplexFeatureLibrary.getFeature(functor));
		Assert.assertNotNull(ComplexFeatureLibrary.getFeature(ComplexFeatureLibrary.ESCAPE_PREFIX + functor));
	}

	@Test
	public void testArgumentLoad() throws Exception {
		final String functor = "TestArgumentLoad";
		BufferedReader r = new BufferedReader(new StringReader(
				functor + "=edu.cmu.ml.praprolog.prove.feat.ComplexFeatureLibraryTest$TestArgumentLoad,a,b"
				));

		ComplexFeatureLibrary.init(new LogicProgram(new Component[0]), r);
		r.close();
		Assert.assertNotNull(ComplexFeatureLibrary.getFeature(functor));
		Assert.assertNotNull(ComplexFeatureLibrary.getFeature(ComplexFeatureLibrary.ESCAPE_PREFIX + functor));
	}

	@Test(expected = IllegalStateException.class)
	public void failBeforeInitialization() {
		ComplexFeatureLibrary._reset();
		ComplexFeatureLibrary.getFeature("");
	}

	@Test(expected = IllegalArgumentException.class)
	public void failWithEmptyFunctor() throws IOException {
		BufferedReader r = new BufferedReader(new StringReader(
				"" + "=edu.cmu.ml.praprolog.prove.feat.ComplexFeatureLibraryTest$TestArgumentLoad,a,b"
				));
		ComplexFeatureLibrary.init(new LogicProgram(new Component[0]), r);
	}

	@Test(expected = IllegalArgumentException.class)
	public void failWithEmptyClass() throws IOException {
		BufferedReader r = new BufferedReader(new StringReader("wow="));
		ComplexFeatureLibrary.init(new LogicProgram(new Component[0]), r);
	}

	@Test(expected = IllegalArgumentException.class)
	public void failWithBadPropertyFormat() throws IOException {
		BufferedReader r = new BufferedReader(new StringReader("needs an equals sign"));
		ComplexFeatureLibrary.init(new LogicProgram(new Component[0]), r);
	}

	@Test(expected = IllegalArgumentException.class)
	public void failWithNullFeature() throws IOException {
		final String functor = "wow, so much";
		BufferedReader r = new BufferedReader(new StringReader(
				functor +
				"=edu.cmu.ml.praprolog.prove.feat.ComplexFeatureLibraryTest$TestLoad"));

		ComplexFeatureLibrary.init(new LogicProgram(new Component[0]), r);

		ComplexFeatureLibrary.getFeature(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void failWithEmptyFeature() throws IOException {
		final String functor = "wow, so much";
		BufferedReader r = new BufferedReader(new StringReader(
				functor +
				"=edu.cmu.ml.praprolog.prove.feat.ComplexFeatureLibraryTest$TestLoad"));

		ComplexFeatureLibrary.init(new LogicProgram(new Component[0]), r);
		ComplexFeatureLibrary.getFeature("");
	}

	static public class TestLoad extends ComplexFeature {
		public TestLoad(LogicProgram lp, String[] args) { super(lp, args); }

		@Override
		protected Map<Goal, Double> featuresAsDict_h(Goal unifiedFeatInst) {
			throw new RuntimeException("unimplemented on purpose");
		}
	}

	static public class TestArgumentLoad extends ComplexFeature {

		final String arg1, arg2;

		public TestArgumentLoad(LogicProgram lp, String[] args) {
			super(lp, args);
			if (args == null || args.length != 2)
				throw new IllegalArgumentException("cannot have null args; args must have length = 2");
			this.arg1 = args[0];
			this.arg2 = args[1];
		}

		@Override
		protected Map<Goal, Double> featuresAsDict_h(Goal unifiedFeatInst) {
			throw new RuntimeException("unimplemented");
		}
	}
}
