package edu.cmu.ml.praprolog.graph.v1;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

public class AnnotatedIntGraph extends AnnotatedGraph<Integer> {
	protected Map<String,Integer> keyToId=new TreeMap<String,Integer>();
	protected Map<Integer,String> idToKey=new TreeMap<Integer,String>();
	private static Integer nextKey=1;
	
	public AnnotatedIntGraph() {
		features = new HashMap<Edge<Integer>,List<Feature>>();//new TreeMap<Edge,List<Feature>>(); //
		featureSet = new TreeSet<String>();
		near = new HashMap<Integer,Map<Integer,Double>>();
		total = new HashMap<Integer,Double>();
	}
	@Override
	public Integer keyToId(String key) {
		if (!keyToId.containsKey(key)) {
			synchronized(nextKey) {
				keyToId.put(key, nextKey);
				idToKey.put(nextKey, key);
				nextKey++;
			}
		}
		return keyToId.get(key);
	}
	@Override
	public String idToKey(Integer id) {
		return idToKey.get(id);
	}
	@Override
	public Integer[] keyArray(int length) {
		return new Integer[length];
	}

}
