package edu.cmu.ml.proppr.learn;


import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.graph.LearningGraph;
import edu.cmu.ml.proppr.learn.tools.LossData;
import edu.cmu.ml.proppr.learn.tools.LossData.LOSS;
import edu.cmu.ml.proppr.prove.DprProver;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.ParamVector;
import edu.cmu.ml.proppr.util.SRWOptions;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.iterator.TObjectDoubleIterator;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;

public class AprSRW extends SRW<PosNegRWExample> {
	private static final Logger log = Logger.getLogger(AprSRW.class);
	private static final double bound = 1.0e-15; //Prevent infinite log loss.
	public static final double DEFAULT_STAYPROB=DprProver.STAYPROB_DEFAULT;
	
	private double stayProb;
	protected LossData cumloss;

	public AprSRW() {
		super();
		init(DEFAULT_STAYPROB);
	}
	
	public AprSRW(SRWOptions params) {
		super(params);
		init(DEFAULT_STAYPROB);
	}
	
	public AprSRW(double istayProb) {
		super();
		this.init(istayProb);
	}
	public AprSRW(SRWOptions params, double istayProb) {
		super(params);
		this.init(istayProb);
	}

	
	private void init(double istayProb) {
		//set walk parameters here
		stayProb = istayProb;
		this.cumloss = new LossData();
	}
	
	@Override
	public TObjectDoubleMap<String> gradient(ParamVector<String,?> paramVec, PosNegRWExample example) {
		// startNode maps node->weight
		TIntDoubleMap query = example.getQueryVec();
		if (query.size() > 1) throw new UnsupportedOperationException("Can't do multi-node queries");
		int startNode = query.keySet().iterator().next();
		
		// gradient maps feature->gradient with respect to that feature
		TObjectDoubleMap<String> gradient = null;
		
		// maps storing the probability and remainder weights of the nodes:
		TIntDoubleMap p = new TIntDoubleHashMap();
		TIntDoubleMap r = new TIntDoubleHashMap();
		
		// initializing the above maps:
		for(int node : example.getGraph().getNodes()) {
			p.put(node, 0.0);
			r.put(node, 0.0);
		}
		
		r.putAll(query);
		
		// maps storing the gradients of p and r for each node:
		TIntObjectMap<TObjectDoubleMap<String>> dp = new TIntObjectHashMap<TObjectDoubleMap<String>>();
		TIntObjectMap<TObjectDoubleMap<String>> dr = new TIntObjectHashMap<TObjectDoubleMap<String>>();
		
		// initializing the above maps:
		for(int node : example.getGraph().getNodes()) {
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
		while(completeCount < example.getGraph().nodeSize()) {
			log.debug("Starting pass");
			completeCount = 0;
			for(int u : example.getGraph().getNodes()) {
				double ru = r.get(u);
				if(ru / (double) example.getGraph().near(u).size() > c.apr.epsilon)
					while(ru / example.getGraph().near(u).size() > c.apr.epsilon) {
						this.push(u, p, r, example.getGraph(), paramVec, dp, dr);
						if (r.get(u) > ru) throw new IllegalStateException("r increasing! :(");
						ru = r.get(u);
					}
				else {
					completeCount++;
					log.debug("Counting "+u);
				}
			}
			log.debug(completeCount +" of " + example.getGraph().nodeSize() + " completed this pass");
		}
		
		for (String f : trainableFeatures(localFeatures(paramVec,example))) {
			this.cumloss.add(LOSS.REGULARIZATION, c.mu * Math.pow(Dictionary.safeGet(paramVec,f), 2));
		}
		double pmax = 0;
		for (int x : example.getPosList()) {
			double px = p.get(x);
			this.cumloss.add(LOSS.LOG, -Math.log(clip(px)));
			pmax = Math.max(pmax,px);
		}
		//negative instance booster
		double h = pmax + c.delta;
		double beta = 1;
		if(c.delta < 0.5) beta = (Math.log(1/h))/(Math.log(1/(1-h)));
		for (int x : example.getNegList()) {
			this.cumloss.add(LOSS.LOG, -Math.log(clip(1.0-p.get(x))));
		}
		
		gradient = dp.get(startNode);
		
		return gradient;
	}
	
	private double dotP(TObjectDoubleMap<String> phi, ParamVector<String,?> paramVec) {
		double dotP = 0;
		for (TObjectDoubleIterator<String> f = phi.iterator(); f.hasNext();) {
			f.advance();
			dotP += paramVec.get(f.key()) * f.value();
		}
		return dotP;
	}
	
	private double clip(double prob) {
		if(prob <= 0)
		{
			prob = bound;
		}
		return prob;
	}
	
	public double totalEdgeProbWeight(LearningGraph g, int u,  ParamVector<String, ?> p) {
		double sum = 0.0;
		for (TIntIterator it = g.near(u).iterator(); it.hasNext();) {
			int v = it.next();
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
	public void push(int u, TIntDoubleMap p, TIntDoubleMap r, LearningGraph graph, ParamVector<String,?> paramVec,
			TIntObjectMap<TObjectDoubleMap<String>> dp, TIntObjectMap<TObjectDoubleMap<String>> dr) {
		log.debug("Pushing "+u);
		
		// update p for the pushed node:
		Dictionary.increment(p, u, c.apr.alpha * r.get(u));
		TObjectDoubleMap<String> dru = dr.get(u);
		
		TIntDoubleMap unwrappedDotP = new TIntDoubleHashMap();
		for (TIntIterator it = graph.near(u).iterator(); it.hasNext();) {
			int v = it.next();
			unwrappedDotP.put(v, dotP(graph.getFeatures(u,v),paramVec));
		}
		
		// calculate the sum of the weights (raised to exp) of the edges adjacent to the input node:
		double rowSum = this.totalEdgeProbWeight(graph, u, paramVec);
		
		// calculate the gradients of the rowSums (needed for the calculation of the gradient of r):
		TObjectDoubleMap<String> drowSums = new TObjectDoubleHashMap<String>();
		TObjectDoubleMap<String> prevdr = new TObjectDoubleHashMap<String>();
		for(String feature : (graph.getFeatureSet())) {
//			log.debug("dru["+feature+"] = "+dru.get(feature));
			// simultaneously update the dp for the pushed node:
			if (trainable(feature)) Dictionary.increment(dp,u,feature,c.apr.alpha * dru.get(feature));
			double drowSum = 0;
			for (TIntIterator it = graph.near(u).iterator(); it.hasNext();) {
				int v = it.next();
				if(graph.getFeatures(u, v).containsKey(feature)) {
					drowSum += c.weightingScheme.derivEdgeWeight(unwrappedDotP.get(v));
				}
			}
			drowSums.put(feature, drowSum);
			
			// update dr for the pushed vertex, storing dr temporarily for the calculation of dr for the other vertices:
			prevdr.put(feature, dru.get(feature));
			dru.put(feature, dru.get(feature) * (1 - c.apr.alpha) * stayProb);
		}
		
		// update dr for other vertices:
		for (TIntIterator it = graph.near(u).iterator(); it.hasNext();) {
			int v = it.next();
			for(String feature : (graph.getFeatureSet())) {
				double dotP = c.weightingScheme.edgeWeight(unwrappedDotP.get(v));
				double ddotP = c.weightingScheme.derivEdgeWeight(unwrappedDotP.get(v));
				int contained = graph.getFeatures(u, v).containsKey(feature) ? 1 : 0;
				double vdr = dr.get(v).get(feature);
				
				// whoa this is pretty gross.
				vdr += (1-stayProb)*(1-c.apr.alpha)*((prevdr.get(feature)*dotP/rowSum)+(r.get(u)*((contained*ddotP*rowSum)-(dotP*drowSums.get(feature)))/(rowSum*rowSum)));
				dr.get(v).put(feature, vdr);
			}
		}
		
		// update r for all affected vertices:
		double ru = r.get(u);
		r.put(u, ru * stayProb * (1 - c.apr.alpha));
		for (TIntIterator it = graph.near(u).iterator(); it.hasNext();) {
			int v = it.next();
			// calculate edge weight on v:
			double dotP = c.weightingScheme.edgeWeight(unwrappedDotP.get(v));
			Dictionary.increment(r, v, (1 - stayProb) * (1 - c.apr.alpha) * (dotP / rowSum) * ru);
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
