package edu.cmu.ml.praprolog.util;

public class TimestampedWeight {
//	public TimestampedWeight() {
//		this.k = -1;
//		this.wt = 0.0;
//	}
	public TimestampedWeight(Double value,int k) {
		this.wt = value;
		this.k = k;
	}
	public int k;
	public double wt;
}
