package edu.cmu.ml.proppr.learn;

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

import edu.cmu.ml.proppr.examples.RWExample;
import edu.cmu.ml.proppr.graph.LearningGraph;
import edu.cmu.ml.proppr.learn.tools.LossData;
import edu.cmu.ml.proppr.learn.tools.ReLUWeightingScheme;
import edu.cmu.ml.proppr.learn.tools.SRWParameters;
import edu.cmu.ml.proppr.learn.tools.TanhWeightingScheme;
import edu.cmu.ml.proppr.learn.tools.WeightingScheme;
import edu.cmu.ml.proppr.learn.tools.LossData.LOSS;
import edu.cmu.ml.proppr.prove.MinAlphaException;
import edu.cmu.ml.proppr.prove.wam.plugins.WamPlugin;
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
public class SRW<E extends RWExample> {
	private static final Logger log = Logger.getLogger(SRW.class);
	private static Random random = new Random();
	public static void seed(long seed) { random.setSeed(seed); }
	public static final int NUM_EPOCHS = 5;
	public static final int DEFAULT_RATE_LENGTH = 1;
	public static final double PERTURB_EPSILON=1e-10;
	protected static final TObjectDoubleMap EMPTY = new TObjectDoubleHashMap();
	public static WeightingScheme DEFAULT_WEIGHTING_SCHEME() { return new ReLUWeightingScheme(); }
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
	public void addDefaultWeights(LearningGraph graph, ParamVector<String,?> params) {
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
	public double edgeWeight(LearningGraph g, int u, int v, ParamVector<String,?> p) {
		double wt = c.weightingScheme.edgeWeight(p,g.getFeatures(u, v));

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
	public <D> double totalEdgeWeight(LearningGraph g, int u,  ParamVector<String,D> p) {
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
	public TIntDoubleMap rwrUsingFeatures(LearningGraph g, TIntDoubleMap startVec, ParamVector<String,?> paramVec) {
		TIntDoubleMap vec = startVec;
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
	public <D> TIntDoubleMap walkOnceUsingFeatures(final LearningGraph g, TIntDoubleMap vec, final ParamVector<String,D> paramVec) {
		if (vec.isEmpty()) return vec;
		final TIntDoubleMap nextVec = new TIntDoubleHashMap();
		vec.forEachEntry(new TIntDoubleProcedure() {
			int k=-1;
			LearningGraph graph = g;
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
					nextVec.adjustOrPutValue(v, inc, inc);
//					Dictionary.increment(nextVec,v,inc);
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
	public TIntObjectMap<TObjectDoubleMap<String>> derivRWRbyParams(final LearningGraph graph, 
			TIntDoubleMap queryVec, final ParamVector<String,?> paramVec) {
		TIntDoubleMap pTrack = queryVec;
		TIntObjectMap<TObjectDoubleMap<String>> dTrack = new TIntObjectHashMap<TObjectDoubleMap<String>>();
		for (int i=0; i<c.maxT; i++) {
			final TIntDoubleMap p = pTrack;
			final TIntObjectMap<TObjectDoubleMap<String>> d = dTrack;
			TIntDoubleMap pNext = walkOnceUsingFeatures(graph, p, paramVec);
			// dNext[u] is the vector deriv of the weight vector at u
			final TIntObjectMap<TObjectDoubleMap<String>> dNext = new TIntObjectHashMap<TObjectDoubleMap<String>>();
			pNext.forEachKey(new TIntProcedure() {
				@Override
				public boolean execute(int j) {
					double z = totalEdgeWeight(graph,j,paramVec);
					if (z == 0) return true;
					double pj = Dictionary.safeGet(p, j);
					for (TIntIterator it = graph.near(j).iterator(); it.hasNext();) {
						int u = it.next();
						TObjectDoubleMap<String> dWP_ju = derivWalkProbByParams(graph,j,u,paramVec);
						Set<String> features = new TreeSet<String>();
						if(d.containsKey(j)) features.addAll(d.get(j).keySet());
						features.addAll(dWP_ju.keySet());
						for (String f : trainableFeatures(features)) {
							Dictionary.increment(dNext, u, f, 
									edgeWeight(graph,j,u,paramVec)
									/ z 
									* Dictionary.safeGetGet(d, j, f) 
									+ pj 
									* Dictionary.safeGet(dWP_ju, f));
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
	protected TObjectDoubleMap<String> derivWalkProbByParams(LearningGraph graph,
			int u, int v, ParamVector<String,?> paramVec) {
		
		double totEdgeWeightU = totalEdgeWeight(graph,u,paramVec);
        TObjectDoubleMap<String> derWalk = new TObjectDoubleHashMap<String>();
        if (totEdgeWeightU == 0) return derWalk;

        final TObjectDoubleMap<String> totDerFeature = new TObjectDoubleHashMap<String>();
		for (TIntIterator it = graph.near(u).iterator(); it.hasNext();) {
			int k = it.next();
            TObjectDoubleMap<String> derEdgeUK = this.derivEdgeWeightByParams(graph,u,k,paramVec);
            derEdgeUK.forEachEntry(new TObjectDoubleProcedure<String>() {
				@Override
				public boolean execute(String key, double value) {
	            	Dictionary.increment(totDerFeature, key, value);
					return true;
				}
			});
        }

        double edgeUV = this.edgeWeight(graph, u, v, paramVec);
        TObjectDoubleMap<String> derEdgeUV = this.derivEdgeWeightByParams(graph,u,v,paramVec);
        for (String f : trainableFeatures(totDerFeature.keySet())) {
            // revised to avoid overflow with very large edge weights, 15 jan 2014 by kmm:
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
	protected TObjectDoubleMap<String> derivEdgeWeightByParams(LearningGraph graph, 
			int u, int v, final ParamVector<String,?> paramVec) {
		TObjectDoubleMap<String> phi = graph.getFeatures(u, v);
		final TObjectDoubleMap<String> result = new TObjectDoubleHashMap<String>(phi.size());
		final WeightingScheme<String> w = c.weightingScheme;
		phi.forEachKey(new TObjectProcedure<String>() {
			@Override
			public boolean execute(String featureName) {
				result.put(featureName, w.derivEdgeWeight(
						Dictionary.safeGet(paramVec, featureName, w.defaultWeight())));
				return true;
			}
		});
		return result;
	}

	public boolean trainable(String feature) {
		return !untrainedFeatures.contains(feature);
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
	public Set<String> trainableFeatures(Map<String,?> paramVec) {
		return trainableFeatures(paramVec.keySet());
	}
	
//	/**
//	 * Builds a set of features in the names of the specified Feature set that are not on the untrainedFeatures list.
//	 * @param candidates Feature objects
//	 * @return
//	 */
//	public Set<String> trainableFeatures(List<Stringeature> candidates) {
//
//		TreeSet<String> result = new TreeSet<String>();
//		for (Feature f : candidates) {
//			if (trainable(f.featureName)) result.add(f.featureName);
//		}
//		return result;
//	}

	/** Allow subclasses to filter feature list **/
	public Set<String> localFeatures(ParamVector<String,?> paramVec, E example) {
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
	public void trainOnExample(final ParamVector<String,?> paramVec, E example) {
		addDefaultWeights(example.getGraph(),paramVec);
		prepareGradient(paramVec,example);
		TObjectDoubleMap<String> grad = gradient(paramVec,example);
		if (log.isDebugEnabled()) {
			log.debug("Gradient: "+Dictionary.buildString(grad, new StringBuilder(), "\n\t").toString());
//			checkGradient(grad, paramVec, example);
		}
		final double rate = learningRate();
		if (log.isDebugEnabled()) log.debug("rate "+rate);
		grad.forEachEntry(new TObjectDoubleProcedure<String>() {
			@Override
			public boolean execute(String f, double value) {
				Dictionary.increment(paramVec, f, - rate * value);
				log.debug(f+"->"+paramVec.get(f));
				return true;
			}
		});
		project2feasible(example.getGraph(), paramVec, example.getQueryVec());
	}
	
	/**
	 * Check if first-order approximation is close
	 */
	protected void checkGradient(TObjectDoubleMap<String> grad, final ParamVector<String,?> paramVec, final E example) {
		final ParamVector<String,?> perturbedParamVec = paramVec.copy();
        grad.forEachEntry(new TObjectDoubleProcedure<String>() {
        	double perturbedLoss;
        	double loss = empiricalLoss(paramVec, example);
			@Override
			public boolean execute(String f, double value) {
				if (untrainedFeatures.contains(f)) return true;
	            Dictionary.increment(perturbedParamVec, f, PERTURB_EPSILON);
	            perturbedLoss = empiricalLoss(perturbedParamVec, example);
	            log.debug(f + "\ttrue: " + (perturbedLoss-loss) + "\tapproximation: " + (PERTURB_EPSILON*value));
	            loss = perturbedLoss;
				return true;
			}
		});
	}	

	protected double learningRate() {
		return Math.pow(this.epoch,-2) * c.eta;
	}

	protected void project2feasible (final LearningGraph g,
            ParamVector paramVec, TIntDoubleMap query) {
        for (int u : g.getNodes()) {
        	processNode(u,g,paramVec,query);
        }
    }
	protected void processNode(final int u, final LearningGraph g,
            final ParamVector paramVec, final TIntDoubleMap query) {
		query.forEachKey(new TIntProcedure() {
			@Override
			public boolean execute(int q) {
	            // if the node can restart
	        	TObjectDoubleMap<String> restart = g.getFeatures(u, q);
	        	if (restart.isEmpty()) return true;
	        	if (restart.containsKey("id(restart)") || restart.containsKey("id(alphaBooster)")){
	            
					// check & project for each node
	            	double z = totalEdgeWeight(g, u, paramVec);
	            	double rw = edgeWeight(g,u,q,paramVec);
	            	if (rw / z < c.alpha) {
	                	projectOneNode(u, g, paramVec, z, rw, q);
						if (log.isDebugEnabled()) {
	                		z = totalEdgeWeight(g, u, paramVec);
	                		rw = edgeWeight(g,u,q,paramVec);
		            		log.debug("Local alpha = " + rw / z);
						}
					}
	            }
	            return true;
			}
		});
	}

	protected void projectOneNode(int u, LearningGraph g, ParamVector paramVec,
            double z, double rw, int queryNode) {
        final Set<String> nonRestartFeatureSet = new TreeSet<String>();
        int nonRestartNodeNum = 0;
        for (TIntIterator it = g.near(u).iterator(); it.hasNext();) {
        	int v = it.next();
            if (v == (queryNode)) continue;
            nonRestartNodeNum ++;
            g.getFeatures(u, v).forEachKey(new TObjectProcedure<String>() {

				@Override
				public boolean execute(String feature) {
	                nonRestartFeatureSet.add(feature);
	                return true;
				}
			});
        }
        double newValue = c.weightingScheme.projection(rw,c.alpha,nonRestartNodeNum);
        for (String f : nonRestartFeatureSet) {
            if (!f.startsWith(WamPlugin.FACTS_FUNCTOR)) {
				throw new MinAlphaException("Minalpha assumption violated: not a fact/db feature (" + f + ")");
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
	public void prepareGradient(ParamVector<String,?> paramVec, E example) {}
	
	/**
	 * Compute the local gradient of the parameters, associated
	 *  with a particular start vector and a particular desired
	 *  ranking as encoded in the example.
	 * @param paramVec
	 * @param example
	 * @return
	 */
	public TObjectDoubleMap<String> gradient(ParamVector<String,?> paramVec, E example) {
		throw new UnsupportedOperationException("Bad programmer! Must override in subclass.");
	}

	/** Give the learner the opportunity to swap in an alternate parameter implementation **/
	public ParamVector<String,?> setupParams(ParamVector<String,?> paramVec) { return paramVec; }
	
	/** Give the learner the opportunity to do additional parameter processing **/
	public void cleanupParams(ParamVector<String,?> paramVec) {}
	
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
	public double empiricalLoss(ParamVector<String,?> paramVec, E example) {
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
	public double averageLoss(ParamVector<String,?> paramVec, Iterable<E> exampleIt) {
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
	public WeightingScheme<String> getWeightingScheme() {
		return c.weightingScheme;
	}
	public void setWeightingScheme(WeightingScheme<String> weightingScheme) {
		c.weightingScheme = weightingScheme;
	}
}
