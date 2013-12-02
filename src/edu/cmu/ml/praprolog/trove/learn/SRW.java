package edu.cmu.ml.praprolog.trove.learn;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import edu.cmu.ml.praprolog.trove.graph.AnnotatedTroveGraph;
import edu.cmu.ml.praprolog.trove.graph.Feature;
import edu.cmu.ml.praprolog.util.Dictionary;
import gnu.trove.iterator.TIntDoubleIterator;
import gnu.trove.iterator.TObjectDoubleIterator;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;

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
	protected double mu;
	protected int maxT;
	protected double eta;
	protected int epoch;
	protected Set<String> untrainedFeatures;
	public SRW() { this(10); }
	public SRW(int maxT) { this(maxT, 0.001, 1.0); }
	public SRW(int maxT, double mu, double eta) {
		this.maxT = maxT;
		this.mu = mu;
		this.eta = eta;
		this.epoch = 1;
		this.untrainedFeatures = new TreeSet<String>();
	}
	/**
	 * For each feature in the graph which is not already in the parameter vector,
	 * initialize the parameter value to a weight near 1.0, slightly randomized to avoid symmetry.
	 * @param graph
	 * @param p Edge parameter vector mapping edge feature names to nonnegative values.
	 */
	public static  void addDefaultWeights(AnnotatedTroveGraph graph,  Map<String,Double> p) {
		
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
	public  double edgeWeight(AnnotatedTroveGraph g, int u, int v,  Map<String,Double> p) {
		double sum = 0.0;
		for (Feature f : g.phi(u, v)) {
			sum += Dictionary.safeGet(p, f.featureName) * f.weight;
		}
		return edgeWeightFunction(sum);
	}
	/**
	 * The sum of the unnormalized weights of all outlinks from u.
	 * @param g
	 * @param u Start node
	 * @param p Edge parameter vector mapping edge feature names to nonnegative values.
	 * @return
	 */
	public  double totalEdgeWeight(AnnotatedTroveGraph g, int u,  Map<String,Double> p) {
		double sum = 0.0;
		for (TIntDoubleIterator v = g.near(u).iterator(); v.hasNext();) {
			v.advance();
			double ew = edgeWeight(g,u,v.key(),p); 
			sum+=ew;
		}
		return sum;
	}

	/**
	 * The function wraps the product of edge weight and feature.
	 * @param p product of edge weight and feature.
	 * @return 
	 */
	public double edgeWeightFunction(double product) {
		return Math.exp(product);
	}


	/**
	 * Random walk with restart from start vector using this.maxint iterations.
	 * @param g
	 * @param startVec Query vector mapping node names to values.
	 * @param paramVec Edge parameter vector mapping edge feature names to nonnegative values.
	 * @return RWR result vector mapping nodes to values
	 */
	public  TIntDoubleMap rwrUsingFeatures(AnnotatedTroveGraph g, TIntDoubleMap startVec, Map<String,Double> paramVec) {
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
	public  TIntDoubleMap walkOnceUsingFeatures(AnnotatedTroveGraph g, TIntDoubleMap vec, Map<String,Double> paramVec) {
		TIntDoubleMap nextVec = new TIntDoubleHashMap();
		int k=-1;
		for (TIntDoubleIterator u = vec.iterator(); u.hasNext(); ) { 
			u.advance();
			k++;
			if (k>0 && k%100 == 0) log.debug("Walked from "+k+" nodes...");
			double z = totalEdgeWeight(g,u.key(),paramVec);
			if (z==0) {
				log.info("0 total edge weight at u="+u+"; skipping");
				continue;
			}
			for (TIntDoubleIterator e = g.near(u.key()).iterator(); e.hasNext(); ) {
				e.advance();
				int v = e.key();
				double ew = edgeWeight(g,u.key(),v,paramVec);
				double inc = u.value() * ew / z;
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
	public  TIntObjectMap<TObjectDoubleHashMap<String>> derivRWRbyParams(AnnotatedTroveGraph graph, TIntDoubleMap queryVec, Map<String, Double> paramVec) {
		TIntDoubleMap p = queryVec;
		TIntObjectMap<TObjectDoubleHashMap<String>> d = new TIntObjectHashMap<TObjectDoubleHashMap<String>>();
		for (int i=0; i<maxT; i++) {
			TIntDoubleMap pNext = walkOnceUsingFeatures(graph, p, paramVec);
			// dNext[u] is the vector deriv of the weight vector at u
			TIntObjectMap<TObjectDoubleHashMap<String>> dNext = new TIntObjectHashMap<TObjectDoubleHashMap<String>>();
			for (TIntDoubleIterator j = pNext.iterator(); j.hasNext(); ) {
				j.advance();
				for (TIntDoubleIterator u = graph.near(j.key()).iterator(); u.hasNext(); ) {
					u.advance();
					TObjectDoubleHashMap<String> dWP_ju = derivWalkProbByParams(graph,j.key(),u.key(),paramVec);
					for (String f : trainableFeatures(graph.phi(j.key(),u.key()))) {
						Dictionary.increment(dNext, u.key(), f, 
								  edgeWeight(graph,j.key(),u.key(),paramVec) * Dictionary.safeGet(d, j.key(), f) 
								+ Dictionary.safeGet(p, j.key()) * dWP_ju.get(f));
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
	protected  TObjectDoubleHashMap<String> derivWalkProbByParams(AnnotatedTroveGraph graph,
			int u, int v, Map<String, Double> paramVec) {
		
		double edgeUV = this.edgeWeight(graph, u, v, paramVec);
		// vector of edge weights - one for each active feature
		TObjectDoubleMap<String> derEdgeUV = this.derivEdgeWeightByParams(graph,u,v,paramVec);
		Set<String> activeFeatures = derEdgeUV.keySet();
		double totEdgeWeightU = totalEdgeWeight(graph,u,paramVec);
//		double totEdgeWeightV = totalEdgeWeight(graph, v, paramVec);
		double totDerEdgeUV = 0;
		for (double w : derEdgeUV.values()) totDerEdgeUV += w;
		TObjectDoubleHashMap<String> derWalk = new TObjectDoubleHashMap<String>();
		for (String f : trainableFeatures(activeFeatures)) {
			double val = derEdgeUV.get(f) * totEdgeWeightU - edgeUV * totDerEdgeUV;
			derWalk.put(f, val / (totEdgeWeightU * totEdgeWeightU));
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
	protected  TObjectDoubleMap<String> derivEdgeWeightByParams(AnnotatedTroveGraph graph, int u,
			int v, Map<String, Double> paramVec) {
		TObjectDoubleMap<String> result = new TObjectDoubleHashMap<String>();
		for (Feature f : graph.phi(u, v)) {
			result.put(f.featureName, derivEdgeWeightFunction(f.weight));
		}
		return result;
	}

	/**
	 * The function wraps the derivative of edge weight.
	 * @param weight: edge weight.
	 * @return wrapped derivative of the edge weight.
	 */
	public double derivEdgeWeightFunction(double weight) {
		return Math.exp(weight);
	}


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
	
	public Set<String> untrainedFeatures() { return this.untrainedFeatures; }
	
	/**
	 * Modify the parameter vector paramVec by taking a gradient step along the dir suggested by this example.
	 * @param weightVec
	 * @param pairwiseRWExample
	 */
	public void trainOnExample(Map<String, Double> paramVec, E example) {
		addDefaultWeights(example.getGraph(),paramVec);
		TObjectDoubleHashMap<String> grad = gradient(paramVec,example);
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
			for (TObjectDoubleIterator<String>f = grad.iterator(); f.hasNext(); ) { //String f = fEntry.getKey();
				f.advance();
				if (f.value() > 0) { 
					rate = Math.min(rate, Dictionary.safeGet(paramVec,f.key()) / f.value());
				}
			}
//			if (log.isDebugEnabled()) log.debug("adjusted rate "+rate);
			for (TObjectDoubleIterator<String>f = grad.iterator(); f.hasNext(); ) {
				f.advance();
//				log.debug(String.format("%s %f %f [%f]", f,Dictionary.safeGet(paramVec,f),grad.get(f),rate*grad.get(f)));
				Dictionary.increment(paramVec, f.key(), - rate * f.value());
//				if (paramVec.get(f.key()) < 0) {
//					throw new IllegalStateException("Parameter weight "+f.key()+" can't be negative");
//				}
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
	public TObjectDoubleHashMap<String> gradient(Map<String,Double> paramVec, E example) {
		throw new UnsupportedOperationException("Never call directly on SRW; use a subclass");
	}
	/**
	 * The empirical loss of the current ranking. [This method originally from PairwiseLossTrainedSRW]
	 * @param weightVec
	 * @param pairwiseRWExample
	 */
	public double empiricalLoss(Map<String, Double> paramVec,
			E example) {
		throw new UnsupportedOperationException("Never call directly on SRW; use a subclass");
	}
	
	
}
