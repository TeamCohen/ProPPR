package edu.cmu.ml.proppr.prove.wam;

import edu.cmu.ml.proppr.prove.wam.plugins.WamPlugin;
import edu.cmu.ml.proppr.util.SymbolTable;

public class Feature implements Comparable<Feature> {
	public final String name;
	public Feature(FeatureBuilder f, SymbolTable<String> constants) {
		StringBuilder sb = new StringBuilder();
		if (f.functor.endsWith(WamPlugin.WEIGHTED_SUFFIX)) {
			sb.append(f.functor.substring(0,f.functor.length()-WamPlugin.WEIGHTED_SUFFIX.length()));
		} else sb.append(f.functor);
		if (f.arity>0) {
			sb.append("(");
			for (int i : f.args) {
				sb.append(constants.getSymbol(i)).append(",");
			}
			sb.setCharAt(sb.length()-1, ')');
		}
		this.name = sb.toString();
	}
	public Feature(String key) {
		this.name = key;
	}
	@Override
	public String toString() {
		return this.name;
	}
	@Override
	public int compareTo(Feature o) {
		return this.name.compareTo(o.name);
	}
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Feature)) return false;
		return this.name.equals(((Feature)o).name);
	}
	@Override
	public int hashCode() {
		return this.name.hashCode();
	}
}
