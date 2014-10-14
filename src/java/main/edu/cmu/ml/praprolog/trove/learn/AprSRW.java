package edu.cmu.ml.praprolog.trove.learn;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.cmu.ml.praprolog.graph.Feature;
import edu.cmu.ml.praprolog.learn.tools.LossData;
import edu.cmu.ml.praprolog.learn.tools.LossData.LOSS;
import edu.cmu.ml.praprolog.trove.graph.AnnotatedTroveGraph;
import edu.cmu.ml.praprolog.trove.learn.tools.PosNegRWExample;
import edu.cmu.ml.praprolog.learn.tools.WeightingScheme;
import edu.cmu.ml.praprolog.prove.v1.DprProver;
import edu.cmu.ml.praprolog.util.Dictionary;
import edu.cmu.ml.praprolog.util.ParamVector;
import gnu.trove.iterator.TIntDoubleIterator;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;

public class AprSRW extends SRW<PosNegRWExample> {
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
	
	public AprSRW(int maxT, double mu, double eta, WeightingScheme wScheme, double delta) {
		super(maxT,mu,eta,wScheme,delta);
		init(DEFAULT_ALPHA,DEFAULT_EPSILON,DEFAULT_STAYPROB);
	}
	
	public AprSRW(double ialpha, double iepsilon, double istayProb) {
		super(); 
		this.init(ialpha,iepsilon,istayProb);
	}
	public AprSRW(int maxT, double mu, double eta, WeightingScheme wScheme, double delta,
			double ialpha, double iepsilon, double istayProb) {
		super(maxT,mu,eta,wScheme,delta);
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
	public TObjectDoubleHashMap<String> gradient(ParamVector paramVec, PosNegRWExample example) {
		// startNode maps node->weight
		TIntDoubleHashMap query = example.getQueryVec();
		if (query.size() > 1) throw new UnsupportedOperationException("Can't do multi-node queries");
		int startNode = query.keySet().iterator().next();
		
		// gradient maps feature->gradient with respect to that feature
		TObjectDoubleHashMap<String> gradient = null;
		
		// maps storing the probability and remainder weights of the nodes:
		TIntDoubleHashMap p = new TIntDoubleHashMap();
		TIntDoubleHashMap r = new TIntDoubleHashMap();
		
		// initializing the above maps:
		for(TIntIterator it = example.getGraph().getNodes().iterator(); it.hasNext(); )
		{
			int node = it.next();
			p.put(node, 0.0);
			r.put(node, 0.0);
		}
		
		r.putAll(query);
		
		// maps storing the gradients of p and r for each node:
		TIntObjectMap<TObjectDoubleHashMap<String>> dp = new TIntObjectHashMap<TObjectDoubleHashMap<String>>();
		TIntObjectMap<TObjectDoubleHashMap<String>> dr = new TIntObjectHashMap<TObjectDoubleHashMap<String>>();
		
		// initializing the above maps:
		for(TIntIterator it = example.getGraph().getNodes().iterator(); it.hasNext(); )
		{
			int node = it.next();
			dp.put(node, new TObjectDoubleHashMap<String>());
			dr.put(node, new TObjectDoubleHashMap<String>());
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
			for(TIntIterator it = example.getGraph().getNodes().iterator(); it.hasNext(); )
			{
				int u = it.next();
				double ru = r.get(u);
				if(ru / (double) example.getGraph().near(u).size() > epsilon)
					while(ru / example.getGraph().near(u).size() > epsilon) {
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
		for (int x : example.getPosList()) {
			double px = p.get(x);
			this.cumloss.add(LOSS.LOG, -Math.log(clip(px)));
			pmax = Math.max(pmax,px);
		}
		//negative instance booster
		double h = pmax + delta;
		double beta = 1;
		if(delta < 0.5) beta = (Math.log(1/h))/(Math.log(1/(1-h)));
		for (int x : example.getNegList()) {
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
	
	public double totalEdgeProbWeight(AnnotatedTroveGraph g, int u,  Map<String,Double> p) {
		double sum = 0.0;
		for (TIntDoubleIterator v = g.near(u).iterator(); v.hasNext();) {
			v.advance();
			double ew = Math.max(0,edgeWeight(g,u,v.key(),p)); 
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
	public void push(int u, TIntDoubleMap p, TIntDoubleMap r, AnnotatedTroveGraph graph, ParamVector paramVec,
			TIntObjectMap<TObjectDoubleHashMap<String>> dp, TIntObjectMap<TObjectDoubleHashMap<String>> dr)
	{
		log.debug("Pushing "+u);
		
		// update p for the pushed node:
		Dictionary.increment(p, u, alpha * r.get(u));
		TObjectDoubleHashMap<String> dru = dr.get(u);
		
		TIntDoubleMap unwrappedDotP = new TIntDoubleHashMap();
		for (TIntDoubleIterator v = graph.near(u).iterator(); v.hasNext();) {
			v.advance();
			unwrappedDotP.put(v.key(), dotP(graph.phi(u,v.key()),paramVec));
		}
		
		// calculate the sum of the weights (raised to exp) of the edges adjacent to the input node:
		double rowSum = this.totalEdgeProbWeight(graph, u, paramVec);
		
		// calculate the gradients of the rowSums (needed for the calculation of the gradient of r):
		TObjectDoubleHashMap<String> drowSums = new TObjectDoubleHashMap<String>();
		TObjectDoubleHashMap<String> prevdr = new TObjectDoubleHashMap<String>();
		for(String feature : (graph.getFeatureSet()))
		{
//			log.debug("dru["+feature+"] = "+dru.get(feature));
			// simultaneously update the dp for the pushed node:
			if (trainable(feature)) Dictionary.increment(dp,u,feature,alpha * dru.get(feature));
			double drowSum = 0;
			for(TIntDoubleIterator v = graph.near(u).iterator(); v.hasNext();)
			{
				v.advance();
				if(Feature.contains(graph.phi(u, v.key()), feature))
				{
					drowSum += this.weightingScheme.derivEdgeWeight(unwrappedDotP.get(v.key()));
				}
			}
			drowSums.put(feature, drowSum);
			
			// update dr for the pushed vertex, storing dr temporarily for the calculation of dr for the other vertices:
			prevdr.put(feature, dru.get(feature));
			dru.put(feature, dru.get(feature) * (1 - alpha) * stayProb);
		}
		
		// update dr for other vertices:
		for(TIntDoubleIterator v = graph.near(u).iterator(); v.hasNext();)
		{
			v.advance();
			for(String feature : (graph.getFeatureSet()))
			{
				double dotP = this.weightingScheme.edgeWeightFunction(unwrappedDotP.get(v.key()));
				double ddotP = this.weightingScheme.derivEdgeWeight(unwrappedDotP.get(v.key()));
				int c = Feature.contains(graph.phi(u, v.key()), feature) ? 1 : 0;
				double vdr = dr.get(v.key()).get(feature);
				
				// whoa this is pretty gross.
				vdr += (1-stayProb)*(1-alpha)*((prevdr.get(feature)*dotP/rowSum)+(r.get(u)*((c*ddotP*rowSum)-(dotP*drowSums.get(feature)))/(rowSum*rowSum)));
				dr.get(v.key()).put(feature, vdr);
			}
		}
		
		// update r for all affected vertices:
		double ru = r.get(u);
		r.put(u, ru * stayProb * (1 - alpha));
		for(TIntDoubleIterator v = graph.near(u).iterator(); v.hasNext();)
		{
			v.advance();
			// calculate edge weight on v:
			double dotP = this.weightingScheme.edgeWeightFunction(unwrappedDotP.get(v.key()));
			Dictionary.increment(r, v.key(), (1 - stayProb) * (1 - alpha) * (dotP / rowSum) * ru);
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
