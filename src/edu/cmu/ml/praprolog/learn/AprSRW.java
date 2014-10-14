package edu.cmu.ml.praprolog.learn;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.cmu.ml.praprolog.graph.AnnotatedGraph;
import edu.cmu.ml.praprolog.graph.Feature;
import edu.cmu.ml.praprolog.learn.tools.LossData;
import edu.cmu.ml.praprolog.learn.tools.LossData.LOSS;
import edu.cmu.ml.praprolog.learn.tools.PosNegRWExample;
import edu.cmu.ml.praprolog.learn.tools.WeightingScheme;
import edu.cmu.ml.praprolog.prove.DprProver;
import edu.cmu.ml.praprolog.util.Dictionary;
import edu.cmu.ml.praprolog.util.ParamVector;

public class AprSRW<T> extends SRW<PosNegRWExample<T>> {
	private static final Logger log = Logger.getLogger(AprSRW.class);
	private static final double bound = 1.0e-15; //Prevent infinite log loss.
	public static final double DEFAULT_ALPHA=DprProver.MINALPH_DEFAULT;
	public static final double DEFAULT_EPSILON=DprProver.EPS_DEFAULT;
	public static final double DEFAULT_STAYPROB=DprProver.STAYPROB_DEFAULT;
	
	private double alpha;
	private double epsilon;
	private double stayProb;
	protected LossData cumloss;

	public AprSRW() {
		super();
		init(DEFAULT_ALPHA,DEFAULT_EPSILON,DEFAULT_STAYPROB);
	}
	
	public AprSRW(int maxT, double mu, double eta, WeightingScheme wScheme, double delta, File affgraph, double zeta) {
		super(maxT,mu,eta,wScheme,delta,affgraph,zeta);
		init(DEFAULT_ALPHA,DEFAULT_EPSILON,DEFAULT_STAYPROB);
	}
	
	public AprSRW(double ialpha, double iepsilon, double istayProb) {
		super(); 
		this.init(ialpha,iepsilon,istayProb);
	}
	public AprSRW(int maxT, double mu, double eta, WeightingScheme wScheme, double delta, File affgraph, double zeta,
			double ialpha, double iepsilon, double istayProb) {
		super(maxT,mu,eta,wScheme,delta,affgraph,zeta);
		this.init(ialpha,iepsilon,istayProb);
	}

	
	private void init(double ialpha, double iepsilon, double istayProb) {
		//set walk parameters here
		alpha = ialpha;
		epsilon = iepsilon;
		stayProb = istayProb;
		this.cumloss = new LossData();
	}
	
	@Override
	public Map<String, Double> gradient(ParamVector paramVec, PosNegRWExample<T> example) {
		// startNode maps node->weight
		Map<T,Double> query = example.getQueryVec();
		if (query.size() > 1) throw new UnsupportedOperationException("Can't do multi-node queries");
		T startNode = query.keySet().iterator().next();
		
		// gradient maps feature->gradient with respect to that feature
		Map<String,Double> gradient = null;
		
		// maps storing the probability and remainder weights of the nodes:
		HashMap<T,Double> p = new HashMap<T,Double>();
		HashMap<T,Double> r = new HashMap<T,Double>();
		
		// initializing the above maps:
		for(T node : example.getGraph().getNodes())
		{
			p.put(node, 0.0);
			r.put(node, 0.0);
		}
		
		r.putAll(query);
		
		// maps storing the gradients of p and r for each node:
		HashMap<T,Map<String,Double>> dp = new HashMap<T,Map<String,Double>>();
		HashMap<T,Map<String,Double>> dr = new HashMap<T,Map<String,Double>>();
		
		// initializing the above maps:
		for(T node : example.getGraph().getNodes())
		{
			dp.put(node, new HashMap<String,Double>());
			dr.put(node, new HashMap<String,Double>());
			for(String feature : (example.getGraph().getFeatureSet()))
			{
				dp.get(node).put(feature, 0.0);
				dr.get(node).put(feature, 0.0);
			}
		}
		
		// APR Algorithm:
		int completeCount = 0;
		while(completeCount < example.getGraph().getNumNodes())
		{
			log.debug("Starting pass");
			completeCount = 0;
			for(T u : example.getGraph().getNodes())
			{
				double ru = r.get(u);
				if(ru / (double) example.getGraph().nearNative(u).size() > epsilon)
					while(ru / example.getGraph().nearNative(u).size() > epsilon) {
						this.push(u, p, r, example.getGraph(), paramVec, dp, dr);
						if (r.get(u) > ru) throw new IllegalStateException("r increasing! :(");
						ru = r.get(u);
					}
				else {
					completeCount++;
					log.debug("Counting "+u);
				}
			}
			log.debug(completeCount +" of " + example.getGraph().getNumNodes() + " completed this pass");
		}
		
		for (String f : trainableFeatures(localFeatures(paramVec,example))) {
			this.cumloss.add(LOSS.REGULARIZATION, this.mu * Math.pow(Dictionary.safeGet(paramVec,f), 2));
		}
		double pmax = 0;
		for (T x : example.getPosList()) {
			double px = p.get(x);
			this.cumloss.add(LOSS.LOG, -Math.log(clip(px)));
			pmax = Math.max(pmax,px);
		}
		//negative instance booster
		double h = pmax + delta;
		double beta = 1;
		if(delta < 0.5) beta = (Math.log(1/h))/(Math.log(1/(1-h)));
		for (T x : example.getNegList()) {
			this.cumloss.add(LOSS.LOG, -Math.log(clip(1.0-p.get(x))));
		}
		
		gradient = dp.get(startNode);
		
		return gradient;
	}
	
	private double dotP(List<Feature> phi, ParamVector paramVec) {
		double dotP = 0;
		for(Feature feature : phi)
			dotP += paramVec.get(feature.featureName);
		return dotP;
	}
	
	private double clip(double prob) {
		if(prob <= 0)
		{
			prob = bound;
		}
		return prob;
	}
	
	public <T> double totalEdgeProbWeight(AnnotatedGraph<T> g, T u,  Map<String,Double> p) {
		double sum = 0.0;
		for (T v : g.nearNative(u).keySet()) {
			double ew = Math.max(0,edgeWeight(g,u,v,p)); 
			sum+=ew;
		}
		if (Double.isInfinite(sum)) return Double.MAX_VALUE;
		return sum;
	}
	
	/**
	 * Simulates a single lazy random walk step on the input vertex
	 * @param u the vertex to be 'pushed'
	 * @param p
	 * @param r
	 * @param graph
	 * @param paramVec
	 * @param dp
	 * @param dr
	 */
	public void push(T u, HashMap<T,Double> p, HashMap<T,Double> r, AnnotatedGraph<T> graph, ParamVector paramVec,
			HashMap<T,Map<String,Double>> dp, HashMap<T,Map<String,Double>> dr)
	{
		log.debug("Pushing "+u);
		
		// update p for the pushed node:
		Dictionary.increment(p, u, alpha * r.get(u));
		Map<String, Double> dru = dr.get(u);
		
		HashMap<T,Double> unwrappedDotP = new HashMap<T,Double>();
		for (T v : graph.nearNative(u).keySet()) unwrappedDotP.put(v, dotP(graph.phi(u,v),paramVec));
		
		// calculate the sum of the weights (raised to exp) of the edges adjacent to the input node:
		double rowSum = this.totalEdgeProbWeight(graph, u, paramVec);
		
		// calculate the gradients of the rowSums (needed for the calculation of the gradient of r):
		Map<String, Double> drowSums = new HashMap<String, Double>();
		Map<String, Double> prevdr = new HashMap<String, Double>();
		for(String feature : (graph.getFeatureSet()))
		{
//			log.debug("dru["+feature+"] = "+dru.get(feature));
			// simultaneously update the dp for the pushed node:
			if (trainable(feature)) Dictionary.increment(dp,u,feature,alpha * dru.get(feature));
			double drowSum = 0;
			for(T v : graph.nearNative(u).keySet())
			{
				if(Feature.contains(graph.phi(u, v), feature))
				{
					drowSum += this.weightingScheme.derivEdgeWeight(unwrappedDotP.get(v));
				}
			}
			drowSums.put(feature, drowSum);
			
			// update dr for the pushed vertex, storing dr temporarily for the calculation of dr for the other vertices:
			prevdr.put(feature, dru.get(feature));
			dru.put(feature, dru.get(feature) * (1 - alpha) * stayProb);
		}
		
		// update dr for other vertices:
		for(T v : graph.nearNative(u).keySet())
		{
			for(String feature : (graph.getFeatureSet()))
			{
				double dotP = this.weightingScheme.edgeWeightFunction(unwrappedDotP.get(v));
				double ddotP = this.weightingScheme.derivEdgeWeight(unwrappedDotP.get(v));
				int c = Feature.contains(graph.phi(u, v), feature) ? 1 : 0;
				double vdr = dr.get(v).get(feature);
				
				// whoa this is pretty gross.
				vdr += (1-stayProb)*(1-alpha)*((prevdr.get(feature)*dotP/rowSum)+(r.get(u)*((c*ddotP*rowSum)-(dotP*drowSums.get(feature)))/(rowSum*rowSum)));
				dr.get(v).put(feature, vdr);
			}
		}
		
		// update r for all affected vertices:
		double ru = r.get(u);
		r.put(u, ru * stayProb * (1 - alpha));
		for(T v : graph.nearNative(u).keySet())
		{
			// calculate edge weight on v:
			double dotP = this.weightingScheme.edgeWeightFunction(unwrappedDotP.get(v));
			Dictionary.increment(r, v, (1 - stayProb) * (1 - alpha) * (dotP / rowSum) * ru);
		}
	}
	
	@Override
	public LossData cumulativeLoss() {
		return cumloss.copy();
	}
	@Override
	public void clearLoss() {
		cumloss.clear(); // ?
	}
}
