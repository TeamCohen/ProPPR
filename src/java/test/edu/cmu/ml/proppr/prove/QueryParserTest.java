package edu.cmu.ml.proppr.prove;

import static org.junit.Assert.*;

import org.junit.Test;

import edu.cmu.ml.proppr.prove.wam.ConstantArgument;
import edu.cmu.ml.proppr.prove.wam.Query;
import edu.cmu.ml.proppr.prove.wam.VariableArgument;

public class QueryParserTest {

	public void checkTo0(Query q) {
		assertEquals("query length",1,q.getRhs().length);
		assertEquals("head functor","bob",q.getRhs()[0].getFunctor());
	}
	
	@Test
	public void testArity0() {
		Query q = Query.parse("bob");
		checkTo0(q);
		assertEquals("head arity",0,q.getRhs()[0].getArity());
	}
	
	@Test
	public void testArity1() {
		Query q = Query.parse("bob(joe)");
		System.out.println(q);
		checkTo0(q);
		assertEquals("head arity",1,q.getRhs()[0].getArity());
		assertEquals("head arg0",new ConstantArgument("joe"),q.getRhs()[0].getArg(0));
	}
	
	@Test
	public void testArity2() {
		Query q = Query.parse("bob(joe,X)");
		q.variabilize();
		System.out.println(q);
		checkTo0(q);
		assertEquals("head arity",2,q.getRhs()[0].getArity());
		assertEquals("head arg0",new ConstantArgument("joe"),q.getRhs()[0].getArg(0));
		assertEquals("head arg1",new VariableArgument(-1),q.getRhs()[0].getArg(1));
	}
}
