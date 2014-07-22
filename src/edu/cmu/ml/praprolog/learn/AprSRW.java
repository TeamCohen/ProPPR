package edu.cmu.ml.praprolog.learn;

import java.util.HashMap;
import java.util.Map;

import edu.cmu.ml.praprolog.graph.Feature;
import edu.cmu.ml.praprolog.learn.tools.PosNegRWExample;
import edu.cmu.ml.praprolog.util.ParamVector;

public class AprSRW<T> extends SRW<PosNegRWExample<T>> {
	
	public static double alpha;
	public static double epsilon;
	public static double stayProb;
	
	public ParamVector paramVec;
	Map<T,Double> p;
	Map<T,Double> r;
	Map<T,Map<String,Double>> dp;
	Map<T,Map<String,Double>> dr;
	PosNegRWExample<T> example;
	
	public AprSRW() {
		//set walk parameters here
		alpha = .1;
		epsilon = .0001;
		stayProb = 0.0;
	}
	
	@Override
	public Map<String, Double> gradient(ParamVector paramVec, PosNegRWExample<T> example) {
		// startNode maps node->weight
		Map<T,Double> startNode = example.getQueryVec();
		
		// gradient maps feature->gradient with respect to that feature
		Map<String,Double> gradient = null;
		
		// maps storing the probability and remainder weights of the nodes:
		p = new HashMap<T,Double>();
		r = new HashMap<T,Double>();
		
		// initializing the above maps:
		for(T node : example.getGraph().getNodes())
		{
			p.put(node, 0.0);
			r.put(node, 0.0);
		}
		
		for(T node : startNode.keySet())
			if(startNode.get(node) != null)
				r.put(node, startNode.get(node));
		
		// maps storing the gradients of p and r for each node:
		dp = new HashMap<T,Map<String,Double>>();
		dr = new HashMap<T,Map<String,Double>>();
		
		// initializing the above maps:
		for(T node : example.getGraph().getNodes())
		{
			dp.put(node, new HashMap<String,Double>());
			dr.put(node, new HashMap<String,Double>());
			for(String feature : example.getGraph().getFeatureSet())
			{
				dp.get(node).put(feature, 0.0);
				dr.get(node).put(feature, 0.0);
			}
		}
		
		// initialize paramVec and example:
		this.paramVec = paramVec;
		this.example = example;
		
		// APR Algorithm:
		int isDone = 0;
		while(isDone < example.getGraph().getNumNodes())
		{
			isDone = 0;
			for(T node : example.getGraph().getNodes())
			{
				if(r.get(node) / (double) example.getGraph().nearNative(node).size() > epsilon)
					while(r.get(node) / example.getGraph().nearNative(node).size() > epsilon)
						push(node);
				else
					isDone++;
			}
		}
		
		//for(T node : dp.keySet())
		//	for(String feature : dp.get(node).keySet())
		//		System.out.println(feature + ": " + dp.get(node).get(feature));
		
		// Returning the gradient of the specified node:
		T start = null;
		int i = 0;
		for(T node : startNode.keySet())
			if(startNode.get(node) == 1)
				start = node;
		gradient = dp.get(start);
		
		return gradient;
	}
	
	/**
	 * Simulates a single lazy random walk step on the input vertex
	 * @param node the vertex to be 'pushed'
	 */
	public void push(T node)
	{
		// update p for the pushed node:
		p.put(node, p.get(node) + alpha * r.get(node));
		
		// calculate the sum of the weights (raised to exp) of the edges adjacent to the input node:
		double rowSum = 0;
		for(T aNode : example.getGraph().nearNative(node).keySet())
		{
			double dotP = 0;
			for(Feature feature : example.getGraph().phi(node, aNode))
				dotP += paramVec.get(feature.featureName);
			rowSum += Math.exp(dotP);
		}
		
		// calculate the gradients of the rowSums (needed for the calculation of the gradient of r):
		Map<String, Double> drowSums = new HashMap<String, Double>();
		for(String feature : example.getGraph().getFeatureSet())
		{
			// simultaneously update the dp for the pushed node:
			dp.get(node).put(feature, dp.get(node).get(feature) + alpha * dr.get(node).get(feature));
			double drowSum = 0;
			for(T aNode : example.getGraph().nearNative(node).keySet())
			{
				boolean containsF = false;
				for(Feature f : example.getGraph().phi(node, aNode))
					if(feature.equals(f.featureName))
						containsF = true;
				//for(Feature f : example.getGraph().phi(aNode, node))
				//	if(feature.equals(f.featureName))
				//		containsF = true;
				if(containsF)
				{
					double dotP = 0;
					for(Feature f : example.getGraph().phi(node, aNode))
						dotP += paramVec.get(f.featureName);
					drowSum += Math.exp(dotP);
				}
			}
			drowSums.put(feature, drowSum);
		}
		
		// update dr for the pushed vertex, storing dr temporarily for the calculation of dr for the other vertecies:
		Map<String, Double> ndr = new HashMap<String, Double>();
		for(String feature : example.getGraph().getFeatureSet())
		{
			ndr.put(feature, dr.get(node).get(feature));
			dr.get(node).put(feature, dr.get(node).get(feature) * (1 - alpha) * stayProb);
		}
		
		// update dr for other vertecies:
		for(T aNode : example.getGraph().nearNative(node).keySet())
		{
			for(String feature : example.getGraph().getFeatureSet())
			{
				double dotP = 0;
				int c = 0;//the gradient of dotP w/ respect to feature
				for(Feature f : example.getGraph().phi(node, aNode))
				{
					dotP += paramVec.get(f.featureName);
					if(f.featureName.equals(feature))
						c = 1;
				}
				//for(Feature f : example.getGraph().phi(aNode, node))
				//{
				//	dotP += paramVec.get(f.featureName);
				//	if(f.featureName.equals(feature))
				//		c = 1;
				//}
				dotP = Math.exp(dotP);
				double aNdr = dr.get(aNode).get(feature);
				//if(feature.equals("fromr"))
				//	System.out.println(c + " : " + drowSums.get(feature) + " : " + rowSum);
				aNdr += (1-stayProb)*(1-alpha)*((ndr.get(feature)*dotP/rowSum)+(r.get(node)*((c*dotP*rowSum)-(dotP*drowSums.get(feature)))/(rowSum*rowSum)));
				dr.get(aNode).put(feature, aNdr);
			}
		}
		
		// update r for all affected vertecies:
		double nr = r.get(node);
		r.put(node, r.get(node) * stayProb * (1 - alpha));
		for(T aNode : example.getGraph().nearNative(node).keySet())
		{
			// calculate edge weight on aNode:
			double dotP = 0;
			for(Feature f : example.getGraph().phi(node, aNode))
				dotP += paramVec.get(f.featureName);
			dotP = Math.exp(dotP);
			r.put(aNode, r.get(aNode) + (1 - stayProb) * (1 - alpha) * (dotP / rowSum) * nr);
		}
	}
}
