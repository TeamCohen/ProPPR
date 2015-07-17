package edu.cmu.ml.proppr.prove.wam;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashSet;

import org.junit.Test;

import edu.cmu.ml.proppr.prove.wam.Argument;
import edu.cmu.ml.proppr.prove.wam.Goal;
import edu.cmu.ml.proppr.prove.wam.Query;
import edu.cmu.ml.proppr.util.SimpleSymbolTable;

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
}
