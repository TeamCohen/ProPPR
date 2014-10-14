package edu.cmu.ml.praprolog.prove.feat;

import edu.cmu.ml.praprolog.prove.*;
import edu.cmu.ml.praprolog.prove.v1.Argument;
import edu.cmu.ml.praprolog.prove.v1.Goal;
import edu.cmu.ml.praprolog.prove.v1.LogicProgram;
import edu.cmu.ml.praprolog.prove.v1.LogicProgramException;
import edu.cmu.ml.praprolog.prove.v1.RenamingSubstitution;

import java.util.Map;


/**
 * Implementing classes absolutely must have a (LogicProgram,String[]) constructor.
 * If implementing class does not have said constructor, then it will not be able
 * to interact with ComplexFeatureLibrary -- the init() function of that class
 * will throw a NoSuchMethodException.
 *
 * @author Malcolm Greaves
 */
public abstract class ComplexFeature {

    protected LogicProgram lp;

    public ComplexFeature(LogicProgram lp, String[] childargs) {
        setLogicProgram(lp);

        if (childargs == null)
            throw new NullPointerException("child's arguments cannot be null");
        for (int ai = 0; ai < childargs.length; ai++) {
            String arg = childargs[ai];
            if (arg == null)
                throw new NullPointerException("(string) argument " + ai + " is null");
        }
    }

    public LogicProgram getLogicProgram() {
        return lp;
    }

    public void setLogicProgram(LogicProgram lp) {
        if (lp == null)
            throw new NullPointerException("logic program cannot be null");
        this.lp = lp;
    }

    /**
     * Alias for featuresAsDict(theta, RenamingSubstitution.NOT_RENAMED)
     */
    final public Map<Goal, Double> featuresAsDict(RenamingSubstitution theta, Goal featureInstance)
            throws LogicProgramException {
        return featuresAsDict(theta, RenamingSubstitution.NOT_RENAMED, featureInstance);
    }

    /**
     * Should be thread-safe.
     * Implemented by extended classes using helper function (featuresAsDict_h)..
     *
     * @param theta           cannot be null
     * @param featureInstance cannot be null
     * @param renamedP
     */
    final public Map<Goal, Double> featuresAsDict(RenamingSubstitution theta, int renamedP, Goal featureInstance)
            throws LogicProgramException {
        if (theta == null)
            throw new NullPointerException("input RenamingSubsittution cannot be null");
        if (featureInstance == null)
            throw new NullPointerException("input feature instance Goal cannot be null");

        final Goal unified = theta.applyToGoal(featureInstance, renamedP);
        for (final Argument a : unified.getArgs()) {
            if (!a.isConstant()) {
                throw new LogicProgramException("Error converting features of goal \"" +
                                                unified + "\" with theta " + theta);
            }
        }

        final Map<Goal, Double> m = featuresAsDict_h(unified);

        for (final Map.Entry<Goal, Double> entry : m.entrySet()) {
            final Goal g = entry.getKey();
            for (final Argument a : g.getArgs()) {
                if (!a.isConstant()) {
                    throw new LogicProgramException("Error: ComplexFeature \"" + getClass() +
                                                    "\" returned goal \"" + g + "\" with variable argument");
                }
            }
        }

        return m;
    }

    /**
     * @param unifiedFeatInst (not null) will have as many variable arguments satisifed as possible, using the RenamingSubstitution
     * @return each goal in the mapping must have no variable arguments
     */
    protected abstract Map<Goal, Double> featuresAsDict_h(Goal unifiedFeatInst);
}
