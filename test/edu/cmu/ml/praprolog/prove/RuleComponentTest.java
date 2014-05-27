package edu.cmu.ml.praprolog.prove;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import edu.cmu.ml.praprolog.prove.Component.Outlink;

public class RuleComponentTest {
    private static final Logger log = Logger.getLogger(RuleComponentTest.class);
    
    @Before
    public void setup() {
		BasicConfigurator.configure(); Logger.getRootLogger().setLevel(Level.WARN);
    }

    @Test
    public void testLoadCompiled() {
    	RuleComponent rc = RuleComponent.loadCompiled("testcases/classify.crules");
    	assertEquals("functor/arity",2,rc.index.size());
    }
    
    @Test
    public void testOutlinks() throws LogicProgramException {
        RuleComponent r = (RuleComponent) RuleComponentTest.makeMemIDB(); r.compile();
        List<Outlink> outlinks = r.outlinks(Prover.parseQuery("mem", "X","l_de"));
        for (Outlink o : outlinks) {
            LogicProgramState s = o.getState();
            Goal g = s.getHeadGoal();
            log.debug(s);
            assertEquals("parts",g.getFunctor());
            assertEquals(3,g.getArgs().length);
            assertTrue("arg 0 must be constant", g.getArg(0).isConstant());
            assertTrue("arg 0 must be variable", g.getArg(1).isVariable());
            assertTrue("arg 0 must be variable", g.getArg(2).isVariable());

        }
    }
    public static Component makeClassifyIDB() {
        RuleComponent p = new RuleComponent();
        Goal isaduck = new Goal("isa","X","duck"),
                isachx = new Goal("isa","X","chicken"),
                isaplat = new Goal("isa","X","platypus"),
                hasFeathers = new Goal("hasFeathers","X"),
                hasBill = new Goal("hasBill","X"),
                quacks = new Goal("quacks","X"),
                laysEggs = new Goal("laysEggs","X"),
                clucks = new Goal("clucks","X"),
                hasBeak = new Goal("hasBeak","X"),
                hasFur = new Goal("hasFur","X"),
                givesMilk = new Goal("givesMilk","X"),
                f_covering = new Goal("covering"),
                f_looks = new Goal("looks"),
                f_noise = new Goal("noise"),
                f_kids = new Goal("kids"),
                f_milk = new Goal("milk");
        p.add(new Rule(isaduck, f_covering, hasFeathers));
        p.add(new Rule(isaduck, f_looks, hasBill));
        p.add(new Rule(isaduck, f_noise, quacks));
        p.add(new Rule(isaduck, f_kids, laysEggs));
        p.add(new Rule(isachx,  f_covering, hasFeathers));
        p.add(new Rule(isachx,  f_noise, clucks));
        p.add(new Rule(isachx,  f_kids, laysEggs));
        p.add(new Rule(isachx,  f_looks, hasBeak));
        p.add(new Rule(isaplat, f_covering, hasFur));
        p.add(new Rule(isaplat, f_looks, hasBill));
        p.add(new Rule(isaplat, f_kids, laysEggs));
        p.add(new Rule(isaplat, f_milk, givesMilk));

        return p;
    }
    public static Component makeMemIDB() {
        RuleComponent p = new RuleComponent();
        Goal mem3list = new Goal("mem3","S1,S2,S3,List".split(",")),
                parts1 = new Goal("parts","List,S1,Tail".split(",")),
                mem2tail2 = new Goal("mem2","S2,S3,Tail".split(",")),
                partsH = new Goal("parts","List,Head,Tail".split(",")),
                mem3tail = new Goal("mem3","S1,S2,S3,Tail".split(",")),
                mem2list = new Goal("mem2","S1,S2,List".split(",")),
                mem2tail = new Goal("mem2","S1,S2,Tail".split(",")),
                memtail2 = new Goal("mem","S2,Tail".split(",")),
                memlist = new Goal("mem","S1,List".split(",")),
                memtail = new Goal("mem","S1,Tail".split(",")),
                f_m3b = new Goal("m3b"),
                f_m2b = new Goal("m2b"),
                f_m3r = new Goal("m3r"),
                f_mb = new Goal("mb"),
                f_mr = new Goal("mr");
        p.add(new Rule(mem3list,f_m3b,parts1,mem2tail2));
        p.add(new Rule(mem3list,f_m3b,partsH,mem3tail));
        p.add(new Rule(mem2list,f_m2b,parts1,memtail2));
        p.add(new Rule(mem2list,f_m3r,partsH,mem2tail));
        p.add(new Rule(memlist,f_mb,parts1));
        p.add(new Rule(memlist,f_mr,partsH,memtail));
        return p;

    }
    
	@Test
	public void inProgram() {
		LogicProgram lp = new LogicProgram(
				GoalComponent.loadCompiled("testcases/family-more.cfacts"), RuleComponent.loadCompiled("testcases/family.crules"));
		Prover p = new DprProver();
		Map<LogicProgramState,Double> result = p.proveState(lp, new ProPPRLogicProgramState(Goal.decompile("sim,katie,-1")));
		for (Map.Entry<LogicProgramState,Double> e : result.entrySet()) {
			if (e.getKey().isSolution()) System.out.println(e.getValue()+"\t"+e.getKey());
		}
	}
}
