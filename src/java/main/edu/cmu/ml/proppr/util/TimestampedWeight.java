package edu.cmu.ml.proppr.util;

public class TimestampedWeight {
//	public TimestampedWeight() {
//		this.k = -1;
//		this.wt = 0.0;
//	}
	public TimestampedWeight(Double value,long k) {
		this.wt = value;
		this.k = k;
	}
	public long k;
	public double wt;
}
