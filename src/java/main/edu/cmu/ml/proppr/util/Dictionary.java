package edu.cmu.ml.proppr.util;

import edu.cmu.ml.proppr.prove.v1.Argument;
import edu.cmu.ml.proppr.prove.v1.Goal;
import edu.cmu.ml.proppr.prove.v1.LogicProgramState;
import gnu.trove.iterator.TIntDoubleIterator;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.iterator.TObjectDoubleIterator;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;

/**
 * Utility methods for common tasks on Map<String,Double> and Map<String,Map<String,Double>>.
 * @author krivard
 *
 */
public class Dictionary {
	private static final Logger log = Logger.getLogger(Dictionary.class);
	private static double sanitize(Double d, String msg) {
		if (d.isInfinite()) {
			log.warn(d+" at "+msg+"; truncating");
			return (d>0) ? Double.MAX_VALUE : -Double.MAX_VALUE; // does this even work?
		} else if (d.isNaN()) {
			throw new IllegalArgumentException("NaN encountered at "+msg);
		}
		return d;
	}
	/**
	 * Increment the key's value, or set it if the key is new.
	 * @param map
	 * @param key
	 * @param value
	 */
	public static void increment(TIntDoubleMap map, int key, double value) {
		if (!map.containsKey(key)) map.put(key, sanitize(value, String.valueOf(key)));
		else {
			double newvalue = map.get(key)+value;
			map.put(key,sanitize(newvalue, String.valueOf(key)));
		}
	}

	/**
	 * Increment the key's value, or set it if the key is new.
	 * @param map
	 * @param key
	 * @param value
	 */
	public static <K> void increment(TObjectDoubleMap<K> map, K key, double value) {
		if (!map.containsKey(key)) map.put(key, sanitize(value, String.valueOf(key)));
		else {
			double newvalue = map.get(key)+value;
			map.put(key,sanitize(newvalue, String.valueOf(key)));
		}
	}
	/**
	 * Increment the key's value, or set it if the key is new.
	 * Adds a TreeMap if key1 is new.
	 * @param map
	 * @param key1
	 * @param key2
	 * @param value
	 */
	public static void increment(TIntObjectMap<TIntDoubleHashMap> map, int key1, int key2, Double value) {
		if (!map.containsKey(key1)) { map.put(key1,new TIntDoubleHashMap()); }
		TIntDoubleHashMap inner = map.get(key1); 
		if (!inner.containsKey(key2)) { inner.put(key2, sanitize(value, key1+":"+key2)); }
		else {
			double newvalue = inner.get(key2)+value;
			inner.put(key2, sanitize(newvalue, key1+":"+key2));
		}
	}

	public static <K> void increment( TIntObjectMap<TObjectDoubleMap<K>> map, int key1,
			K key2, double value) {
		if (!map.containsKey(key1)) { map.put(key1,new TObjectDoubleHashMap<K>()); }
		TObjectDoubleMap<K> inner = map.get(key1); 
		if (!inner.containsKey(key2)) { inner.put(key2, sanitize(value, key1+":"+key2)); }
		else {
			double newvalue = inner.get(key2)+value;
			inner.put(key2, sanitize(newvalue, key1+":"+key2));
		}
	}

	/**
	 * Return the key's value, or 0.0 if the key is not in this map.
	 * @param map
	 * @param key
	 * @return
	 */
	public static double safeGet(TIntDoubleMap map, int key) {
		return safeGet(map,key,0.0);
	}	
	/**
	 * Return the key's value, or dflt if the key is not in this map.
	 * @param map
	 * @param key
	 * @return
	 */
	public static double safeGet(TIntDoubleMap map, int key, double dflt) {
		if (map.containsKey(key)) return map.get(key);
		return dflt;
	}
	/**
	 * Return the key's value, or provided default if the key is not in this map.
	 * @param map
	 * @param key1
	 * @param key2
	 * @return
	 */
	public static <K> TObjectDoubleMap<K> safeGet(TIntObjectMap<TObjectDoubleMap<K>> map, int key1, TObjectDoubleMap<K> dflt) {
		if (map.containsKey(key1)) {
			return map.get(key1);
		}
		return dflt;
	}
	public static <K> double safeGet(TObjectDoubleMap<K> map, K key) {
		if (map.containsKey(key)) return map.get(key);
		return 0.0;
	}
	/**
	 * Serialize this map to a StringBuilder, using the specified delimiter between key:value pairs.
	 * The string added to the StringBuilder is:
	 *    $delim$key1:$value1$delim$key2:$value2 ... $delim$keyN$valueN
	 * @param map
	 * @param sb
	 * @param delim
	 * @return 
	 */
	public static StringBuilder buildString(TIntDoubleMap map,StringBuilder sb,String delim) {
		for (TIntDoubleIterator e = map.iterator(); e.hasNext(); ) {
			e.advance();
			sb.append(delim).append(e.key()).append(":").append(e.value()); 
		}
		return sb;
	}
	/**
	 * 
	 * Serialize this map to a StringBuilder, using the specified delimiters between key:value pairs.
	 * The string added to the StringBuilder is:
	 *    $delim1$key1:{buildString(map[key1],sb,delim2)} ... $delim1$keyN:{buildString(map[keyN],sb,delim2)}
	 * @param map
	 * @param sb
	 * @param delim1
	 * @param delim2
	 * @return
	 */
	public static StringBuilder buildString(TIntObjectMap<TIntDoubleMap> map, StringBuilder sb, String delim1, String delim2) {
		for(TIntObjectIterator<TIntDoubleMap> e = map.iterator(); e.hasNext(); ) {
			e.advance();
			sb.append(delim1).append(e.key()).append(":");
			buildString(e.value(),sb,delim2);
		}
		return sb;
	}
	public static <K> StringBuilder buildString(TObjectDoubleMap<K> map, StringBuilder sb, String delim) {
		for(TObjectDoubleIterator<K> e = map.iterator(); e.hasNext(); ) {
			e.advance();
			sb.append(delim).append(e.key()).append(":").append(e.value()); 
		}
		return sb;
	}

	public static <K> StringBuilder buildString(Iterable<K> keys, StringBuilder sb, String delim) {
		boolean first=true;
		for (K k : keys) {
			if (first) first=false;
			else sb.append(delim);
			sb.append(k);
		}
		return sb;
	}

	public static StringBuilder buildString(int[] keys, StringBuilder sb, String delim) {
		for (int i : keys) {
			sb.append(delim).append(i);
		}
		return sb;
	}
	public static <T> StringBuilder buildString(T[] keys, StringBuilder sb, String delim) {
		return buildString(keys,sb,delim,true);
	}
	public static <T> StringBuilder buildString(T[] keys, StringBuilder sb, String delim, boolean first) {
		for (T t : keys) { 
			if (first) first = false;
			else sb.append(delim);
			sb.append(t); 
		}
		return sb;
	}

// // removed by krivard 17 mar 2014 - not called anywhere 
//	public static void save(TIntDoubleMap map, String filename) {
//		BufferedWriter writer;
//		try {
//			writer = new BufferedWriter(new FileWriter(filename));
//			for (TIntDoubleIterator e = map.iterator(); e.hasNext(); ) {
//				e.advance();
//				writer.write(String.format("%s\t%f\n", String.valueOf(e.key()),e.value()));
//			}
//			writer.close();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
	public static boolean safeContains(TIntObjectMap<TIntDoubleMap> map,
			int key1, int key2) {
		if (!map.containsKey(key1)) return false;
		if (!map.get(key1).containsKey(key2)) return false;
		return true;
	}
	public static <K> boolean safeContains(
			TIntObjectMap<TObjectDoubleMap<K>> map, int key1, K key2) {
		if (!map.containsKey(key1)) return false;
		if (!map.get(key1).containsKey(key2)) return false;
		return true;
	}

	/**
	 * Increment the key's value, or set it if the key is new.
	 * @param map
	 * @param key
	 * @param value
	 */
	public static <K> void increment(Map<K,Double> map, K key, Double value, String msg) {
		if (!map.containsKey(key)) map.put(key, sanitize(value, msg));
		else {
			double newvalue = map.get(key)+value;
			map.put(key,sanitize(newvalue, String.valueOf(key)));
		}
	}public static <K> void increment(Map<K,Double> map, K key, Double value) { increment(map,key,value,String.valueOf(key)); }
	/**
	 * Increment the key's value, or set it if the key is new.
	 * Adds a TreeMap if key1 is new.
	 * @param map
	 * @param key1
	 * @param key2
	 * @param value
	 */
	public static <K,L> void increment(Map<K,Map<L,Double>> map, K key1, L key2, Double value) {
		if (!map.containsKey(key1)) { map.put(key1,new TreeMap<L,Double>()); }
		Map<L,Double> inner = map.get(key1); 
		if (!inner.containsKey(key2)) { inner.put(key2, sanitize(value, key1+":"+key2)); }
		else {
			double newvalue = inner.get(key2)+value;
			inner.put(key2, sanitize(newvalue, key1+":"+key2));
		}
	}
	/**
	 * Return the key's value, or 0.0 if the key is not in this map.
	 * @param map
	 * @param key
	 * @return
	 */
	public static <K> double safeGet(Map<K,Double> map, K key) {
		if (map.containsKey(key)) return map.get(key);
		return 0.0;
	}
	/**
	 * Return the key's value, or 0.0 if the key is not in this map.
	 * @param map
	 * @param key1
	 * @param key2
	 * @return
	 */
	public static <K,L> double safeGetGet(Map<K,Map<L,Double>> map, K key1, L key2) {
		if (map.containsKey(key1)) {
			return safeGet(map.get(key1),key2);
		}
		return 0.0;
	}
	/**
	 * Return the key's value, or 0.0 if the key is not in this map.
	 * @param map
	 * @param key1
	 * @param key2
	 * @return
	 */
	public static double safeGetGet(TIntObjectMap<TIntDoubleMap> map, int key1, int key2) {
		if (map.containsKey(key1)) {
			return safeGet(map.get(key1),key2);
		}
		return 0.0;
	}
	/**
	 * Return the key's value, or 0.0 if the key is not in this map.
	 * @param map
	 * @param key1
	 * @param key2
	 * @return
	 */
	public static <K> double safeGetGet(TIntObjectMap<TObjectDoubleMap<K>> map, int key1, K key2) {
		if (map.containsKey(key1)) {
			return safeGet(map.get(key1),key2);
		}
		return 0.0;
	}
	/**
	 * Return the key's value, or 0.0 if the key is not in this map.
	 * @param map
	 * @param key1
	 * @param key2
	 * @return
	 */
	public static <K,L,M> M safeGetGet(Map<K,Map<L,M>> map, K key1, L key2, M dflt) {
		if (map.containsKey(key1)) {
			return safeGet(map.get(key1),key2,dflt);
		}
		return dflt;
	}
	/**
	 * Serialize this map to a StringBuilder, using the specified delimiter between key:value pairs.
	 * The string added to the StringBuilder is:
	 *    $delim$key1:$value1$delim$key2:$value2 ... $delim$keyN$valueN
	 * @param map
	 * @param sb
	 * @param delim
	 * @return 
	 */
	public static <K,V> StringBuilder buildString(Map<K,V> map,StringBuilder sb,String delim) {
		for (Map.Entry<K,V>e : map.entrySet()) { sb.append(delim).append(e.getKey()).append(":").append(e.getValue()); }
		return sb;
	}
	/**
	 * 
	 * Serialize this map to a StringBuilder, using the specified delimiters between key:value pairs.
	 * The string added to the StringBuilder is:
	 *    $delim1$key1:{buildString(map[key1],sb,delim2)} ... $delim1$keyN:{buildString(map[keyN],sb,delim2)}
	 * @param map
	 * @param sb
	 * @param delim1
	 * @param delim2
	 * @return
	 */
	public static <K1,K2> StringBuilder buildString(Map<K1,Map<K2,Double>> map, StringBuilder sb, String delim1, String delim2) {
		for(Map.Entry<K1,Map<K2,Double>>e : map.entrySet()) {
			sb.append(delim1).append(e.getKey()).append(":");
			buildString(e.getValue(),sb,delim2);
		}
		return sb;
	}

	public static <K> void save(Map<K,Double> map, String filename) {
		BufferedWriter writer;
		try {
			writer = new BufferedWriter(new FileWriter(filename));
			save(map,writer);
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static <K> void save(Map<K,Double> map, Writer writer) throws IOException {
		for (Map.Entry<K,Double>e : map.entrySet()) {
			writer.write(String.format("%s\t%.6g\n", String.valueOf(e.getKey()),e.getValue()));
		}
	}
	public static <K,L> boolean safeContains(Map<K, Map<L, Double>> map,
			K key1, L key2) {
		if (!map.containsKey(key1)) return false;
		if (!map.get(key1).containsKey(key2)) return false;
		return true;
	}
	public static <K,V> void safeAppend(Map<K, List<V>> map,
			K key, V newVal) {
		if (!map.containsKey(key)) {
			map.put(key, new ArrayList<V>());
		}
		map.get(key).add(newVal);
	}
	public static <K,L,M> void safeAppend(
			Map<K, Map<L, List<M>>> map, K key1, L key2, M newVal) {
		if(!map.containsKey(key1)) {
			map.put(key1, new HashMap<L,List<M>>());
		}
		safeAppend(map.get(key1),key2,newVal);
	}
	public static <K,V> List<V> safeGet(
			Map<K, List<V>> map,
			K key, List<V> dflt) {
		if (map.containsKey(key)) return map.get(key);
		return dflt;
	}
	public static <K,L,M> Map<L,M> safeGet(
			Map<K, Map<L,M>> map,
			K key1, Map<L,M> dflt) {
		if (map.containsKey(key1)) return  map.get(key1);
		return dflt;
	}
	public static <K,L,M> List<M> safeGetGet(
			Map<K, Map<L,List<M>>> map,
			K key1, L key2, List<M> dflt) {
		if (map.containsKey(key1)) return safeGet(map.get(key1),key2, dflt);//map.get(key);
		return dflt;
	}
	public static <K,V> V safeGet(Map<K, V> map, K key, V dflt) {
		if (map.containsKey(key)) return map.get(key);
		return dflt;
	}
	/**
	 * Given a dictionary that represents a sparse numeric vector,
    rescale the values in-place to sum to some desired amount, and return the original (now changed) vector.
	 * @param map
	 * @return
	 */
	public static <K> Map<K, Double> normalize(Map<K, Double> map) {
		double z = 0.0;
		for (Double d : map.values()) z += d;
		for (Map.Entry<K,Double> e : map.entrySet()) e.setValue(e.getValue()/z);
		return map;
	}
	public static Map<String, Double> load(String filename) {
		return load(new ParsedFile(filename));
	}
	public static Map<String,Double> load(ParsedFile file) {
		Map<String,Double> map = new HashMap<String,Double>();
		for (String line : file) {
			String[] parts = line.split("\t");
			if (parts.length != 2) file.parseError();
			map.put(parts[0], Double.parseDouble(parts[1]));
		}
		file.close();
		return map;
	}

	/**
	 * Given a weighted set, return a list of the elements in descending order.
	 * @param map
	 * @return
	 */
	public static <K> List<Map.Entry<K, Double>> sort(Map<K,Double> map) {
		List<Map.Entry<K, Double>> ret = new ArrayList<Map.Entry<K,Double>>();
		ret.addAll(map.entrySet());
		Collections.sort(ret, new Comparator<Map.Entry<K,Double>>() {
			@Override
			public int compare(Entry<K, Double> arg0, Entry<K, Double> arg1) {
				return arg1.getValue().compareTo(arg0.getValue());
			}});
		return ret;
	}
}
