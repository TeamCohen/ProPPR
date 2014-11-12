package edu.cmu.ml.proppr.prove.wam;

import java.util.Map;

public class Outlink {
	public State child;
	public Map<Goal,Double> fd;
	public double wt=0.0;
	public Outlink(Map<Goal,Double> features, State state) {
		child = state;
		fd = features;
	}
}
