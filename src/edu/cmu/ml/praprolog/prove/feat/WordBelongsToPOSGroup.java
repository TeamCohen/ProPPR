package edu.cmu.ml.praprolog.prove.feat;

import edu.cmu.ml.praprolog.prove.*;
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
    }

    /**
     * Checks if the word has a noun type POS tag
     *
     * @author Malcolm
     */
    static public class Noun extends WordBelongsToPOSGroup {

        static protected Logger log = Logger.getLogger(Noun.class);
        static public final String FOUND = "FoundWordWithNounPOSTag";
        static public final double VALUE = 1.0;
        // fields
        protected GoalComponent[] facts;
        protected String goalComponentFunctorLookup;

        /**
         * @param lp
         * @param childargs first arg is the functor that should be looked up in the goal components
         */
        public Noun(LogicProgram lp, String[] childargs) {
            super(lp, childargs);
            this.goalComponentFunctorLookup = childargs[0];
            for (String tag : new String[] {"NN", "NNS", "NNP", "NNPS"}) {
                tags.add(tag);
            }


            List<GoalComponent> f = new ArrayList<GoalComponent>(lp.getComponents().length);
            for (Component c : lp.getComponents()) {
                if (c instanceof GoalComponent) {
                    f.add((GoalComponent) c);
                }
            }
            this.facts = f.toArray(new GoalComponent[f.size()]);
        }

        @Override
        protected Map<Goal, Double> featuresAsDict_h(Goal unifiedFeatInst) {
            log.info("working on unified feature instance: " + unifiedFeatInst);
            // arg 0 == sentence
            // arg 1 == word
            Map<Goal, Double> m = new HashMap<Goal, Double>();
            try {
                final Argument sentence = unifiedFeatInst.getArg(0);
                final Argument word = unifiedFeatInst.getArg(1);
                if (sentence.isConstant() && word.isConstant()) {
                    for (GoalComponent c : facts) {
                        for (Goal g : c.goalsMatching(goalComponentFunctorLookup, 3, sentence)) {
                            if (g.getArg(1).getName().equals(word.getName()) &&
                                tags.contains(g.getArg(2).getName())) {

                                Goal found = new Goal(FOUND, unifiedFeatInst.getArgs());
                                m.put(found, VALUE);
                                log.info("GoalComponent goal: \"" + g + "\" matches, returning \"" + found);
                                return m;
                            }
                        }
                    }
                }

            } catch (Exception e) {
                log.warn("[skip] Exception trying to process unified goal: \"" +
                         unifiedFeatInst + "\"\n" + e);
                throw new RuntimeException(e);
            }
            log.info("nada");
            return m;
        }
    }
}
