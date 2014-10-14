package edu.cmu.ml.praprolog.prove.v1;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashSet;

import org.junit.Test;

import edu.cmu.ml.praprolog.prove.v1.Argument;
import edu.cmu.ml.praprolog.prove.v1.Goal;
import edu.cmu.ml.praprolog.util.SymbolTable;

public class GoalTest {

    @Test
    public void testEquals() {
        Goal f = new Goal("milk"), g = new Goal("milk");
        assertTrue(f.equals(g));
        assertTrue(g.equals(f));
        
    }
    @Test
    public void testHashMembership() {
        Goal f = new Goal("eggs"), g = new Goal("eggs");
        
//        System.err.println("f"); 
//        f.hashCode();
//        System.err.println("g"); 
//        g.hashCode();
//        assertEquals("hashcode",f.hashCode(),g.hashCode()); // if this passes
        assertTrue("equals",g.equals(f)); // and this passes
        
        HashSet<Goal> set = new HashSet<Goal>();
        set.add(f);
        
//        boolean has = false;
//        for (Goal q : set) has = has || g.equals(q);
//        assertTrue("set membership by hand",has); // and this passes

        assertTrue("set membership",set.contains(g)); // shouldn't this pass?
    }


    @Test
    public void testDecompile() {
    	Goal g = Goal.decompile("predict,-1,-2 ");
    	for (Argument a : g.getArgs()) {
    		assertTrue(a.toString(),a.isVariable() || !a.getName().startsWith("-"));
    	}
    }
    
    @Test
    public void testCompile() {
    	Goal g = Goal.decompile("samebib,class_338,-1");
    	Goal g2 = Goal.parseGoal("samebib class_338 X");
    	System.out.println(g);
    	System.out.println(g2);
    	assertNotSame("uncompiled strings",g.toString(), g2.toString());
    	assertNotSame("uncompiled objects",g,g2);
    	
    	System.out.println("compiling...");
    	g2.compile(new SymbolTable());
    	System.out.println(g);
    	System.out.println(g2);
    	assertEquals("compiled strings",g.toString(), g2.toString());
    	assertEquals("compiled objects",g,g2);
    }
    
}
