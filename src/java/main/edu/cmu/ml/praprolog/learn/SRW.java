package edu.cmu.ml.praprolog.learn;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import edu.cmu.ml.praprolog.examples.RWExample;
import edu.cmu.ml.praprolog.graph.v1.AnnotatedGraph;
import edu.cmu.ml.praprolog.graph.v1.Feature;
import edu.cmu.ml.praprolog.learn.tools.LossData;
import edu.cmu.ml.praprolog.learn.tools.LossData.LOSS;
import edu.cmu.ml.praprolog.learn.tools.ReLUWeightingScheme;
import edu.cmu.ml.praprolog.learn.tools.TanhWeightingScheme;
import edu.cmu.ml.praprolog.learn.tools.WeightingScheme;
import edu.cmu.ml.praprolog.util.Dictionary;
import edu.cmu.ml.praprolog.util.ParamVector;

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
	public static final int DEFAULT_RATE_LENGTH = 1;
	public static WeightingScheme DEFAULT_WEIGHTING_SCHEME() { return new ReLUWeightingScheme(); }
	protected double mu;
	protected int maxT;
	protected double eta;
	protected double delta;
	protected int epoch;
	protected Set<String> untrainedFeatures;
	protected WeightingScheme weightingScheme;
	public SRW() { this(DEFAULT_MAX_T); }
	public SRW(int maxT) { this(maxT, DEFAULT_MU, DEFAULT_ETA, DEFAULT_WEIGHTING_SCHEME(), DEFAULT_DELTA); }
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
	public <T> void addDefaultWeights(AnnotatedGraph<T> graph,  Map<String,Double> p) {
		for (String f : graph.getFeatureSet()) {
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
	public <T> double edgeWeight(AnnotatedGraph<T> g, T u, T v,  Map<String,Double> p) {
		double wt = this.weightingScheme.edgeWeight(p,g.phi(u, v));

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
	public <T> Map<T,Double> rwrUsingFeatures(AnnotatedGraph<T> g, Map<T,Double> startVec, ParamVector paramVec) {
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
	public <T> Map<T,Double> walkOnceUsingFeatures(AnnotatedGraph<T> g, Map<T,Double> vec, ParamVector paramVec) {
		Map<T,Double> nextVec = new TreeMap<T,Double>();
		int k=-1;
		for (Map.Entry<T, Double> u : vec.entrySet()) { k++;
			if (k>0 && k%100 == 0) log.debug("Walked from "+k+" nodes...");
			if (u.getValue() == 0) {
				log.info("0 node weight at u="+u+"; skipping");
				continue;
			}
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
	public <T> Map<T, Map<String, Double>> derivRWRbyParams(AnnotatedGraph<T> graph, Map<T, Double> queryVec, ParamVector paramVec) {
		Map<T,Double> p = queryVec;
		Map<T,Map<String,Double>> d = new TreeMap<T,Map<String,Double>>();
		for (int i=0; i<maxT; i++) {
			Map<T,Double> pNext = walkOnceUsingFeatures(graph, p, paramVec);
			// dNext[u] is the vector deriv of the weight vector at u
			Map<T,Map<String,Double>> dNext = new TreeMap<T,Map<String,Double>>();
			for (T j : pNext.keySet()) {
				double z = totalEdgeWeight(graph,j,paramVec);
				if (z == 0) continue;
				double pj = Dictionary.safeGet(p, j);
				for (T u : graph.nearNative(j).keySet()) {
					Map<String,Double> dWP_ju = derivWalkProbByParams(graph,j,u,paramVec);
					Set<String> features = new TreeSet<String>();
					if(d.containsKey(j)) features.addAll(d.get(j).keySet());
					features.addAll(dWP_ju.keySet());
					for (String f : trainableFeatures(features)) {
						Dictionary.increment(dNext, u, f, 
								edgeWeight(graph,j,u,paramVec)/z * Dictionary.safeGetGet(d, j, f) + pj * Dictionary.safeGet(dWP_ju, f));
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
			T u, T v, ParamVector paramVec) {
		
		double totEdgeWeightU = totalEdgeWeight(graph,u,paramVec);
        Map<String,Double> derWalk = new TreeMap<String,Double>();
        if (totEdgeWeightU == 0) return derWalk;

        Map<String,Double> totDerFeature = new TreeMap<String,Double>();
        for (T k : graph.nearNative(u).keySet()) {
            Map<String,Double> derEdgeUK = this.derivEdgeWeightByParams(graph,u,k,paramVec);
            for (Map.Entry<String,Double> e : derEdgeUK.entrySet()) 
            	Dictionary.increment(totDerFeature, e.getKey(), e.getValue());
        }

        double edgeUV = this.edgeWeight(graph, u, v, paramVec);
        Map<String,Double> derEdgeUV = this.derivEdgeWeightByParams(graph,u,v,paramVec);
        for (String f : trainableFeatures(totDerFeature.keySet())) {
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
	protected <T> Map<String,Double> derivEdgeWeightByParams(AnnotatedGraph<T> graph, T u,
			T v, ParamVector paramVec) {
		Map<String,Double> result = new TreeMap<String,Double>();
		for (Feature f : graph.phi(u, v)) {
			result.put(f.featureName, this.weightingScheme.derivEdgeWeight(Dictionary.safeGet(paramVec, f.featureName, this.weightingScheme.defaultWeight())));
		}
		return result;
	}

	/**
	 * Builds a set of features in the specified set that are not on the untrainedFeatures list.
	 * @param candidates feature names
	 * @return
	 */
	public Set<String> trainableFeatures(Set<String> candidates) {
		TreeSet<String> result = new TreeSet<String>();
		for (String f : candidates) {
			if (trainable(f)) result.add(f);
		}
		return result;
	}
	/**
	 * Builds a set of features from the keys of the specified map that are not on the untrainedFeatures list.
	 * @param paramVec Maps from features names to nonnegative values.
	 * @return
	 */
	public <X> Set<String> trainableFeatures(Map<String,X> paramVec) {
		return trainableFeatures(paramVec.keySet());
	}
	
	public boolean trainable(String feature) {
		return !untrainedFeatures.contains(feature);
	}
	/**
	 * Builds a set of features in the names of the specified Feature set that are not on the untrainedFeatures list.
	 * @param candidates Feature objects
	 * @return
	 */
	public Set<String> trainableFeatures(List<Feature> candidates) {

		TreeSet<String> result = new TreeSet<String>();
		for (Feature f : candidates) {
			if (trainable(f.featureName)) result.add(f.featureName);
		}
		return result;
	}

	/** Allow subclasses to filter feature list **/
	public Set<String> localFeatures(ParamVector paramVec, E example) {
		return paramVec.keySet();
	}

	public Set<String> untrainedFeatures() { return this.untrainedFeatures; }

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
	public void trainOnExample(ParamVector paramVec, E example) {
		addDefaultWeights(example.getGraph(),paramVec);
		prepareGradient(paramVec,example);
		Map<String,Double> grad = gradient(paramVec,example);
		if (log.isDebugEnabled()) {
			log.debug("Gradient: "+Dictionary.buildString(grad, new StringBuilder(), "\n\t").toString());
			checkGradient(grad, paramVec, example);
		}
		double rate = learningRate();
		if (log.isDebugEnabled()) log.debug("rate "+rate);
		for (Map.Entry<String, Double> f : grad.entrySet()) {
			Dictionary.increment(paramVec, f.getKey(), - rate * f.getValue());
			log.debug(f.getKey()+"->"+paramVec.get(f.getKey()));
		}
	}
	
	/**
	 * Check if first-order approximation is close
	 */
	protected void checkGradient(Map<String,Double> grad, ParamVector paramVec, E example) {
		ParamVector perturbedParamVec = paramVec.copy();
        double epsilon = 1.0e-10;
        double loss = empiricalLoss(paramVec, example);
        double perturbedLoss;
        for (Map.Entry<String, Double> f : grad.entrySet()) {
			if (untrainedFeatures.contains(f.getKey())) continue;
            Dictionary.increment(perturbedParamVec, f.getKey(), epsilon);
            perturbedLoss = empiricalLoss(perturbedParamVec, example);
            log.debug(f.getKey() + "\ttrue: " + (perturbedLoss-loss) + "\tapproximation: " + (epsilon*f.getValue()));
            loss = perturbedLoss;
        }
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
	public void prepareGradient(ParamVector paramVec, E example) {}
	
	/**
	 * Compute the local gradient of the parameters, associated
	 *  with a particular start vector and a particular desired
	 *  ranking as encoded in the example.
	 * @param paramVec
	 * @param example
	 * @return
	 */
	public Map<String,Double> gradient(ParamVector paramVec, E example) {
		throw new UnsupportedOperationException("Bad programmer! Must override in subclass.");
	}

	/** Give the learner the opportunity to swap in an alternate parameter implementation **/
	public ParamVector setupParams(ParamVector paramVec) { return paramVec; }
	
	/** Give the learner the opportunity to do additional parameter processing **/
	public void cleanupParams(ParamVector paramVec) {}
	
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
	public double empiricalLoss(ParamVector paramVec, E example) {
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
	public double averageLoss(ParamVector paramVec, Iterable<E> exampleIt) {
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
	public WeightingScheme getWeightingScheme() {
		return weightingScheme;
	}
	public void setWeightingScheme(WeightingScheme weightingScheme) {
		this.weightingScheme = weightingScheme;
	}
}
