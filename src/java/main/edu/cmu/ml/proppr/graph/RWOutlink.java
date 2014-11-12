package edu.cmu.ml.proppr.graph;

import gnu.trove.map.TObjectDoubleMap;

public class RWOutlink<F> {
	public final TObjectDoubleMap<F> fd;
	public final int nodeid;
	public RWOutlink(TObjectDoubleMap<F> fd, int v) {
		this.fd = fd;
		this.nodeid = v;
	}

}
