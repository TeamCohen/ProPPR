package edu.cmu.ml.praprolog.prove;

import static org.junit.Assert.*;

import org.junit.Test;

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
//    	GoalComponent g = GoalComponent.loadCompiled("testcases/toy.cfacts");
    	GoalComponent g = GoalComponent.loadCompiled("testcases/classify.cfacts");
    	assertEquals("functors",2,g.indexF.size());
    	assertEquals("functor+arg1",8,g.indexFA1.size());//5
    }
}
