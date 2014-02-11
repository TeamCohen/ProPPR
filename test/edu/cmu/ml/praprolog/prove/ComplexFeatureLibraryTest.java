package edu.cmu.ml.praprolog.prove;

import junit.framework.Assert;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
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
                functor + "=edu.cmu.ml.praprolog.prove.ComplexFeatureLibraryTest$TestLoad"
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
                functor + "=edu.cmu.ml.praprolog.prove.ComplexFeatureLibraryTest$TestArgumentLoad,a,b"
        ));
        LogicProgram lp = new LogicProgram(new Component[0]);

        ComplexFeatureLibrary.init(lp, r);
        r.close();
        Assert.assertNotNull(ComplexFeatureLibrary.getFeature(functor));
        Assert.assertNotNull(ComplexFeatureLibrary.getFeature(ComplexFeatureLibrary.ESCAPE_PREFIX + functor));
    }

    @Test
    public void properFails() throws Exception {
        try {
            ComplexFeatureLibrary.getFeature("");
            Assert.fail("should have thrown exception because ComplexFeatureLibrary is not initalized");
        } catch (IllegalStateException e) {}

        BufferedReader r = null;
        LogicProgram lp = new LogicProgram(new Component[0]);

        try {

            r = new BufferedReader(new StringReader(
                    "" + "=edu.cmu.ml.praprolog.prove.ComplexFeatureLibraryTest$TestArgumentLoad,a,b"
            ));
            ComplexFeatureLibrary.init(lp, r);
            Assert.fail("zero length functor in init should be caugth");
        } catch (IllegalArgumentException e) {
        } finally {
            if (r != null) r.close();
        }

        try {

            r = new BufferedReader(new StringReader("wow="));
            ComplexFeatureLibrary.init(lp, r);
            Assert.fail("zero length class in init should be caugth");
        } catch (IllegalArgumentException e) {
        } finally {
            if (r != null) r.close();
        }

        try {
            r = new BufferedReader(new StringReader("needs an equals sign"));
            ComplexFeatureLibrary.init(lp, r);
            Assert.fail("no \"=\" should be caught in init");
        } catch (IllegalArgumentException e) {
        } finally {
            if (r != null) r.close();
        }

        final String functor = "wow, so much";
        r = new BufferedReader(new StringReader(
                functor +
                "=edu.cmu.ml.praprolog.prove.ComplexFeatureLibraryTest$TestLoad"));

        ComplexFeatureLibrary.init(lp, r);

        try {
            ComplexFeatureLibrary.getFeature(null);
            Assert.fail("getFeature should throw NPE");
        } catch (NullPointerException e) {}

        try {
            ComplexFeatureLibrary.getFeature("");
            Assert.fail("getFeature should throw exception for zero-length functor");
        } catch (IllegalArgumentException e) {}
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
