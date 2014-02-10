package edu.cmu.ml.praprolog.learn;

public abstract class WeightingScheme {
	public abstract double edgeWeightFunction(double product);
	public abstract double derivEdgeWeightFunction(double weight);
}
