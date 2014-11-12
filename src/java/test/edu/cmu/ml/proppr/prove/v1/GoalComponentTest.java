package edu.cmu.ml.proppr.prove.v1;

import static org.junit.Assert.*;

import org.junit.Test;

import edu.cmu.ml.proppr.prove.v1.Component;
import edu.cmu.ml.proppr.prove.v1.Goal;
import edu.cmu.ml.proppr.prove.v1.GoalComponent;
import edu.cmu.ml.proppr.prove.v1.GoalComponent.FunctorArityArg1Arg2Key;

public class GoalComponentTest {

    @Test
    public void testKeyHashing() {
        Goal f = new Goal("parts","q,r,s".split(",")),
                g = new Goal("parts","q,r,s".split(","));
        GoalComponent gc = new GoalComponent();
        gc.addFact(f);
        assertTrue(gc.contains(g));
    }

    public static Component makeMemEDB() {
        GoalComponent p = new GoalComponent();
        p.addFact(new Goal("parts","l_zabcde,z,l_abcde".split(",")));
        p.addFact(new Goal("parts","l_abcde,a,l_bcde".split(",")));
        p.addFact(new Goal("parts","l_bcde,b,l_cde".split(",")));
        p.addFact(new Goal("parts","l_cde,c,l_de".split(",")));
        p.addFact(new Goal("parts","l_de,d,l_e".split(",")));
        p.addFact(new Goal("parts","l_e,e,l_".split(",")));
        return p;
    }
    
    public static Component makeClassifyEDB() {
            GoalComponent p = new GoalComponent();
            String[] hfeatures = "hasFeathers hasBill quacks laysEggs".split(" ");
            for (String f : hfeatures) p.addFact(new Goal(f,"howard"));
            String[] elfeatures = "hasFeathers hasBeak clucks laysEggs".split(" ");
            for (String f : elfeatures) p.addFact(new Goal(f,"little"));
            String[] dfeatures = "hasFur hasBill givesMilk laysEggs".split(" ");
            for (String f : dfeatures) p.addFact(new Goal(f,"dundee"));
            String[] tfeatures = "hasFeathers hasBeak laysEggs".split(" ");
            for (String f : tfeatures) p.addFact(new Goal(f,"tweetie"));
            return p;
    }
    
    @Test
    public void testLoadCompiled() {
    	GoalComponent g = GoalComponent.loadCompiled("testcases/toy.cfacts");
    	assertEquals("functors",2,g.indexF.size());
    	assertEquals("functor+arg1",5,g.indexFA1.size());
    }
    
    @Test
    public void testFeatureName() {
    	String label = "testcases/textcattoy/toylabels.cfacts";
		GoalComponent c = new GoalComponent(label);
		assertEquals("'"+label+"'",c.label);
    }
    
    @Test
    public void testTernaryIndex() {
    	GoalComponent g = new GoalComponent("test",true);
    	for (String city : new String[] {"Pittsburgh","Chicago","New York","Philadelphia","New Orleans"}) {
    		for (String context : new String[] {"_ played _","_ is greener than _","flights from _ to _", "cities like _ and _"}) {
    			g.addFact(new Goal("context",city, context, "San Francisco"));
    		}
    	}
    	assertEquals("functors",1, g.indexF.size());
    	assertEquals("functor+arg1",5,g.indexFA1.size());
    	assertEquals("functor+arg2",4,g.indexFA2.size());
    	
    	for (FunctorArityArg1Arg2Key key : g.indexFA1A2.keySet()) {
    		System.err.println(key.functor+":"+key.arg+":"+key.arg2);
    	}
    	
    	assertEquals("functor+arg1+arg2",20,g.indexFA1A2.size());
    }
}
