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
	
    private int nUtot = 0;
    private int nKtot = 0;
    private int nUguess = 0;
    private int nKguess = 0;
    private static final int GUESS_FACTOR=3000;
	private void asynch_countFeature(Feature g) {
	    double pGuess = Math.min(1,this.nthreads * this.nthreads / (double) GUESS_FACTOR);
	    pGuess += Math.min(1,numKnownFeatures / (double) this.weights.size());
	    if (pGuess < 0) { throw new IllegalStateException("BAD PGUESS: nK "+numKnownFeatures+" nKt "+nKtot+" w "+this.weights.size()); }
	    if (!this.weights.containsKey(g)) {
		nUtot++;
		double pNew = Math.min(1,numUnknownFeatures/(double)nUtot);
		pGuess += 1.-pNew;
		pGuess = pGuess/3.;
		if (Math.random() < pGuess) {
		    nUguess++;
		    // guess whether we've seen an unknown feature
		    if (Math.random() < pNew || numUnknownFeatures == 0) { //unknownFeatures.add(g); 
			numUnknownFeatures++; }
		} else {
		    // truly compute whether we've seen an unknown feature
		    if (!unknownFeatures.contains(g)) {
			unknownFeatures.add(g);
			numUnknownFeatures++;
		    }
		}
		//if (nUtot % 10000 ==0) { log.info("U pGuess "+pGuess +" pNew "+pNew+" w "+this.weights.size()+" nK "+numKnownFeatures); }
	    } else {
		nKtot++;
		double pNew = Math.min(1,numKnownFeatures/(double)nKtot);
		pGuess += 1.-pNew;
		pGuess = pGuess/3.;
		if (Math.random() < pGuess) {
		    nKguess++;
		    // guess whether we've seen a known feature
		    if (Math.random() < pNew || numKnownFeatures == 0) { //knownFeatures.add(g); 
			numKnownFeatures++; }
		} else {
		    // truly compute whether we've seen a known feature
		    if (!knownFeatures.contains(g)) {
			knownFeatures.add(g);
			numKnownFeatures++;
		    }
		}
		//if (nKtot % 1000000 ==0) { log.info("K pGuess "+pGuess +" pNew "+pNew+" nKt "+nKtot +" nUt "+nUtot); }
	    }
	}
	private void finish_asynch() {
	}
	public int seenUnknownFeatures() {
	    log.info("U Guesses "+nUguess+" / "+nUtot);
		finish_asynch();
		return numUnknownFeatures;
	}
	public int seenKnownFeatures() {
	    log.info("K Guesses "+nKguess+" / "+nKtot);
		finish_asynch();
		return numKnownFeatures;
	}
}
