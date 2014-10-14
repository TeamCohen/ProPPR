package edu.cmu.ml.praprolog.trove.learn;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import edu.cmu.ml.praprolog.learn.tools.LossData;
import edu.cmu.ml.praprolog.learn.tools.SigmoidWeightingScheme;
import edu.cmu.ml.praprolog.learn.tools.TanhWeightingScheme;
import edu.cmu.ml.praprolog.learn.tools.WeightingScheme;
import edu.cmu.ml.praprolog.trove.graph.AnnotatedTroveGraph;
import edu.cmu.ml.praprolog.trove.learn.tools.RWExample;
import edu.cmu.ml.praprolog.graph.AnnotatedGraph;
import edu.cmu.ml.praprolog.graph.Feature;
import edu.cmu.ml.praprolog.util.Dictionary;
import edu.cmu.ml.praprolog.util.ParamVector;
import gnu.trove.iterator.TIntDoubleIterator;
import gnu.trove.iterator.TIntIterator;
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
	protected double mu;
	protected int maxT;
	protected double eta;
	protected double delta;
	protected double zeta;
	protected File affgraph;
	protected Map<String,List<String>> affinity;
	protected Map<String,Integer> diagonalDegree;
	protected int epoch;
	protected Set<String> untrainedFeatures;
	protected WeightingScheme weightingScheme;
	public SRW() { this(edu.cmu.ml.praprolog.learn.SRW.DEFAULT_MAX_T); }
	public SRW(int maxT) { 
		this(maxT, 
				edu.cmu.ml.praprolog.learn.SRW.DEFAULT_MU, 
				edu.cmu.ml.praprolog.learn.SRW.DEFAULT_ETA, 
				edu.cmu.ml.praprolog.learn.SRW.DEFAULT_WEIGHTING_SCHEME(),
				edu.cmu.ml.praprolog.learn.SRW.DEFAULT_DELTA,
				edu.cmu.ml.praprolog.learn.SRW.DEFAULT_AFFGRAPH,
				edu.cmu.ml.praprolog.learn.SRW.DEFAULT_ZETA); }
	public SRW(int maxT, double mu, double eta, WeightingScheme wScheme, double delta, File affgraph, double zeta) {
		this.maxT = maxT;
		this.mu = mu;
		this.eta = eta;
		this.epoch = 1;
		this.zeta = zeta;
		this.delta = delta;
		this.untrainedFeatures = new TreeSet<String>();
		this.weightingScheme = wScheme;
		this.affgraph = affgraph;
		if(zeta>0){
			affinity = edu.cmu.ml.praprolog.learn.SRW.constructAffinity(affgraph);
			diagonalDegree = edu.cmu.ml.praprolog.learn.SRW.constructDegree(affinity);
		}
	}

	/**
	 * For each feature in the graph which is not already in the parameter vector,
	 * initialize the parameter value to a weight near 1.0, slightly randomized to avoid symmetry.
	 * @param graph
	 * @param p Edge parameter vector mapping edge feature names to nonnegative values.
	 */
	public void addDefaultWeights(AnnotatedTroveGraph graph, Map<String,Double> p) {
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
	public  double edgeWeight(AnnotatedTroveGraph g, int u, int v,  Map<String,Double> p) {
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
	public  double totalEdgeWeight(AnnotatedTroveGraph g, int u,  Map<String,Double> p) {
		double sum = 0.0;
		for (TIntDoubleIterator v = g.near(u).iterator(); v.hasNext();) {
			v.advance();
			double ew = edgeWeight(g,u,v.key(),p); 
			sum+=ew;
		}
		if (Double.isInfinite(sum)) return Double.MAX_VALUE;
		return sum;
	}


	/**
	 * Random walk with restart from start vector using this.maxint iterations.
	 * @param g
	 * @param startVec Query vector mapping node names to values.
	 * @param paramVec Edge parameter vector mapping edge feature names to nonnegative values.
	 * @return RWR result vector mapping nodes to values
	 */
	public  TIntDoubleMap rwrUsingFeatures(AnnotatedTroveGraph g, TIntDoubleMap startVec, ParamVector paramVec) {
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
	public  TIntDoubleMap walkOnceUsingFeatures(AnnotatedTroveGraph g, TIntDoubleMap vec, ParamVector paramVec) {
		TIntDoubleMap nextVec = new TIntDoubleHashMap();
		int k=-1;
		for (TIntDoubleIterator u = vec.iterator(); u.hasNext(); ) { 
			u.advance();
			k++;
			if (k>0 && k%100 == 0) log.debug("Walked from "+k+" nodes...");
			if (u.value() == 0) {
				log.info("0 node weight at u="+u+"; skipping");
				continue;
			}
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
	public  TIntObjectMap<TObjectDoubleHashMap<String>> derivRWRbyParams(AnnotatedTroveGraph graph, TIntDoubleMap queryVec, ParamVector paramVec) {
		TIntDoubleMap p = queryVec;
		TIntObjectMap<TObjectDoubleHashMap<String>> d = new TIntObjectHashMap<TObjectDoubleHashMap<String>>();
		for (int i=0; i<maxT; i++) {
			TIntDoubleMap pNext = walkOnceUsingFeatures(graph, p, paramVec);
			// dNext[u] is the vector deriv of the weight vector at u
			TIntObjectMap<TObjectDoubleHashMap<String>> dNext = new TIntObjectHashMap<TObjectDoubleHashMap<String>>();
			for (TIntDoubleIterator j = pNext.iterator(); j.hasNext(); ) {
				j.advance();
				double z = totalEdgeWeight(graph,j.key(),paramVec);
				if (z == 0) continue;
				double pj = Dictionary.safeGet(p, j.key());
				for (TIntDoubleIterator u = graph.near(j.key()).iterator(); u.hasNext(); ) {
					u.advance();
					TObjectDoubleHashMap<String> dWP_ju = derivWalkProbByParams(graph,j.key(),u.key(),paramVec);
					Set<String> features = new TreeSet<String>();
					if(d.containsKey(j.key())) features.addAll(d.get(j.key()).keySet());
					features.addAll(dWP_ju.keySet());
					for (String f : trainableFeatures(features)) {
						Dictionary.increment(dNext, u.key(), f, 
								edgeWeight(graph,j.key(),u.key(),paramVec) 
								/ z
								* Dictionary.safeGet(d, j.key(), f) 
								+ pj 
								* Dictionary.safeGet(dWP_ju, f));
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
			int u, int v, ParamVector paramVec) {

		double totEdgeWeightU = totalEdgeWeight(graph,u,paramVec);
		TObjectDoubleHashMap<String> derWalk = new TObjectDoubleHashMap<String>();
        if (totEdgeWeightU == 0) return derWalk;
		
        TObjectDoubleHashMap<String> totDerFeature = new TObjectDoubleHashMap<String>();
        for (int k : graph.near(u).keys()) {
        	for (TObjectDoubleIterator<String> e = this.derivEdgeWeightByParams(graph, u, k, paramVec).iterator(); e.hasNext(); ) {
        		e.advance();
        		Dictionary.increment(totDerFeature, e.key(), e.value());
        	}
        }
        
		double edgeUV = this.edgeWeight(graph, u, v, paramVec);
		// vector of edge weights - one for each active feature
		TObjectDoubleHashMap<String> derEdgeUV = this.derivEdgeWeightByParams(graph,u,v,paramVec);
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
	protected  TObjectDoubleHashMap<String> derivEdgeWeightByParams(AnnotatedTroveGraph graph, int u,
			int v, ParamVector paramVec) {
		TObjectDoubleHashMap<String> result = new TObjectDoubleHashMap<String>();
		for (Feature f : graph.phi(u, v)) {
			result.put(f.featureName, 
					this.weightingScheme.derivEdgeWeight(
							Dictionary.safeGet(paramVec, f.featureName,
									this.weightingScheme.defaultWeight())));
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
	public <X> Set<String> trainableFeatures(ParamVector paramVec) {
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
	public void accumulateGradient(TObjectDoubleHashMap<String> grad, double exampleLength,Map<String,Double> sumGradient) {
		for (TObjectDoubleIterator<String>f = grad.iterator(); f.hasNext(); ) {
			f.advance();
			if (!sumGradient.containsKey(f.key())) {
				sumGradient.put(f.key(), new Double(0.0));
			}
			sumGradient.put(f.key(), new Double(sumGradient.get(f.key()).doubleValue() + f.value()/exampleLength));
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
		TObjectDoubleHashMap<String> grad = gradient(paramVec,example);
		if (log.isDebugEnabled()) {
			log.debug("Gradient: "+Dictionary.buildString(grad, new StringBuilder(), "\n\t").toString());
			checkGradient(grad, paramVec, example);
		}
		double rate = learningRate();
		if (log.isDebugEnabled()) log.debug("rate "+rate);
		for (TObjectDoubleIterator<String>f = grad.iterator(); f.hasNext(); ) {
			f.advance();
			Dictionary.increment(paramVec, f.key(), - rate * f.value());
			log.debug(f.key()+"->"+paramVec.get(f.key()));
		}
		project2feasible(example.getGraph(), paramVec, example.getQueryVec());
	}
	
	/**
	 * Check if first-order approximation is close
	 */
	protected void checkGradient(TObjectDoubleHashMap<String> grad, ParamVector paramVec, E example) {
		ParamVector perturbedParamVec = paramVec.copy();
        double loss = empiricalLoss(paramVec, example);
        double perturbedLoss;
        for (TObjectDoubleIterator f = grad.iterator(); f.hasNext(); ) {
        	f.advance();
			if (untrainedFeatures.contains(f.key())) continue;
            Dictionary.increment(perturbedParamVec, f.key(), edu.cmu.ml.praprolog.learn.SRW.PERTURB_EPSILON);
            perturbedLoss = empiricalLoss(perturbedParamVec, example);
            log.debug(f.key() + "\ttrue: " + (perturbedLoss-loss) + "\tapproximation: " + (edu.cmu.ml.praprolog.learn.SRW.PERTURB_EPSILON*f.value()));
            loss = perturbedLoss;
        }
	}	
	
	protected double learningRate() {
		return Math.pow(this.epoch,-2) * this.eta;
	}
	protected <T> void project2feasible (AnnotatedTroveGraph g,
            ParamVector paramVec, TIntDoubleHashMap query) {
		// temporarily hard-code here
        double alpha = 0.1;
        for (TIntIterator ui = g.getNodes().iterator(); ui.hasNext(); ) {
        	int u = ui.next();
        	for (int q : query.keys()) {
	            // if the node can restart
	        	List<Feature> restart = g.phi(u, q);
	        	if (restart.isEmpty()) continue;
	            Feature f = restart.get(0);
	            if(f.featureName.equals("id(defaultRestart)") || f.featureName.equals("id(alphaBooster)")){
	            
					// check & project for each node
	            	double z = totalEdgeWeight(g, u, paramVec);
	            	double rw = edgeWeight(g,u,q,paramVec);
	            	if (rw / z < alpha) {
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

	protected <T> void projectOneNode(AnnotatedTroveGraph g, int u, ParamVector paramVec,
            double z, double rw, int queryNode) {

		// temporarily hard-code here
        double alpha = 0.1;
        Set<String> nonRestartFeatureSet = new TreeSet<String>();
        int nonRestartNodeNum = 0;
        for (TIntDoubleIterator e = g.near(u).iterator(); e.hasNext(); ) {
        	e.advance();
            int v = e.key();
            if (v != queryNode) {
                nonRestartNodeNum ++;
                for (Feature f : g.phi(u, v)) {
                    nonRestartFeatureSet.add(f.featureName);
                }
            }
        }
        double newValue = weightingScheme.projection(rw,alpha,nonRestartNodeNum);
        for (String f : nonRestartFeatureSet) {
            if (!f.startsWith("db(")) {
				throw new UnsupportedOperationException("The assumption that minalpha only happens on fact/db feature is violated. (" + f + ")");
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
	public TObjectDoubleHashMap<String> gradient(ParamVector paramVec, E example) {
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
	 * Retrieve the current loss accumulated across all calls to gradient().
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
