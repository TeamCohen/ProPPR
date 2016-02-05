package edu.cmu.ml.proppr.util;

public class StatusLogger {
	private static final int DEFAULT_PERIOD_MS = 3000;
	private int period_ms;
	private long start,last;
	public StatusLogger() {
		this(DEFAULT_PERIOD_MS);
		this.start();
	}
	public StatusLogger(int p) {
		this.period_ms = p;
	}
	public void start() {
		this.start = this.last = System.currentTimeMillis();
	}
	public boolean due() { return due(0); }
	public boolean due(int level) {
		long now = System.currentTimeMillis();
		boolean ret = now-last > Math.exp(level)*period_ms;
		if (ret) last = now;
		return ret;
	}
	public long sinceLast() {
		return since(last);
	}
	public long sinceStart() {
		return since(start);
	}
	public long since(long t) {
		return System.currentTimeMillis() - t;
	}
	public long tick() { return last = System.currentTimeMillis(); }
}
