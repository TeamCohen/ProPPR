package edu.cmu.ml.praprolog.prove;

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
    final public Map<Goal, Double> featuresAsDict(RenamingSubstitution theta, Goal featureInstance) {
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
    final public Map<Goal, Double> featuresAsDict(RenamingSubstitution theta, int renamedP, Goal featureInstance) {
        if (theta == null)
            throw new NullPointerException("input RenamingSubsittution cannot be null");
        if (featureInstance == null)
            throw new NullPointerException("input feature instance Goal cannot be null");

        return featuresAsDict_h(theta, renamedP,
                                theta.applyToGoal(featureInstance, renamedP));
    }

    /**
     * @param theta           will not be null
     * @param unifiedFeatInst will have as many variable arguments satisifed as possible, using the RenamingSubstitution
     * @param renamedP
     */
    protected abstract Map<Goal, Double> featuresAsDict_h(RenamingSubstitution theta, int renamedP, Goal unifiedFeatInst);
}
