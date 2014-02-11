package edu.cmu.ml.praprolog.prove;

import edu.cmu.ml.praprolog.prove.Component.Outlink;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
    public void testLoadCompiled() {
        RuleComponent rc = RuleComponent.loadCompiled("testcases/classify.crules");
        assertEquals("functor/arity", 2, rc.index.size());
    }

    @Test
    public void testOutlinks() {
        RuleComponent r = (RuleComponent) ComplexFeatureTest.makeMemIDB();
        r.compile();
        List<Outlink> outlinks = r.outlinks(Prover.parseQuery("mem", "X", "l_de"));
        for (Outlink o : outlinks) {
            LogicProgramState s = o.getState();
            Goal g = s.getHeadGoal();
            log.debug(s);
            assertEquals("parts", g.getFunctor());
            assertEquals(3, g.getArgs().length);
            assertTrue("arg 0 must be constant", g.getArg(0).isConstant());
            assertTrue("arg 0 must be variable", g.getArg(1).isVariable());
            assertTrue("arg 0 must be variable", g.getArg(2).isVariable());

        }
    }

    public static Component makeClassifyIDB() {
        RuleComponent p = new RuleComponent();
        Goal isaduck = new Goal("isa", "X", "duck"),
                isachx = new Goal("isa", "X", "chicken"),
                isaplat = new Goal("isa", "X", "platypus"),
                hasFeathers = new Goal("hasFeathers", "X"),
                hasBill = new Goal("hasBill", "X"),
                quacks = new Goal("quacks", "X"),
                laysEggs = new Goal("laysEggs", "X"),
                clucks = new Goal("clucks", "X"),
                hasBeak = new Goal("hasBeak", "X"),
                hasFur = new Goal("hasFur", "X"),
                givesMilk = new Goal("givesMilk", "X"),
                f_covering = new Goal("covering"),
                f_looks = new Goal("looks"),
                f_noise = new Goal("noise"),
                f_kids = new Goal("kids"),
                f_milk = new Goal("milk");
        p.add(new Rule(isaduck, f_covering, hasFeathers));
        p.add(new Rule(isaduck, f_looks, hasBill));
        p.add(new Rule(isaduck, f_noise, quacks));
        p.add(new Rule(isaduck, f_kids, laysEggs));
        p.add(new Rule(isachx, f_covering, hasFeathers));
        p.add(new Rule(isachx, f_noise, clucks));
        p.add(new Rule(isachx, f_kids, laysEggs));
        p.add(new Rule(isachx, f_looks, hasBeak));
        p.add(new Rule(isaplat, f_covering, hasFur));
        p.add(new Rule(isaplat, f_looks, hasBill));
        p.add(new Rule(isaplat, f_kids, laysEggs));
        p.add(new Rule(isaplat, f_milk, givesMilk));

        return p;
    }

    public static Component makeMemIDB() {
        RuleComponent p = new RuleComponent();
        Goal mem3list = new Goal("mem3", "S1,S2,S3,List".split(",")),
                parts1 = new Goal("parts", "List,S1,Tail".split(",")),
                mem2tail2 = new Goal("mem2", "S2,S3,Tail".split(",")),
                partsH = new Goal("parts", "List,Head,Tail".split(",")),
                mem3tail = new Goal("mem3", "S1,S2,S3,Tail".split(",")),
                mem2list = new Goal("mem2", "S1,S2,List".split(",")),
                mem2tail = new Goal("mem2", "S1,S2,Tail".split(",")),
                memtail2 = new Goal("mem", "S2,Tail".split(",")),
                memlist = new Goal("mem", "S1,List".split(",")),
                memtail = new Goal("mem", "S1,Tail".split(",")),
                f_m3b = new Goal("m3b"),
                f_m2b = new Goal("m2b"),
                f_m3r = new Goal("m3r"),
                f_mb = new Goal("mb"),
                f_mr = new Goal("mr");
        p.add(new Rule(mem3list, f_m3b, parts1, mem2tail2));
        p.add(new Rule(mem3list, f_m3b, partsH, mem3tail));
        p.add(new Rule(mem2list, f_m2b, parts1, memtail2));
        p.add(new Rule(mem2list, f_m3r, partsH, mem2tail));
        p.add(new Rule(memlist, f_mb, parts1));
        p.add(new Rule(memlist, f_mr, partsH, memtail));
        return p;

    }

    @Test
    public void inProgram() {
        LogicProgram lp = new LogicProgram(
                GoalComponent.loadCompiled("testcases/family-more.cfacts"), RuleComponent
                .loadCompiled("testcases/family.crules"));
        Prover p = new DprProver();
        Map<LogicProgramState, Double> result = p
                .proveState(lp, new ProPPRLogicProgramState(Goal.decompile("sim,katie,-1")));
        for (Map.Entry<LogicProgramState, Double> e : result.entrySet()) {
            if (e.getKey().isSolution()) System.out.println(e.getValue() + "\t" + e.getKey());
        }
    }

    static public void nullCFChecks(ComplexFeature cf) {
        try {
            cf.featuresAsDict(null, 0, null);
            Assert.fail("exepcted ComplexFeature.featuresAsDict(null RenamingSubstitution to fail)");
        } catch (NullPointerException e) {}

        try {
            cf.featuresAsDict(new RenamingSubstitution(0), 0, null);
            Assert.fail("exepcted ComplexFeature.featuresAsDict(...null Goal to fail)");
        } catch (NullPointerException e) {}
    }

    @Test
    public void specialWordComplexFeature() {
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


    }

    static public class SpecialWordCF extends ComplexFeature {

        final public String specialVarName, specialValue;
        final public double defaultValue;

        public SpecialWordCF(LogicProgram lp, String[] args) {
            super(lp, args);
            if (args == null || args.length != 3)
                throw new IllegalArgumentException("args cannot be null and must be length 3");
            for (int ai = 0; ai < args.length; ai++) {
                if (args[ai] == null)
                    throw new NullPointerException("arguments[" + ai + "] cannot be null");
            }
            this.specialVarName = args[0];
            this.specialValue = args[1];
            try {
                this.defaultValue = Double.parseDouble(args[2]);
            } catch (NumberFormatException e) {
                throw new NumberFormatException("last argument: \"" + args[2] + "\" is not a double\n" +
                                                e.getMessage());
            }
        }

        @Override
        protected Map<Goal, Double> featuresAsDict_h(RenamingSubstitution theta, int renamedP, Goal unifiedFeatInst) {
            Map<Goal, Double> m = new HashMap<Goal, Double>();
            for (int ai = 0; ai < unifiedFeatInst.args.length; ai++) {
                Argument a = unifiedFeatInst.args[ai];
                if (a instanceof VariableArgument && a.getName().equals(specialVarName)) {
                    Goal g = new Goal(unifiedFeatInst.getFunctor(),
                                      new Argument[unifiedFeatInst.args.length]);
                    System.arraycopy(unifiedFeatInst.args, 0, g.args, 0, g.args.length);
                    g.args[ai] = new ConstantArgument(specialValue);
                    m.put(g, defaultValue);
                }
            }
            return m;
        }
    }
}
