package edu.cmu.ml.proppr.graph;

import gnu.trove.map.TObjectDoubleMap;

public class RWOutlink {
	public final TObjectDoubleMap<String> fd;
	public final int nodeid;
	public RWOutlink(TObjectDoubleMap<String> fd, int v) {
		this.fd = fd;
		this.nodeid = v;
	}

}
