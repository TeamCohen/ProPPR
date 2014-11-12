package edu.cmu.ml.proppr.learn;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.examples.RWExample;
import edu.cmu.ml.proppr.graph.LearningGraph;
import edu.cmu.ml.proppr.learn.tools.LossData;
import edu.cmu.ml.proppr.learn.tools.ReLUWeightingScheme;
import edu.cmu.ml.proppr.learn.tools.TanhWeightingScheme;
import edu.cmu.ml.proppr.learn.tools.WeightingScheme;
import edu.cmu.ml.proppr.learn.tools.LossData.LOSS;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.ParamVector;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.procedure.TIntDoubleProcedure;
import gnu.trove.procedure.TIntProcedure;
import gnu.trove.procedure.TObjectDoubleProcedure;
import gnu.trove.procedure.TObjectProcedure;

/**
 *  Supervised random walk with reset on a graph, following
 *  Backstrom and Leskovec's 2011 WSDM paper, but using SGD instead of
 *  their LBG optimization scheme, and assuming that all restart links
 *  are explicitly represented in the graph.
 *  
 * @author wcohen,krivard,yww
 *
 */
public class SRW<F,E extends RWExample<F>> {
	private static final Logger log = Logger.getLogger(SRW.class);
	private static Random random = new Random();
	public static void seed(long seed) { random.setSeed(seed); }
	protected static final int NUM_EPOCHS = 5;
	public static final int DEFAULT_MAX_T=10;
	public static final double DEFAULT_MU=.001;
	public static final double DEFAULT_ETA=1.0;
	public static final double DEFAULT_DELTA=0.5;
	public static final int DEFAULT_RATE_LENGTH = 1;
	public static WeightingScheme DEFAULT_WEIGHTING_SCHEME() { return new ReLUWeightingScheme(); }
	protected double mu;
	protected int maxT;
	protected double eta;
	protected double delta;
	protected int epoch;
	protected Set<F> untrainedFeatures;
	protected WeightingScheme<F> weightingScheme;
	public SRW() { this(DEFAULT_MAX_T); }
	public SRW(int maxT) { this(maxT, DEFAULT_MU, DEFAULT_ETA, DEFAULT_WEIGHTING_SCHEME(), DEFAULT_DELTA); }
	public SRW(int maxT, double mu, double eta, WeightingScheme<F> wScheme, double delta) {
		this.maxT = maxT;
		this.mu = mu;
		this.eta = eta;
		this.epoch = 1;
		this.delta = delta;
		this.untrainedFeatures = new TreeSet<F>();
		this.weightingScheme = wScheme;
	}


	/**
	 * For each feature in the graph which is not already in the parameter vector,
	 * initialize the parameter value to a weight near 1.0, slightly randomized to avoid symmetry.
	 * @param graph
	 * @param p Edge parameter vector mapping edge feature names to nonnegative values.
	 */
	public void addDefaultWeights(LearningGraph<F> graph, ParamVector<F,?> p) {
		for (F f : graph.getFeatureSet()) {
			if (!p.containsKey(f)) {
				p.put(f,weightingScheme.defaultWeight()+0.01*random.nextDouble());
			}
		}
	}
	/**
	 * The unnormalized weight of the edge from u to v, weighted by the given parameter vector.
	 * @param g
	 * @param u Start node
	 * @param v End node
	 * @param p Edge parameter vector mapping edge feature names to nonnegative values.
	 * @return
	 */
	public double edgeWeight(LearningGraph<F> g, int u, int v, ParamVector<F,?> p) {
		double wt = this.weightingScheme.edgeWeight(p,g.getFeatures(u, v));

		if (Double.isInfinite(wt)) return Double.MAX_VALUE;
		return wt;
	}

	/**
	 * The sum of the unnormalized weights of all outlinks from u.
	 * @param g
	 * @param u Start node
	 * @param p Edge parameter vector mapping edge feature names to nonnegative values.
	 * @return
	 */
	public double totalEdgeWeight(LearningGraph<F> g, int u,  ParamVector<F,?> p) {
		double sum = 0.0;
		for (TIntIterator it = g.near(u).iterator(); it.hasNext();) {
			int v = it.next();
			double ew = edgeWeight(g,u,v,p); 
			sum+=ew;
		}
		if (Double.isInfinite(sum)) return Double.MAX_VALUE;
		return sum;
	}
	/**
	 * Random walk with restart from start vector using this.maxT iterations.
	 * @param g
	 * @param startVec Query vector mapping node names to values.
	 * @param paramVec Edge parameter vector mapping edge feature names to nonnegative values.
	 * @return RWR result vector mapping nodes to values
	 */
	public TIntDoubleMap rwrUsingFeatures(LearningGraph<F> g, TIntDoubleMap startVec, ParamVector<F,?> paramVec) {
		TIntDoubleMap vec = startVec;
		for(int i=0; i<maxT; i++) {
			vec = walkOnceUsingFeatures(g,vec,paramVec);
		}
		return vec;
	}
	/**
	 * Walk one step away from vec and update vec weights according to paramVec (feature weights).
	 * @param g
	 * @param vec The current query vector (or last iteration's result vector) mapping node names to values.
	 * @param paramVec Edge parameter vector mapping edge feature names to nonnegative values.
	 * @return Mapping from new set of node names to updated values.
	 */
	public TIntDoubleMap walkOnceUsingFeatures(final LearningGraph<F> g, TIntDoubleMap vec, final ParamVector<F,?> paramVec) {
		final TIntDoubleMap nextVec = new TIntDoubleHashMap();
		vec.forEachEntry(new TIntDoubleProcedure() {
			int k=-1;
			@Override
			public boolean execute(int u, double uw) {
				k++;
				if (k>0 && k%100 == 0) log.debug("Walked from "+k+" nodes...");
				if (uw == 0) {
					log.info("0 node weight at u="+u+"; skipping");
					return true;
				}
				double z = totalEdgeWeight(g,u,paramVec);
				if (z==0) {
					log.info("0 total edge weight at u="+u+"; skipping");
					return true;
				}
				for (TIntIterator it = g.near(u).iterator(); it.hasNext();) {
					int v = it.next();
					double ew = edgeWeight(g,u,v,paramVec);
					double inc = uw * ew / z;
					Dictionary.increment(nextVec,v,inc);
				}
				return true;
			}
		});
		if (nextVec.size() == 0) {
			log.warn("NO entries in nextVec after walkOnceUsingFeatures :(");
		}
		return nextVec;
	}
	/**
	 * Derivative of the function associated with the
	 * rwrUsingFeatures function, with respect to the parameter
	 * vector.  Returns a 2-d dictionary d so that d[node][feature]
	 * is derivative of feature f at node u - algorithm 1 of the
	 * paper.
	 * 
	 * @param graph
	 * @param queryVec Maps node names to values.
	 * @param paramVec Maps edge feature names to nonnegative values.
	 * @return Mapping from each outgoing node from the random walk of the query and each feature relevant to the outgoing edge, to the derivative value. 
	 */
	public TIntObjectMap<TObjectDoubleMap<F>> derivRWRbyParams(final LearningGraph<F> graph, 
			TIntDoubleMap queryVec, final ParamVector<F,?> paramVec) {
		TIntDoubleMap pTrack = queryVec;
		TIntObjectMap<TObjectDoubleMap<F>> dTrack = new TIntObjectHashMap<TObjectDoubleMap<F>>();
		for (int i=0; i<maxT; i++) {
			final TIntDoubleMap p = pTrack;
			final TIntObjectMap<TObjectDoubleMap<F>> d = dTrack;
			TIntDoubleMap pNext = walkOnceUsingFeatures(graph, p, paramVec);
			// dNext[u] is the vector deriv of the weight vector at u
			final TIntObjectMap<TObjectDoubleMap<F>> dNext = new TIntObjectHashMap<TObjectDoubleMap<F>>();
			pNext.forEachKey(new TIntProcedure() {
				@Override
				public boolean execute(int j) {
					double z = totalEdgeWeight(graph,j,paramVec);
					if (z == 0) return true;
					double pj = Dictionary.safeGet(p, j);
					for (TIntIterator it = graph.near(j).iterator(); it.hasNext();) {
						int u = it.next();
						TObjectDoubleMap<F> dWP_ju = derivWalkProbByParams(graph,j,u,paramVec);
						Set<F> features = new TreeSet<F>();
						if(d.containsKey(j)) features.addAll(d.get(j).keySet());
						features.addAll(dWP_ju.keySet());
						for (F f : trainableFeatures(features)) {
							Dictionary.increment(dNext, u, f, 
									edgeWeight(graph,j,u,paramVec)/z * Dictionary.safeGetGet(d, j, f) + pj * Dictionary.safeGet(dWP_ju, f));
						}
					}
					return true;
				}
			});
			pTrack = pNext;
			dTrack = dNext;
		}
		return dTrack;
	}
	/**
	 * Subroutine of derivRWRbyParams, corresponding, in the 
	 * paper, to the equation for partial Q_ju / partial w, just
	 * below Alg 1.
	 * 
	 * @param graph
	 * @param j start node
	 * @param u end node
	 * @param paramVec Maps edge feature names to nonnegative values.
	 * @return Mapping from feature names to derivative values.
	 */
	protected TObjectDoubleMap<F> derivWalkProbByParams(LearningGraph<F> graph,
			int u, int v, ParamVector<F,?> paramVec) {
		
		double totEdgeWeightU = totalEdgeWeight(graph,u,paramVec);
        TObjectDoubleMap<F> derWalk = new TObjectDoubleHashMap<F>();
        if (totEdgeWeightU == 0) return derWalk;

        final TObjectDoubleMap<F> totDerFeature = new TObjectDoubleHashMap<F>();
		for (TIntIterator it = graph.near(u).iterator(); it.hasNext();) {
			int k = it.next();
            TObjectDoubleMap<F> derEdgeUK = this.derivEdgeWeightByParams(graph,u,k,paramVec);
            derEdgeUK.forEachEntry(new TObjectDoubleProcedure<F>() {
				@Override
				public boolean execute(F key, double value) {
	            	Dictionary.increment(totDerFeature, key, value);
					return true;
				}
			});
        }

        double edgeUV = this.edgeWeight(graph, u, v, paramVec);
        TObjectDoubleMap<F> derEdgeUV = this.derivEdgeWeightByParams(graph,u,v,paramVec);
        for (F f : trainableFeatures(totDerFeature.keySet())) {
            // above revised to avoid overflow with very large edge weights, 15 jan 2014 by kmm:
            double term2 = (edgeUV / totEdgeWeightU) * Dictionary.safeGet(totDerFeature, f);
            double val = Dictionary.safeGet(derEdgeUV, f) - term2;
            Dictionary.increment(derWalk, f, val / totEdgeWeightU);
        }
        return derWalk;
	}
	/**
	 * A dictionary d so that d[f] is the derivative of the
	 *  unnormalized edge weight between u and v wrt feature f.  This
	 *  assumes edge weights are linear in their feature sums.
	 * @param graph
	 * @param u Start node
	 * @param v End node 
	 * @param paramVec Maps edge feature names to nonnegative values.
	 * @return Mapping from features names to the derivative value.
	 */
	protected TObjectDoubleMap<F> derivEdgeWeightByParams(LearningGraph<F> graph, 
			int u, int v, final ParamVector<F,?> paramVec) {
		TObjectDoubleMap<F> phi = graph.getFeatures(u, v);
		final TObjectDoubleMap<F> result = new TObjectDoubleHashMap<F>(phi.size());
		final WeightingScheme<F> w = this.weightingScheme;
		phi.forEachKey(new TObjectProcedure<F>() {
			@Override
			public boolean execute(F featureName) {
				result.put(featureName, w.derivEdgeWeight(
						Dictionary.safeGet(paramVec, featureName, w.defaultWeight())));
				return true;
			}
		});
		return result;
	}

	public boolean trainable(F feature) {
		return !untrainedFeatures.contains(feature);
	}
	
	/**
	 * Builds a set of features in the specified set that are not on the untrainedFeatures list.
	 * @param candidates feature names
	 * @return
	 */
	public Set<F> trainableFeatures(Set<F> candidates) {
		TreeSet<F> result = new TreeSet<F>();
		for (F f : candidates) {
			if (trainable(f)) result.add(f);
		}
		return result;
	}
	/**
	 * Builds a set of features from the keys of the specified map that are not on the untrainedFeatures list.
	 * @param paramVec Maps from features names to nonnegative values.
	 * @return
	 */
	public Set<F> trainableFeatures(Map<F,?> paramVec) {
		return trainableFeatures(paramVec.keySet());
	}
	
//	/**
//	 * Builds a set of features in the names of the specified Feature set that are not on the untrainedFeatures list.
//	 * @param candidates Feature objects
//	 * @return
//	 */
//	public Set<String> trainableFeatures(List<Feature> candidates) {
//
//		TreeSet<String> result = new TreeSet<String>();
//		for (Feature f : candidates) {
//			if (trainable(f.featureName)) result.add(f.featureName);
//		}
//		return result;
//	}

	/** Allow subclasses to filter feature list **/
	public Set<F> localFeatures(ParamVector<F,?> paramVec, E example) {
		return paramVec.keySet();
	}

	public Set<F> untrainedFeatures() { return this.untrainedFeatures; }

	/** Add the gradient vector to a second accumulator vector
	 */
	public void accumulateGradient(Map<String,Double> grad, double exampleLength, Map<String,Double> sumGradient) {
		for (Map.Entry<String, Double> f : grad.entrySet()) {
			if (!sumGradient.containsKey(f.getKey())) {
				sumGradient.put(f.getKey(), new Double(0.0));
			}
			sumGradient.put(f.getKey(), new Double(sumGradient.get(f.getKey()).doubleValue() + f.getValue()/exampleLength));
		}
	}

	/**
	 * Modify the parameter vector paramVec by taking a gradient step along the dir suggested by this example.
	 * @param weightVec
	 * @param pairwiseRWExample
	 */
	public void trainOnExample(final ParamVector<F,?> paramVec, E example) {
		addDefaultWeights(example.getGraph(),paramVec);
		prepareGradient(paramVec,example);
		TObjectDoubleMap<F> grad = gradient(paramVec,example);
		if (log.isDebugEnabled()) {
			log.debug("Gradient: "+Dictionary.buildString(grad, new StringBuilder(), "\n\t").toString());
			checkGradient(grad, paramVec, example);
		}
		final double rate = learningRate();
		if (log.isDebugEnabled()) log.debug("rate "+rate);
		grad.forEachEntry(new TObjectDoubleProcedure<F>() {
			@Override
			public boolean execute(F f, double value) {
				Dictionary.increment(paramVec, f, - rate * value);
				log.debug(f+"->"+paramVec.get(f));
				return true;
			}
		});
	}
	
	/**
	 * Check if first-order approximation is close
	 */
	protected void checkGradient(TObjectDoubleMap<F> grad, final ParamVector<F,?> paramVec, final E example) {
		final ParamVector<F,?> perturbedParamVec = paramVec.copy();
        final double epsilon = 1.0e-10;
        grad.forEachEntry(new TObjectDoubleProcedure<F>() {
        	double perturbedLoss;
        	double loss = empiricalLoss(paramVec, example);
			@Override
			public boolean execute(F f, double value) {
				if (untrainedFeatures.contains(f)) return true;
	            Dictionary.increment(perturbedParamVec, f, epsilon);
	            perturbedLoss = empiricalLoss(perturbedParamVec, example);
	            log.debug(f + "\ttrue: " + (perturbedLoss-loss) + "\tapproximation: " + (epsilon*value));
	            loss = perturbedLoss;
				return true;
			}
		});
	}	

	protected double learningRate() {
		return Math.pow(this.epoch,-2) * this.eta;
	}

	/**
	 * Increase the epoch count
	 */
	public void setEpoch(int e) {
		this.epoch = e;
	}


	/**
	 * Perform any pre-gradient parameter vector updates that may be necessary.
	 * @param paramVec
	 * @param example
	 */
	public void prepareGradient(ParamVector<F,?> paramVec, E example) {}
	
	/**
	 * Compute the local gradient of the parameters, associated
	 *  with a particular start vector and a particular desired
	 *  ranking as encoded in the example.
	 * @param paramVec
	 * @param example
	 * @return
	 */
	public TObjectDoubleMap<F> gradient(ParamVector<F,?> paramVec, E example) {
		throw new UnsupportedOperationException("Bad programmer! Must override in subclass.");
	}

	/** Give the learner the opportunity to swap in an alternate parameter implementation **/
	public ParamVector<F,?> setupParams(ParamVector<F,?> paramVec) { return paramVec; }
	
	/** Give the learner the opportunity to do additional parameter processing **/
	public void cleanupParams(ParamVector<F,?> paramVec) {}
	
	/**
	 * Reset the loss-tracking state of this walker.
	 */
	public void clearLoss() {
		throw new UnsupportedOperationException("Bad programmer! Must override in subclass.");}
	/**
	 * Retrieve the current loss accumulated across all calls to gradient()
	 * @return
	 */
	public LossData cumulativeLoss() { 
		throw new UnsupportedOperationException("Bad programmer! Must override in subclass."); }
	
	/**
	 * Determine the loss over the specified example using the specified parameters. Clears loss tracking before and after, so this is really not threadsafe at all...
	 * @param paramVec
	 * @param example
	 * @return
	 */
	public double empiricalLoss(ParamVector<F,?> paramVec, E example) {
		log.warn("Discarding accumulated loss information for empirical loss calculation!");
		this.clearLoss();
		this.gradient(paramVec,example);
		double loss = cumulativeLoss().total();
		this.clearLoss();
		return loss;
	}
	/**
	 * Really super not threadsafe at all!!!
	 * @param paramVec
	 * @param exampleIt
	 */
	public double averageLoss(ParamVector<F,?> paramVec, Iterable<E> exampleIt) {
		double totLoss = 0;
		double numTest = 0;
		for (E example : exampleIt) { 
			addDefaultWeights(example.getGraph(),paramVec);
			double el = empiricalLoss(paramVec,example);
			double del = el / example.length();
			totLoss += del;
			numTest += 1;
		}
		return totLoss / numTest;
	}
	public double getMu() {
		return mu;
	}
	public void setMu(double mu) {
		this.mu = mu;
	}
	public int getMaxT() {
		return maxT;
	}
	public void setMaxT(int maxT) {
		this.maxT = maxT;
	}
	public double getEta() {
		return eta;
	}
	public void setEta(double eta) {
		this.eta = eta;
	}
	public double getDelta() {
		return delta;
	}
	public void setDelta(double delta) {
		this.delta = delta;
	}
	public WeightingScheme<F> getWeightingScheme() {
		return weightingScheme;
	}
	public void setWeightingScheme(WeightingScheme<F> weightingScheme) {
		this.weightingScheme = weightingScheme;
	}
}
