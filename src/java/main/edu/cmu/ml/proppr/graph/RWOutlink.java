package edu.cmu.ml.proppr.graph;

import java.util.HashMap;

public class RWOutlink {
	public final HashMap<String,Double> fd;
	public final int nodeid;
	public RWOutlink(HashMap<String,Double> fd, int v) {
		this.fd = fd;
		this.nodeid = v;
	}
}
