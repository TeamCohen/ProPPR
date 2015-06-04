package edu.cmu.ml.praprolog.learn;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import edu.cmu.ml.praprolog.graph.AnnotatedGraph;
import edu.cmu.ml.praprolog.graph.Feature;
import edu.cmu.ml.praprolog.learn.tools.LossData;
import edu.cmu.ml.praprolog.learn.tools.LossData.LOSS;
import edu.cmu.ml.praprolog.learn.tools.RWExample;
import edu.cmu.ml.praprolog.learn.tools.ReLUWeightingScheme;
import edu.cmu.ml.praprolog.learn.tools.SRWParameters;
import edu.cmu.ml.praprolog.learn.tools.TanhWeightingScheme;
import edu.cmu.ml.praprolog.learn.tools.WeightingScheme;
import edu.cmu.ml.praprolog.prove.DprProver;
import edu.cmu.ml.praprolog.prove.MinAlphaException;
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
	public static final int NUM_EPOCHS = 5;
	public static final int DEFAULT_RATE_LENGTH = 1;
	public static final double PERTURB_EPSILON=1e-10;
	protected SRWParameters c;
	protected Set<String> untrainedFeatures;
	protected int epoch;
	public SRW() { this(new SRWParameters()); }
	public SRW(int maxT) { this(new SRWParameters(maxT)); }
	public SRW(SRWParameters params) {
		this.c = params;
		this.epoch = 1;
		this.untrainedFeatures = new TreeSet<String>();
	}

	public static HashMap<String,List<String>> constructAffinity(File affgraph){	
		if (affgraph == null) throw new IllegalArgumentException("Missing affgraph file!");
		//Construct the affinity matrix from the input
		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(affgraph));
			HashMap<String,List<String>> affinity = new HashMap<String,List<String>>();
		    String line = null;
			while ((line = reader.readLine()) != null) {
			    String[] items = line.split("\\t");
			    if(!affinity.containsKey(items[0])){
			    	List<String> pairs = new ArrayList<String>();
			    	pairs.add(items[1]);
			    	affinity.put(items[0], pairs);
			    }
			    else{
			    	List<String> pairs = affinity.get(items[0]);
			    	pairs.add(items[1]);
			    	affinity.put(items[0], pairs);
			    }
			}
			return affinity;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public static HashMap<String,Integer> constructDegree(Map<String,List<String>> affinity){
		HashMap<String,Integer> diagonalDegree = new HashMap<String,Integer>();
		for (String key : affinity.keySet()) {
			diagonalDegree.put(key, affinity.get(key).size());
		}
		log.debug("d size:" + diagonalDegree.size());
		return diagonalDegree;
	}


	/**
	 * For each feature in the graph which is not already in the parameter vector,
	 * initialize the parameter value to a weight near 1.0, slightly randomized to avoid symmetry.
	 * @param graph
	 * @param c Edge parameter vector mapping edge feature names to nonnegative values.
	 */
	public <T> void addDefaultWeights(AnnotatedGraph<T> graph,  Map<String,Double> params) {
		for (String f : graph.getFeatureSet()) {
			if (!params.containsKey(f)) {
				params.put(f,c.weightingScheme.defaultWeight()+0.01*random.nextDouble());
			}
		}
	}
	/**
	 * The unnormalized weight of the edge from u to v, weighted by the given parameter vector.
	 * @param g
	 * @param u Start node
	 * @param v End node
	 * @param params Edge parameter vector mapping edge feature names to nonnegative values.
	 * @return
	 */
	public <T> double edgeWeight(AnnotatedGraph<T> g, T u, T v,  Map<String,Double> params) {
		double wt = c.weightingScheme.edgeWeight(params,g.phi(u, v));

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
		for(int i=0; i<c.maxT; i++) {
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
		Map<T,Double> q = queryVec;
		Map<T,Map<String,Double>> d = new TreeMap<T,Map<String,Double>>();
		for (int i=0; i<c.maxT; i++) {
			Map<T,Double> qNext = walkOnceUsingFeatures(graph, q, paramVec);
			// dNext[u] is the vector deriv of the weight vector at u
			Map<T,Map<String,Double>> dNext = new TreeMap<T,Map<String,Double>>();
			for (T j : qNext.keySet()) {
				double z = totalEdgeWeight(graph,j,paramVec);
				if (z == 0) continue;
				double qj = Dictionary.safeGet(q, j);
				for (T u : graph.nearNative(j).keySet()) {
					Map<String,Double> dWP_ju = derivWalkProbByParams(graph,j,u,paramVec);
					Set<String> features = new TreeSet<String>();
					if(d.containsKey(j)) features.addAll(d.get(j).keySet());
					features.addAll(dWP_ju.keySet());
					for (String f : trainableFeatures(features)) {
						Dictionary.increment(dNext, u, f, 
								edgeWeight(graph,j,u,paramVec)
								/ z 
								* Dictionary.safeGetGet(d, j, f) 
								+ qj 
								* Dictionary.safeGet(dWP_ju, f));
					}
				}
			}
			q = qNext;
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
            double term2 = (edgeUV / totEdgeWeightU) 
            		* Dictionary.safeGet(totDerFeature, f);
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
			result.put(f.featureName, 
					c.weightingScheme.derivEdgeWeight(
							Dictionary.safeGet(paramVec, f.featureName, 
									c.weightingScheme.defaultWeight())));
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
			if (!trainable(f.getKey())) {
				if (paramVec.get(f.getKey()) != 1.0) throw new IllegalStateException("Non-unit value @"+f.getKey());
				continue;
			}
			Dictionary.increment(paramVec, f.getKey(), - rate * f.getValue());
			if(log.isDebugEnabled()) log.debug(f.getKey()+"->"+paramVec.get(f.getKey()));
		}
		try {
			project2feasible(example.getGraph(), paramVec, example.getQueryVec());
		} catch (MinAlphaException e) {
			log.warn(e);
		}
	}
	
	/**
	 * Check if first-order approximation is close
	 */
	protected void checkGradient(Map<String,Double> grad, ParamVector paramVec, E example) {
		ParamVector perturbedParamVec = paramVec.copy();
        double loss = empiricalLoss(paramVec, example);
        double perturbedLoss;
        for (Map.Entry<String, Double> f : grad.entrySet()) {
			if (untrainedFeatures.contains(f.getKey())) continue;
            Dictionary.increment(perturbedParamVec, f.getKey(), PERTURB_EPSILON);
            perturbedLoss = empiricalLoss(perturbedParamVec, example);
            log.debug(f.getKey() + "\ttrue: " + (perturbedLoss-loss) + "\tapproximation: " + (PERTURB_EPSILON*f.getValue()));
            loss = perturbedLoss;
        }
	}	

	protected double learningRate() {
		return Math.pow(this.epoch,-2) * c.eta;
	}

	protected <T> void project2feasible (AnnotatedGraph<T> g,
            ParamVector paramVec, Map<T,Double> query) {
        for (T u : g.getNodes()) {
        	for (T q : query.keySet()) {
	            // if the node can restart
	        	List<Feature> restart = g.phi(u, q);
	        	if (restart.isEmpty()) continue;
	            Feature f = restart.get(0);
	            if(f.featureName.equals("id(defaultRestart)") || f.featureName.equals("id(alphaBooster)")){
	            
					// check & project for each node
	            	double z = totalEdgeWeight(g, u, paramVec);
	            	double rw = edgeWeight(g,u,q,paramVec);
	            	if (rw / z < c.alpha) {
	                	projectOneNode(g, u, paramVec, z, rw, q);
						if (log.isDebugEnabled()) {
	                		z = totalEdgeWeight(g, u, paramVec);
	                		rw = edgeWeight(g,u,q,paramVec);
		            		log.debug("Local alpha = " + rw / z);
						}
					}
	            }
        	}
        }
    }

	protected <T> void projectOneNode(AnnotatedGraph<T> g, T u, ParamVector paramVec,
            double z, double rw, T queryNode) {
        Set<String> nonRestartFeatureSet = new TreeSet<String>();
        int nonRestartNodeNum = 0;
        for (Map.Entry<T, Double> e : g.nearNative(u).entrySet()) {
            T v = e.getKey();
            if (!v.equals(queryNode)) {
                nonRestartNodeNum ++;
                for (Feature f : g.phi(u, v)) {
                    if (trainable(f.featureName)) nonRestartFeatureSet.add(f.featureName);
                }
            }
        }
        double newValue = c.weightingScheme.projection(rw,c.alpha,nonRestartNodeNum);
        for (String f : nonRestartFeatureSet) {
            if (!f.startsWith("db(")) {
				if (nonRestartFeatureSet.size()>1) {
					throw new MinAlphaException("Minalpha assumption violated: not a fact/db feature (" + Dictionary.buildString(nonRestartFeatureSet,new StringBuilder(),",").toString() + ")");
					//log.warn("Minalpha assumption violated: not a fact/db feature (" + Dictionary.buildString(nonRestartFeatureSet,new StringBuilder(),",").toString() + ")");
					//break;
				}
            } else {
                paramVec.put(f, newValue);
            }
        }
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
		return c.mu;
	}
	public void setMu(double mu) {
		c.mu = mu;
	}
	public int getMaxT() {
		return c.maxT;
	}
	public void setMaxT(int maxT) {
		c.maxT = maxT;
	}
	public double getEta() {
		return c.eta;
	}
	public void setEta(double eta) {
		c.eta = eta;
	}
	public double getDelta() {
		return c.delta;
	}
	public void setDelta(double delta) {
		c.delta = delta;
	}
	public WeightingScheme getWeightingScheme() {
		return c.weightingScheme;
	}
	public void setWeightingScheme(WeightingScheme weightingScheme) {
		c.weightingScheme = weightingScheme;
	}
}
