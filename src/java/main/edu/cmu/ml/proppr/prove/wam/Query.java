package edu.cmu.ml.proppr.prove.wam;

import java.util.LinkedList;
import java.util.List;

import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.SimpleSymbolTable;

/**
 * Headless version for queries.
 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
 *
 */
public class Query extends Rule implements Comparable<Query> {

	protected Query(Goal[] rhs, Goal[] features, Goal[] findall) {
		super(null,rhs,features,findall);
	}

	public Query(Goal ... rhs) {
		this(rhs, new Goal[0], new Goal[0]);
	}

	/**
	 * Input a string in prolog form, e.g. "predict(doc1234,X)"; output a Query object.
	 * 
	 * Note that queries can have multiple goals! "predict(doc1234,X),predict(doc5678,Y)" is a valid query.
	 * @param string
	 * @return
	 */
	public static Query parse(String string) {
		char[] array = string.toCharArray();
		LinkedList<Goal> goals = new LinkedList<Goal>();
		int cursor = 0;
		while(cursor < array.length) {
			cursor = goal(array, cursor, goals);
		}
		return new Query(goals.toArray(new Goal[0]));

	}
	public static Goal parseGoal(String string) {
		char[] array = string.toCharArray();
		LinkedList<Goal> goals = new LinkedList<Goal>();
		if (goal(array,0,goals) != array.length) throw new IllegalArgumentException("Bad syntax for goal "+string);
		return goals.getFirst();
	}
	private static int goal(char[] array, int cursor, List<Goal> goals) {
		StringBuilder functor = new StringBuilder();
		cursor = functor(array,cursor,functor);
		if (functor.length() == 0) return cursor;
		Argument[] arguments = new Argument[0];
		if (cursor < array.length && array[cursor] != ',')  {
			LinkedList<Argument> arglist = new LinkedList<Argument>();
			while(cursor < array.length && array[cursor] != ')') {
				//add another argument
				StringBuilder argument = new StringBuilder();
				cursor = argument(array, cursor, argument);
				if (argument.length() == 0) continue;
				arglist.add(new ConstantArgument(argument.toString()));
			}
			arguments = arglist.toArray(arguments);
		}
		goals.add(new Goal(functor.toString(), arguments));
		return cursor;
	}

	private static int argument(char[] array, int cursor, StringBuilder argument) {
		for (int i=cursor; i<array.length; i++) {
			char c = array[i];
			switch(c) {
			case ',': //fallthrough
			case ')': return i+1;
			case ' ': if (argument.length()==0) continue; //fallthrough
			default: argument.append(c);
			}
		}
		return array.length;
	}

	private static int functor(char[] array, int cursor, StringBuilder functor) {
		for (int i=cursor; i<array.length; i++) {
			char c = array[i];
			switch(c) {
			case ' ': continue;
			case ',': //fallthrough
			case '(': return i+1;
			default: functor.append(c);
			}
		}
		return array.length;
	}

	@Override
	public int compareTo(Query o) {
		for (int i=0; i<this.body.length; i++) {
			if (i>=o.body.length) return -1;
			int j = this.body[i].compareTo(o.body[i]);
			if (j!=0) return j;
		}
		return 0;
	}

}
