package edu.cmu.ml.praprolog.prove.feat;

import edu.cmu.ml.praprolog.prove.Argument;
import edu.cmu.ml.praprolog.prove.Goal;
import edu.cmu.ml.praprolog.prove.LogicProgram;
import org.apache.log4j.Logger;

import java.util.*;


/**
 * ComplexFeature that checks if a Word has a certian POS tag
 *
 * @author Malcolm Greaves
 */
public abstract class WordBelongsToPOSGroup extends ComplexFeature {

    protected Set<String> tags;

    public WordBelongsToPOSGroup(LogicProgram lp, String[] childargs) {
        super(lp, childargs);
        this.tags = new HashSet<String>();
        Collections.addAll(this.tags, childargs);
    }

    /**
     * Checks if the word has a noun type POS tag
     *
     * @author Malcolm
     */
    static public class Noun extends WordBelongsToPOSGroup {

        static protected Logger log = Logger.getLogger(WordBelongsToPOSGroup.class);
        static public final String FOUND = "FoundWordWithNounPOSTag";
        static public final double VALUE = 1.0;

        public Noun(LogicProgram lp, String[] childargs) {
            super(lp, new String[] {"NN", "NNS", "NNP", "NNPS"});
        }

        @Override
        protected Map<Goal, Double> featuresAsDict_h(Goal unifiedFeatInst) {
            Map<Goal, Double> m = new HashMap<Goal, Double>();
            try {
                Argument word = unifiedFeatInst.getArg(0);
                if (word.isConstant()) {


                    if (true) {

                        m.put(new Goal(FOUND, unifiedFeatInst.getArgs()), VALUE);
                    }
                }

            } catch (Exception e) {
                log.warn("[ski[] Exception trying to process unified goal: \"" +
                         unifiedFeatInst + "\"", e);
            }
            return m;
        }
    }
}
