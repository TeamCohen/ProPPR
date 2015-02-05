package edu.cmu.ml.proppr.prove.wam;

import static org.junit.Assert.*;

import org.junit.Test;

public class QueryTest {

    @Test
    public void testPrinting() {
    	Query q = Query.parse("squeamish(mathilda,Environments)");
    	String s = q.toString();
    	assertTrue("Freshly parsed",s.indexOf("-") < 0);
    	q.variabilize();
    	s = q.toString();
    	assertTrue("Variabilized",s.indexOf("-") < 0);
    }

}
