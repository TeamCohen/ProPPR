package edu.cmu.ml.proppr.prove;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.skjegstad.utils.BloomFilter;

import edu.cmu.ml.proppr.learn.tools.SquashingFunction;
import edu.cmu.ml.proppr.prove.wam.Feature;
import edu.cmu.ml.proppr.prove.wam.Goal;
import edu.cmu.ml.proppr.util.Configuration;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.multithreading.NamedThreadFactory;

public abstract class FeatureDictWeighter {
	private static final Logger log = Logger.getLogger(FeatureDictWeighter.class);
	protected Map<Feature, Double> weights;
	protected SquashingFunction squashingFunction;
	protected int numUnknownFeatures = 0;
	protected int numKnownFeatures = 0;
	protected BloomFilter<Feature> unknownFeatures;
	protected BloomFilter<Feature> knownFeatures;
	protected int nthreads;
	public FeatureDictWeighter(SquashingFunction ws) {
		this(ws,new HashMap<Feature,Double>());
	}
	public FeatureDictWeighter(SquashingFunction ws, Map<Feature,Double> w) {
		this.squashingFunction = ws;
		this.weights = w;
		this.unknownFeatures = new BloomFilter<Feature>(.01,Math.max(100, weights.size()));
		this.knownFeatures = new BloomFilter<Feature>(.01,Math.max(100, weights.size()));
		
		Configuration c = Configuration.getInstance(); if (c!=null) nthreads = c.nthreads; else nthreads = -1;
	}
	public void put(Feature goal, double i) {
		weights.put(goal,i);
	}
	public abstract double w(Map<Feature, Double> fd);
	public String listing() {
		return "feature dict weighter <no string available>";
	}
	public SquashingFunction getSquashingFunction() { return squashingFunction; }
	public void countFeature(Feature g) {
		if (this.weights.size() == 0) return;
		if (this.nthreads < 20) synchronous_countFeature(g);
		else asynch_countFeature(g);
	}
	private void synchronous_countFeature(Feature g) {
		if (!this.weights.containsKey(g)) {
			if (!unknownFeatures.contains(g)) {
				unknownFeatures.add(g);
				numUnknownFeatures++;
			}
		} else if (!knownFeatures.contains(g)) {
			knownFeatures.add(g);
			numKnownFeatures++;
		}
	}
	
	private ExecutorService countService = null;
	private int tasks = 0;
	private Object sem = new Object();
	private void asynch_countFeature(Feature g) {
		if (countService==null) setup_asynch();
		tasks++;
		countService.submit(new Runnable() {
			@Override
			public void run() {
				if (!weights.containsKey(g)) {
					if (!unknownFeatures.contains(g)) {
						unknownFeatures.add(g);
						numUnknownFeatures++;
					}
				} else if (!knownFeatures.contains(g)) {
					knownFeatures.add(g);
					numKnownFeatures++;
				}
				tasks--;
				sem.notifyAll();
			}
		});
		if (this.tasks>10*this.nthreads) { //throttle
			log.debug("throttling feature counter");
			while(this.tasks>this.nthreads) { 
				try {
					sem.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} 
			}
			log.debug("done throttling feature counter");
		}
	}
	private void setup_asynch() {
		countService = Executors.newSingleThreadExecutor(new NamedThreadFactory("count-"));
	}
	private void finish_asynch() {
		if (countService == null) return;
		countService.shutdown();
		try {
			countService.awaitTermination(7, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public int seenUnknownFeatures() {
		finish_asynch();
		return numUnknownFeatures;
	}
	public int seenKnownFeatures() {
		finish_asynch();
		return numKnownFeatures;
	}
}
