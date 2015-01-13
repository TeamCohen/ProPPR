package edu.cmu.ml.praprolog.learn.tools;

import java.io.File;
import java.util.List;
import java.util.Map;

import edu.cmu.ml.praprolog.learn.SRW;
import edu.cmu.ml.praprolog.prove.DprProver;

public class SRWParameters {
	public static final int DEFAULT_MAX_T=10;
	public static final double DEFAULT_MU=.001;
	public static final double DEFAULT_ETA=1.0;
	public static final double DEFAULT_DELTA=0.5;
	public static final double DEFAULT_ZETA=0;
	public static final File DEFAULT_AFFGRAPH=null;
	public static WeightingScheme DEFAULT_WEIGHTING_SCHEME() { return new ReLUWeightingScheme(); }

	
	private enum names {
		mu,
		maxT,
		eta,
		delta,
		zeta,
		affinityFile,
		alpha,
		weightingScheme
	}
	
	/** regularization */
	public double mu;
	/** iterations */
	public int maxT;
	/** learning rate */
	public double eta;
	/** negative instance booster */
	public double delta;
	/** local L1 group lasso / laplacian */
	public double zeta; 
	/** local L1 group lasso / laplacian */
	public File affinityFile;
	/** local L1 group lasso / laplacian */
	public Map<String,List<String>> affinity; 
	/** local L1 group lasso / laplacian */
	public Map<String,Integer> diagonalDegree;
	/** minalpha projection */
	public double alpha;
	/** wrapper function */
	public WeightingScheme weightingScheme;
	
	/** */
	public SRWParameters() { this(DEFAULT_MAX_T); }
	public SRWParameters(int maxT) {
		this(
				maxT, 
				DEFAULT_MU, 
				DEFAULT_ETA, 
				DEFAULT_WEIGHTING_SCHEME(), 
				DEFAULT_DELTA, 
				DEFAULT_AFFGRAPH, 
				DEFAULT_ZETA, 
				DprProver.MINALPH_DEFAULT); 
	}
	public SRWParameters(
			int maxT, 
			double mu, 
			double eta, 
			WeightingScheme wScheme, 
			double delta, 
			File affgraph, 
			double zeta,
			double alpha) {
		this.maxT = maxT;
		this.mu = mu;
		this.eta = eta;
		this.delta = delta;
		this.zeta = zeta;
		this.weightingScheme = wScheme;
		this.alpha = alpha;
		this.affinityFile = affgraph;
	}

	public void init() {
		if(zeta>0){
			affinity = SRW.constructAffinity(affinityFile);
			diagonalDegree = SRW.constructDegree(affinity);
		} else {
			affinity = null;
			diagonalDegree = null;
		}
	}
	public void set(String...setting) {
		switch(names.valueOf(setting[0])) {
		case mu: this.mu = Double.parseDouble(setting[1]); return;
		case maxT: this.maxT = Integer.parseInt(setting[1]); return;
		case eta: this.eta = Double.parseDouble(setting[1]); return;
		case delta: this.delta = Double.parseDouble(setting[1]); return;
		case zeta: this.zeta = Double.parseDouble(setting[1]); return;
		case affinityFile: 
			File value = new File(setting[1]);
			if (!value.exists()) throw new IllegalArgumentException("File '"+value.getName()+"' must exist");
			this.affinityFile = value; 
			return;
		case alpha: this.alpha = Double.parseDouble(setting[1]); return;
		}
	}
}
