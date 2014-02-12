package edu.cmu.ml.praprolog.prove.feat;

import edu.cmu.ml.praprolog.prove.*;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Malcolm Greaves
 */
public class ComplexFeatureTest {
    private static final Logger log = Logger.getLogger(ComplexFeatureTest.class);

    @Before
    public void setup() {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.WARN);
    }

    @Test
    public void specialWordComplexFeature() throws LogicProgramException {
        final String specialVarName = "special", specialValue = "value";
        final double defaultValue = 1.0;
        final ComplexFeature cf = new SpecialWordCF(
                new LogicProgram(new Component[0]),
                new String[] {specialVarName, specialValue, "" + defaultValue});

        nullCFChecks(cf);

        final RenamingSubstitution theta = new RenamingSubstitution(0);
        final int reanmedP = RenamingSubstitution.RENAMED;
        final Goal specialFeatureInstnace = new Goal(specialVarName, new Argument[] {

        });

        Assert.fail("need to come up with good automated test!");
    }

    static public void nullCFChecks(ComplexFeature cf) throws LogicProgramException {
        try {
            cf.featuresAsDict(null, 0, null);
            Assert.fail("exepcted ComplexFeature.featuresAsDict(null RenamingSubstitution to fail)");
        } catch (NullPointerException e) {}

        try {
            cf.featuresAsDict(new RenamingSubstitution(0), 0, null);
            Assert.fail("exepcted ComplexFeature.featuresAsDict(...null Goal to fail)");
        } catch (NullPointerException e) {}
    }

    static public class SpecialWordCF extends ComplexFeature {
        final public String specialValue;
        final public int specialIndex;
        final public double defaultValue;

        public SpecialWordCF(LogicProgram lp, String[] args) {
            super(lp, args);
            if (args == null || args.length != 3)
                throw new IllegalArgumentException("args cannot be null and must be length 3");
            for (int ai = 0; ai < args.length; ai++) {
                if (args[ai] == null)
                    throw new NullPointerException("arguments[" + ai + "] cannot be null");
            }
            try {
                this.specialIndex = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                throw new NumberFormatException("first argument: \"" + args[0] + "\" is not an int\n" +
                                                e.getMessage());
            }
            this.specialValue = args[1];
            try {
                this.defaultValue = Double.parseDouble(args[2]);
            } catch (NumberFormatException e) {
                throw new NumberFormatException("last argument: \"" + args[2] + "\" is not a double\n" +
                                                e.getMessage());
            }
        }

        @Override
        protected Map<Goal, Double> featuresAsDict_h(Goal unifiedFeatInst) {
            Map<Goal, Double> m = new HashMap<Goal, Double>();
            if (unifiedFeatInst.getArg(specialIndex) instanceof ConstantArgument) {
                ConstantArgument a = (ConstantArgument) unifiedFeatInst.getArg(specialIndex);
                if (a.getName().equals(specialValue)) {
                    m.put(unifiedFeatInst, defaultValue);
                }
            }
            return m;
        }
    }
}
