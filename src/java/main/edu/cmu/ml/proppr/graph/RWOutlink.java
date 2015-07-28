package edu.cmu.ml.proppr.graph;

import java.util.Arrays;
import java.util.HashMap;

public class RWOutlink {
//	public final HashMap<String,Double> fd;
	public final int[] feature_id;
	public final double[] feature_value;
	public final int nodeid;
	public RWOutlink(int[] fid, double[] wt, int v) {
		this.feature_id = Arrays.copyOf(fid,fid.length);
		this.feature_value = Arrays.copyOf(wt,wt.length);
		this.nodeid = v;
	}
	public int labelSize() {
		return feature_id.length;
	}
}
