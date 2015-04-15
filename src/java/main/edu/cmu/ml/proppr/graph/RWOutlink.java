package edu.cmu.ml.proppr.graph;

import gnu.trove.map.TObjectDoubleMap;
import java.util.HashMap;

public class RWOutlink {
	public final TObjectDoubleMap<String> fd;
	public final HashMap<String,Double> fdJava;
	public final int nodeid;
	public RWOutlink(TObjectDoubleMap<String> fd, int v) {
		this.fd = fd;
		this.fdJava = null;
		this.nodeid = v;
	}
	public RWOutlink(HashMap<String,Double> fd, int v) {
		this.fdJava = fd;
		this.fd = null;
		this.nodeid = v;
	}
}
