package edu.cmu.ml.proppr.util.multithreading;

import java.util.concurrent.ThreadFactory;

public class NamedThreadFactory implements ThreadFactory {
	int next=1;
	String name;
	public NamedThreadFactory(String name) {
		this.name = name;
	}
	@Override
	public Thread newThread(Runnable r) {
		return new Thread(r, name+next++);
	}
	public void reset() {
		this.next=1;
	}
}
