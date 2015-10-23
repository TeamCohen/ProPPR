package edu.cmu.ml.proppr.util;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.learn.SRW;
import edu.cmu.ml.proppr.learn.tools.ReLU;
import edu.cmu.ml.proppr.learn.tools.SquashingFunction;
import edu.cmu.ml.proppr.learn.tools.FixedWeightFilter;
import edu.cmu.ml.proppr.prove.DprProver;

public class SRWOptions {
	protected static final Logger log = Logger.getLogger(SRWOptions.class);

	public static final double DEFAULT_MU=.001;
	public static final double DEFAULT_ETA=1.0;
	public static final double DEFAULT_DELTA=0.5;
	public static final double DEFAULT_ZETA=0;
	public static final File DEFAULT_AFFGRAPH=null;
	public static SquashingFunction DEFAULT_SQUASHING_FUNCTION() { return new ReLU(); }

	
	private enum names {
		mu,
		eta,
		delta,
		zeta,
		affinityFile,
		squashingFunction,
		apr,
		fix
	}
	
	/** regularization */
	public double mu;
	/** learning rate */
	public double eta;
	/** 
	 * Negative instance booster. 
	 * If set < 0.5, the gradient component controlled by negative examples is 
	 * increased by a factor of:
	 * log(1/h)/log(1/(1-h))
	 * where h = max(p|positive examples) + delta */
	public double delta;
	/** local L1 group lasso / laplacian */
	public double zeta; 
	/** local L1 group lasso / laplacian */
	public File affinityFile;
	/** local L1 group lasso / laplacian */
	public Map<String,List<String>> affinity; 
	/** local L1 group lasso / laplacian */
	public Map<String,Integer> diagonalDegree;
	/** wrapper function */
	public SquashingFunction squashingFunction;
	/** minalpha projection */
	public APROptions apr;
	/** specification for which features are fixed **/
	public FixedWeightFilter fixedWeights;
	
	/** */
	public SRWOptions(APROptions options, SquashingFunction fn) {
		this(
				DEFAULT_MU, 
				DEFAULT_ETA, 
				fn, 
				DEFAULT_DELTA, 
				DEFAULT_AFFGRAPH, 
				DEFAULT_ZETA, 
				options,
				new FixedWeightFilter());
	}
	public SRWOptions() {
		this(
				DEFAULT_MU, 
				DEFAULT_ETA, 
				DEFAULT_SQUASHING_FUNCTION(), 
				DEFAULT_DELTA, 
				DEFAULT_AFFGRAPH, 
				DEFAULT_ZETA, 
				new APROptions(),
				new FixedWeightFilter()); 
	}
	public SRWOptions(
			double mu, 
			double eta, 
			SquashingFunction f, 
			double delta, 
			File affgraph, 
			double zeta,
			APROptions options,
			FixedWeightFilter fixedWeights) {
		this.mu = mu;
		this.eta = eta;
		this.delta = delta;
		this.zeta = zeta;
		this.squashingFunction = f;
		this.apr = options;
		this.affinityFile = affgraph;
		this.fixedWeights = fixedWeights;
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
			case eta: this.eta = Double.parseDouble(setting[1]); return;
			case delta: this.delta = Double.parseDouble(setting[1]); return;
			case zeta: this.zeta = Double.parseDouble(setting[1]); return;
			case affinityFile: 
				File value = new File(setting[1]);
				if (!value.exists()) throw new IllegalArgumentException("File '"+value.getName()+"' must exist");
				this.affinityFile = value; 
				return;
			case apr: this.apr.set(new String[] { setting[1], setting[2] });
				return;
			case fix: 
				this.fixedWeights = new FixedWeightFilter( setting[1] );
				log.info("fixed-weight rule: "+setting[1]+" i.e. " + this.fixedWeights.toString());
				return;
		}
	}
}
