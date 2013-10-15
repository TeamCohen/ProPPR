package edu.cmu.ml.praprolog.graph;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class AnnotatedStringGraph extends AnnotatedGraph<String> {
	public AnnotatedStringGraph() {
//		public AnnotatedGraph() {
//		//On map implementation choice:
//		// I used to have a TreeMap here, but something is iffy with
//		// Edge comparators maybe? the map kept dropping keys.
//		features = new HashMap<Edge,List<Feature>>();//new TreeMap<Edge,List<Feature>>(); //
//		featureSet = new TreeSet<String>();
//	}//	public AnyKeyGraph() {
//		near = new TreeMap<String,Map<String,Double>>();
//		total = new TreeMap<String,Double>();
//	}
		features = new HashMap<Edge<String>,List<Feature>>();//new TreeMap<Edge,List<Feature>>(); //
		featureSet = new TreeSet<String>();
		near = new HashMap<String,Map<String,Double>>();
		total = new HashMap<String,Double>();
		
	}

	@Override
	public String keyToId(String key) {
		// TODO Auto-generated method stub
		return key;
	}

	@Override
	public String idToKey(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] keyArray(int length) {
		return new String[length];
	}
}
