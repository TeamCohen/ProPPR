package edu.cmu.ml.praprolog.learn;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import edu.cmu.ml.praprolog.graph.AnnotatedGraph;
import edu.cmu.ml.praprolog.graph.Feature;
import edu.cmu.ml.praprolog.util.Dictionary;

/**
 *  Supervised random walk with reset on a graph, following
 *  Backstrom and Leskovec's 2011 WSDM paper, but using SGD instead of
 *  their LBG optimization scheme, and assuming that all restart links
 *  are explicitly represented in the graph.
 *  
 * @author wcohen,krivard,yww
 *
 */
public class SRW<E extends RWExample> {
	private static final Logger log = Logger.getLogger(SRW.class);
	private static Random random = new Random();
	public static void seed(long seed) { random.setSeed(seed); }
	protected static final int NUM_EPOCHS = 5;
	public static final int DEFAULT_MAX_T=10;
	public static final double DEFAULT_MU=.001;
	public static final double DEFAULT_ETA=1.0;
	public static final double DEFAULT_DELTA=0.5;
	protected double mu;
	protected int maxT;
	protected double eta;
	protected double delta;
	protected int epoch;
	protected Set<String> untrainedFeatures;
	protected WeightingScheme weightingScheme;
	public SRW() { this(DEFAULT_MAX_T); }
	public SRW(int maxT) { this(maxT, DEFAULT_MU, DEFAULT_ETA, new TanhWeightingScheme(), DEFAULT_DELTA); }
	public SRW(int maxT, double mu, double eta, WeightingScheme wScheme, double delta) {
		this.maxT = maxT;
		this.mu = mu;
		this.eta = eta;
		this.epoch = 1;
		this.delta = delta;
		this.untrainedFeatures = new TreeSet<String>();
		this.weightingScheme = wScheme;
	}


	/**
	 * For each feature in the graph which is not already in the parameter vector,
	 * initialize the parameter value to a weight near 1.0, slightly randomized to avoid symmetry.
	 * @param graph
	 * @param p Edge parameter vector mapping edge feature names to nonnegative values.
	 */
	public static <T> void addDefaultWeights(AnnotatedGraph<T> graph,  Map<String,Double> p) {
		for (String f : graph.getFeatureSet()) {
			if (!p.containsKey(f)) {
				p.put(f,1.0+0.01*random.nextDouble());
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
	public <T> double edgeWeight(AnnotatedGraph<T> g, T u, T v,  Map<String,Double> p) {
		return this.weightingScheme.edgeWeight(p,g.phi(u, v));
	}

//	/**
//	 * The function wraps the product of edge weight and feature.
//	 * @param p product of edge weight and feature.
//	 * @return 
//	 */
//	public double edgeWeightFunction(double product) {
//		//WW: We found exp to have the overflow issue, replace by sigmoid.
//		//return Math.exp(product);
//		//return sigmoid(product);
//		return tanh(product);
//	}
//
//	public double sigmoid(double x){
//		return 1/(1 + Math.exp(-x));
//       }
//
//	public double tanh(double x){
//		return ( Math.exp(x) -  Math.exp(-x))/( Math.exp(x) +  Math.exp(-x));
//	}

	/**
	 * The sum of the unnormalized weights of all outlinks from u.
	 * @param g
	 * @param u Start node
	 * @param p Edge parameter vector mapping edge feature names to nonnegative values.
	 * @return
	 */
	public <T> double totalEdgeWeight(AnnotatedGraph<T> g, T u,  Map<String,Double> p) {
		double sum = 0.0;
		for (T v : g.nearNative(u).keySet()) {
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
	public <T> Map<T,Double> rwrUsingFeatures(AnnotatedGraph<T> g, Map<T,Double> startVec, Map<String,Double> paramVec) {
		Map<T,Double> vec = startVec;
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
	public <T> Map<T,Double> walkOnceUsingFeatures(AnnotatedGraph<T> g, Map<T,Double> vec, Map<String,Double> paramVec) {
		Map<T,Double> nextVec = new TreeMap<T,Double>();
		int k=-1;
		for (Map.Entry<T, Double> u : vec.entrySet()) { k++;
			if (k>0 && k%100 == 0) log.debug("Walked from "+k+" nodes...");
			double z = totalEdgeWeight(g,u.getKey(),paramVec);
			if (z==0) {
				log.info("0 total edge weight at u="+u+"; skipping");
				continue;
			}
			for (Map.Entry<T, Double> e : g.nearNative(u.getKey()).entrySet()) {
				T v = e.getKey();
				double ew = edgeWeight(g,u.getKey(),v,paramVec);
				double inc = u.getValue() * ew / z;
				Dictionary.increment(nextVec,v,inc);
			}
		}
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
	public <T> Map<T, Map<String, Double>> derivRWRbyParams(AnnotatedGraph<T> graph, Map<T, Double> queryVec, Map<String, Double> paramVec) {
		Map<T,Double> p = queryVec;
		Map<T,Map<String,Double>> d = new TreeMap<T,Map<String,Double>>();
		for (int i=0; i<maxT; i++) {
			Map<T,Double> pNext = walkOnceUsingFeatures(graph, p, paramVec);
			// dNext[u] is the vector deriv of the weight vector at u
			Map<T,Map<String,Double>> dNext = new TreeMap<T,Map<String,Double>>();
			for (T j : pNext.keySet()) {
				for (T u : graph.nearNative(j).keySet()) {
					Map<String,Double> dWP_ju = derivWalkProbByParams(graph,j,u,paramVec);
					for (String f : trainableFeatures(graph.phi(j,u))) {
						Dictionary.increment(dNext, u, f, 
								  edgeWeight(graph,j,u,paramVec) * Dictionary.safeGetGet(d, j, f) 
								+ Dictionary.safeGet(p, j) * dWP_ju.get(f));
					}
				}
			}
			p = pNext;
			d = dNext;
		}
		return d;
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
	protected <T> Map<String, Double> derivWalkProbByParams(AnnotatedGraph<T> graph,
			T u, T v, Map<String, Double> paramVec) {
		
		double edgeUV = this.edgeWeight(graph, u, v, paramVec);
		// vector of edge weights - one for each active feature
		Map<String,Double> derEdgeUV = this.derivEdgeWeightByParams(graph,u,v,paramVec);
		Set<String> activeFeatures = derEdgeUV.keySet();
		double totEdgeWeightU = totalEdgeWeight(graph,u,paramVec);
//		double totEdgeWeightV = totalEdgeWeight(graph, v, paramVec);
		double totDerEdgeUV = 0;
		for (double w : derEdgeUV.values()) totDerEdgeUV += w;
		Map<String,Double> derWalk = new TreeMap<String,Double>();
		for (String f : trainableFeatures(activeFeatures)) {
//			double val = derEdgeUV.get(f) * totEdgeWeightU - edgeUV * totDerEdgeUV;
//			derWalk.put(f, val / (totEdgeWeightU * totEdgeWeightU));
			// above revised to avoid overflow with very large edge weights, 15 jan 2014 by kmm:
			double term2 = (edgeUV / totEdgeWeightU) * totDerEdgeUV;
			double val = derEdgeUV.get(f) - term2;
			derWalk.put(f, val / totEdgeWeightU);
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
	protected <T> Map<String,Double> derivEdgeWeightByParams(AnnotatedGraph<T> graph, T u,
			T v, Map<String, Double> paramVec) {
		Map<String,Double> result = new TreeMap<String,Double>();
		for (Feature f : graph.phi(u, v)) {
			result.put(f.featureName, this.weightingScheme.derivEdgeWeight(f.weight));
		}
		return result;
	}

//	/**
//	 * The function wraps the derivative of edge weight.
//	 * @param weight: edge weight.
//	 * @return wrapped derivative of the edge weight.
//	 */
//	public double derivEdgeWeightFunction(double weight) {
//		
//		//WW: replace with sigmoid function's derivative.
//		//return Math.exp(weight);
//		//return derivSigmoid(weight);
//		return derivTanh(weight);
//	}
//
//	public double derivSigmoid(double value) {
//		return sigmoid(value) * (1 - sigmoid(value));
//       }
//
//	public double derivTanh(double value) {
//		return (1- tanh(value)*tanh(value));
//       }

	/**
	 * Builds a set of features in the specified set that are not on the untrainedFeatures list.
	 * @param candidates feature names
	 * @return
	 */
	public Set<String> trainableFeatures(Set<String> candidates) {
		TreeSet<String> result = new TreeSet<String>();
		for (String f : candidates) {
			if (!untrainedFeatures.contains(f)) result.add(f);
		}
		return result;
	}
	/**
	 * Builds a set of features from the keys of the specified map that are not on the untrainedFeatures list.
	 * @param paramVec Maps from features names to nonnegative values.
	 * @return
	 */
	public Set<String> trainableFeatures(Map<String, Double> paramVec) {
		return trainableFeatures(paramVec.keySet());
	}
	/**
	 * Builds a set of features in the names of the specified Feature set that are not on the untrainedFeatures list.
	 * @param candidates Feature objects
	 * @return
	 */
	public Set<String> trainableFeatures(List<Feature> candidates) {

		TreeSet<String> result = new TreeSet<String>();
		for (Feature f : candidates) {
			if (!untrainedFeatures.contains(f.featureName)) result.add(f.featureName);
		}
		return result;
	}
	
	/** Allow subclasses to filter feature list **/
	public Set<String> localFeatures(Map<String,Double> paramVec, E example) {
		return paramVec.keySet();
	}
	
	public Set<String> untrainedFeatures() { return this.untrainedFeatures; }
	
	/**
	 * Modify the parameter vector paramVec by taking a gradient step along the dir suggested by this example.
	 * @param weightVec
	 * @param pairwiseRWExample
	 */
	public void trainOnExample(Map<String, Double> paramVec, E example) {
		addDefaultWeights(example.getGraph(),paramVec);
		Map<String,Double> grad = gradient(paramVec,example);
		if (log.isDebugEnabled()) {
			log.debug("Gradient: "+Dictionary.buildString(grad, new StringBuilder(), "\n\t").toString());
		}
		double rate = Math.pow(this.epoch,-2) * this.eta / example.length();
		if (log.isDebugEnabled()) log.debug("rate "+rate);
		// since paramVec is restricted to nonnegative values, we automatically adjust the rate
		// by looking at the current values and the current gradient, and reduce the rate as necessary.
		// 
		// unfortunately, this means we need locked access to the paramVec, since if someone fusses with it
		// between when we set the rate and when we apply it, we could end up pushing the paramVec too far.
		// :(
//		synchronized(paramVec) { 
//			for (Map.Entry<String,Double>f : grad.entrySet()) { //String f = fEntry.getKey(); 
//				if (Math.abs(f.getValue()) > 0) { 
//					double pf = Dictionary.safeGet(paramVec,f.getKey());
//					double smallEnough = pf / f.getValue();
//					double largeEnough = (pf - MAX_PARAM_VALUE) / f.getValue();
//					if (f.getValue() > 0) {
//						if (largeEnough > smallEnough) 
//							throw new IllegalStateException("Gradient for feature "+f.getKey()+" out of range");
//						rate = Math.min(rate, smallEnough);
//						rate = Math.max(rate, largeEnough);
//					} else {
//						if (largeEnough < smallEnough) 
//							throw new IllegalStateException("Gradient for feature "+f.getKey()+" out of range");
//						rate = Math.max(rate, smallEnough);
//						rate = Math.min(rate, largeEnough);
//					}
//					
//				}
//			}
//			if (log.isDebugEnabled()) log.debug("adjusted rate "+rate);
			for (Map.Entry<String, Double> f : grad.entrySet()) {
//				log.debug(String.format("%s %f %f [%f]", f,Dictionary.safeGet(paramVec,f),grad.get(f),rate*grad.get(f)));
				Dictionary.increment(paramVec, f.getKey(), - rate * f.getValue());
//				if (paramVec.get(f.getKey()) < 0) {
//					throw new IllegalStateException("Parameter weight "+f.getKey()+" can't be negative");
//				} else if (paramVec.get(f.getKey()) > MAX_PARAM_VALUE)
//					throw new IllegalStateException("Parameter weight "+f.getKey()+" can't trigger Infinity");
			}
//		}
	}
	
	/**
	 * [originally from SRW even though SRW lacks empiricalLoss]
	 * @param paramVec
	 * @param exampleIt
	 */
	public double averageLoss(Map<String,Double> paramVec, Iterable<E> exampleIt) {
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
	/**
	 * Compute the local gradient of the parameters, associated
	 *  with a particular start vector and a particular desired
	 *  ranking as encoded in the example.
	 *  
	 * @param paramVec
	 * @param example
	 * @return
	 */
	public Map<String,Double> gradient(Map<String,Double> paramVec, E example) {
	    throw new UnsupportedOperationException("whoops");
	}

	/**
	 * The empirical loss of the current ranking. [This method originally from PairwiseLossTrainedSRW]
	 * @param weightVec
	 * @param pairwiseRWExample
	 */
	public double empiricalLoss(Map<String, Double> paramVec, E example) {
	    throw new UnsupportedOperationException("whoops");
	}
}
