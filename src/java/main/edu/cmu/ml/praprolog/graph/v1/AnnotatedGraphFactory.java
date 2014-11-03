package edu.cmu.ml.praprolog.graph.v1;

public class AnnotatedGraphFactory<T> {
	public static final String STRING="edu.cmu.ml.praprolog.graph.AnnotatedStringGraph";
	public static final String INT="edu.cmu.ml.praprolog.graph.AnnotatedIntGraph";
	private Class<AnnotatedGraph<T>> clazz;
	public AnnotatedGraphFactory(String classname) {
		try {
			clazz = (Class<AnnotatedGraph<T>>) Class.forName(classname);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public AnnotatedGraph<T> create() {
		try {
			return clazz.newInstance();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
