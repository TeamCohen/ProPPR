package edu.cmu.ml.proppr.learn;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.examples.PprExample;
import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.graph.LearningGraph;
import edu.cmu.ml.proppr.graph.LearningGraph;
import edu.cmu.ml.proppr.learn.SRW.ZeroGradientData;
import edu.cmu.ml.proppr.learn.tools.LossData;
import edu.cmu.ml.proppr.learn.tools.LossData.LOSS;
import edu.cmu.ml.proppr.learn.tools.ReLU;
import edu.cmu.ml.proppr.learn.tools.SquashingFunction;
import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.SRWOptions;
import edu.cmu.ml.proppr.util.SimpleSymbolTable;
import edu.cmu.ml.proppr.util.math.ParamVector;
import edu.cmu.ml.proppr.util.math.SimpleParamVector;
import gnu.trove.iterator.TIntDoubleIterator;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;

/**
 * Random walk learning
 * 
 * Flow of information:
 * 
 * 	 Train on example =
 *     load (initialize example parameters and compute M/dM)
 *     inference (compute p/dp)
 *     sgd (compute empirical loss gradient and apply to parameters)
 * 
 *   Accumulate gradient = 
 *     load  (initialize example parameters and compute M/dM)
 *     inference (compute p/dp)
 *     gradient (compute empirical loss gradient)
 * 
 * @author krivard
 *
 */
public class SRW {	
	private static final Logger log = Logger.getLogger(SRW.class);
	private static final double BOUND = 1.0e-15; //Prevent infinite log loss.
	private static final int MAX_ZERO_LOGS = 10;
	private static Random random = new Random();
	public static final String FIXED_WEIGHT_FUNCTOR="fixedWeight";
	public static void seed(long seed) { random.setSeed(seed); }
	public static SquashingFunction DEFAULT_SQUASHING_FUNCTION() { return new ReLU(); }
	protected Set<String> untrainedFeatures;
	protected int epoch;
	protected SRWOptions c;
	protected LossData cumloss;
	protected ZeroGradientData zeroGradientData;
	protected int zeroLogsThisEpoch=0;
	public SRW() { this(new SRWOptions()); }
	public SRW(SRWOptions params) {
		this.c = params;
		this.epoch = 1;
		this.untrainedFeatures = new TreeSet<String>();
		this.cumloss = new LossData();
		this.zeroGradientData = new ZeroGradientData();
	}

	/**
	 * Modify the parameter vector by taking a gradient step along the dir suggested by this example.
	 * @param params
	 * @param example
	 */
	public void trainOnExample(ParamVector params, PosNegRWExample example) {
		log.info("Training on "+example);

		initializeFeatures(params, example.getGraph());
		prepareForExample(params, example.getGraph(), params);
		load(params, example);
		inference(params, example);
		sgd(params, example);
	}

	public void accumulateGradient(ParamVector params, PosNegRWExample example, ParamVector accumulator) {
		log.info("Gradient calculating on "+example);

		initializeFeatures(params, example.getGraph());
		ParamVector<String,Double> prepare = new SimpleParamVector<String>();
		prepareForExample(params, example.getGraph(), prepare);
		load(params, example);
		inference(params, example);
		TIntDoubleMap gradient = gradient(params,example);
		
		for (Map.Entry<String, Double> e : prepare.entrySet()) {
			if (trainable(e.getKey())) 
				accumulator.adjustValue(e.getKey(), -e.getValue() / example.length());
		}
		for (TIntDoubleIterator it = gradient.iterator(); it.hasNext(); ) {
			it.advance();
			String feature = example.getGraph().featureLibrary.getSymbol(it.key());
			if (trainable(feature)) accumulator.adjustValue(example.getGraph().featureLibrary.getSymbol(it.key()), it.value() / example.length());
		}
	}


	/** fills M, dM in ex **/
	protected void load(ParamVector params, PosNegRWExample example) {
		PprExample ex = (PprExample) example;
		int dM_cursor=0;
		for (int uid = 0; uid < ex.getGraph().node_hi; uid++) {
			// (a); (b): initialization
			double tu = 0;
			TIntDoubleMap dtu = new TIntDoubleHashMap();
			int udeg = ex.getGraph().node_near_hi[uid] - ex.getGraph().node_near_lo[uid];
			double[] suv = new double[udeg];
			double[][] dfu = new double[udeg][];
			// begin (c): for each neighbor v of u,
			for(int eid = ex.getGraph().node_near_lo[uid], xvi = 0; eid < ex.getGraph().node_near_hi[uid]; eid++, xvi++) {
				int vid = ex.getGraph().edge_dest[eid];
				// i. s_{uv} = w * phi_{uv}, a scalar:
				suv[xvi] = 0;
				for (int lid = ex.getGraph().edge_labels_lo[eid]; lid < ex.getGraph().edge_labels_hi[eid]; lid++) {
					suv[xvi] += params.get(ex.getGraph().featureLibrary.getSymbol(ex.getGraph().label_feature_id[lid])) * ex.getGraph().label_feature_weight[lid];
				}
				// ii. t_u += f(s_{uv}), a scalar:
				tu += c.squashingFunction.edgeWeight(suv[xvi]);
				// iii. df_{uv} = f'(s_{uv})* phi_{uv}, a vector, as sparse as phi_{uv}
				// by looping over features i in phi_{uv}
				double [] dfuv = new double[ex.getGraph().edge_labels_hi[eid] - ex.getGraph().edge_labels_lo[eid]] ;
				double cee = c.squashingFunction.computeDerivative(suv[xvi]);
				for (int lid = ex.getGraph().edge_labels_lo[eid], dfuvi = 0; lid < ex.getGraph().edge_labels_hi[eid]; lid++, dfuvi++) {
					// iii. again
					dfuv[dfuvi] = cee * ex.getGraph().label_feature_weight[lid];
					// iv. dt_u += df_{uv}, a vector, as sparse as sum_{v'} phi_{uv'}
					// by looping over features i in df_{uv} 
					// (identical to features i in phi_{uv}, so we use the same loop)
					dtu.adjustOrPutValue(ex.getGraph().label_feature_id[lid], dfuv[dfuvi], dfuv[dfuvi]);
				}
				dfu[xvi] = dfuv;
			}
			// end (c)

			// begin (d): for each neighbor v of u,
			double scale = (1 / (tu*tu));
			for(int eid = ex.getGraph().node_near_lo[uid], xvi = 0; eid < ex.getGraph().node_near_hi[uid]; eid++, xvi++) {
				int vid = ex.getGraph().edge_dest[eid];
				ex.dM_lo[uid][xvi] = dM_cursor;//dM_features.size();
				// create the vector dM_{uv} = (1/t^2_u) * (t_u * df_{uv} - f(s_{uv}) * dt_u)
				// by looping over features i in dt_u
				
				// getting the df offset for features in dt_u is awkward, so we'll first iterate over features in df_uv,
				// then fill in the rest
				int[] seenFeatures = new int[ex.getGraph().edge_labels_hi[eid] - ex.getGraph().edge_labels_lo[eid]];
				for (int lid = ex.getGraph().edge_labels_lo[eid], dfuvi = 0; lid < ex.getGraph().edge_labels_hi[eid]; lid++, dfuvi++) {
					int fid = ex.getGraph().label_feature_id[lid];
					ex.dM_feature_id[dM_cursor] = fid; //dM_features.add(fid);
					double dMuvi = (tu * dfu[xvi][dfuvi] - c.squashingFunction.edgeWeight(suv[xvi]) * dtu.get(fid));
					if (tu == 0) { 
						if (dMuvi != 0)
							throw new IllegalStateException("tu=0 at u="+uid+"; example "+ex.toString()); 
					} else dMuvi *= scale; 
					ex.dM_value[dM_cursor] = dMuvi; //dM_values.add(dMuvi);
					dM_cursor++;
					seenFeatures[dfuvi] = fid; //save this feature so we can skip it later
				}
				Arrays.sort(seenFeatures);
				// we've hit all the features in df_uv, now we do the remaining features in dt_u:
				for (TIntDoubleIterator it = dtu.iterator(); it.hasNext(); ) {
					it.advance();
					// skip features we already added in the df_uv loop
					if (Arrays.binarySearch(seenFeatures, it.key())>=0) continue;
					ex.dM_feature_id[dM_cursor] = it.key();//dM_features.add(it.key());
					// zero the first term, since df_uv doesn't cover this feature
					double dMuvi = scale * ( - c.squashingFunction.edgeWeight(suv[xvi]) * it.value());
					ex.dM_value[dM_cursor] = dMuvi; //dM_values.add(dMuvi);
					dM_cursor++;
				}
				ex.dM_hi[uid][xvi] = dM_cursor;//dM_features.size();
				// also create the scalar M_{uv} = f(s_{uv}) / t_u
				ex.M[uid][xvi] = c.squashingFunction.edgeWeight(suv[xvi]);
				if (tu==0) {
					if (ex.M[uid][xvi] != 0) throw new IllegalStateException("tu=0 at u="+uid+"; example "+ex.toString());
				} else ex.M[uid][xvi] /= tu;
			}
		}
	}

	/** adds new features to params vector @ 1% random perturbation */
	public void initializeFeatures(ParamVector params, LearningGraph graph) {
		for (String f : graph.getFeatureSet()) {
			if (!params.containsKey(f)) {
				params.put(f,c.squashingFunction.defaultValue()+ (trainable(f) ? 0.01*random.nextDouble() : 0));
			}
		}
	}

	/** fills p, dp 
	 * @param params */
	protected void inference(ParamVector params, PosNegRWExample example) {
		PosNegRWExample ex = (PosNegRWExample) example;
		ex.p = new double[ex.getGraph().node_hi];
		ex.dp = new TIntDoubleMap[ex.getGraph().node_hi];
		Arrays.fill(ex.p,0.0);
		// copy query into p
		for (TIntDoubleIterator it = ex.getQueryVec().iterator(); it.hasNext(); ) {
			it.advance();
			ex.p[it.key()] = it.value();
		}
		for (int i=0; i<c.apr.maxDepth; i++) {
			inferenceUpdate(ex);
		}

	}
	protected void inferenceUpdate(PosNegRWExample example) {
		PprExample ex = (PprExample) example;
		double[] pNext = new double[ex.getGraph().node_hi];
		TIntDoubleMap[] dNext = new TIntDoubleMap[ex.getGraph().node_hi];
		// p: 2. for each node u
		for (int uid = 0; uid < ex.getGraph().node_hi; uid++) {
			// p: 2(a) p_u^{t+1} += alpha * s_u
			pNext[uid] += c.apr.alpha * Dictionary.safeGet(ex.getQueryVec(), uid, 0.0);
			// p: 2(b) for each neighbor v of u:
			for(int eid = ex.getGraph().node_near_lo[uid], xvi = 0; eid < ex.getGraph().node_near_hi[uid]; eid++, xvi++) {
				int vid = ex.getGraph().edge_dest[eid];
				// p: 2(b)i. p_v^{t+1} += (1-alpha) * p_u^t * M_uv
				pNext[vid] += (1-c.apr.alpha) * ex.p[uid] * ex.M[uid][xvi];
				// d: i. for each feature i in dM_uv:
				if (dNext[vid] == null)
					dNext[vid] = new TIntDoubleHashMap(ex.dM_hi[uid][xvi] - ex.dM_lo[uid][xvi]);
				for (int dmi = ex.dM_lo[uid][xvi]; dmi < ex.dM_hi[uid][xvi]; dmi++) {
					// d_vi^{t+1} += (1-alpha) * p_u^{t} * dM_uvi
					if (ex.dM_value[dmi]==0) continue;
					double inc = (1-c.apr.alpha) * ex.p[uid] * ex.dM_value[dmi];
					dNext[vid].adjustOrPutValue(ex.dM_feature_id[dmi], inc, inc);
				}
				// d: ii. for each feature i in d_u^t
				if (ex.dp[uid] == null) continue; // skip when d is empty
				for (TIntDoubleIterator it = ex.dp[uid].iterator(); it.hasNext();) {
					it.advance();
					if (it.value()==0) continue;
					// d_vi^{t+1} += (1-alpha) * d_ui^t * M_uv
					double inc = (1-c.apr.alpha) * it.value() * ex.M[uid][xvi];
					dNext[vid].adjustOrPutValue(it.key(),inc,inc);
				}
			}
		}
		
		// sanity check on p
		if (log.isDebugEnabled()) {
			double sum = 0;
			for (double d : pNext) sum += d;
			if (Math.abs(sum - 1.0) > c.apr.epsilon)
				log.error("invalid p computed: "+sum);
		}
		ex.p = pNext;
		ex.dp = dNext;
	}

	/** edits params */
	protected void sgd(ParamVector params, PosNegRWExample ex) {
		TIntDoubleMap gradient = gradient(params,ex);
		// apply gradient to param vector
		for (TIntDoubleIterator grad = gradient.iterator(); grad.hasNext(); ) {
			grad.advance();
			if (grad.value()==0) continue;
			String feature = ex.getGraph().featureLibrary.getSymbol(grad.key());
			if (trainable(feature)) params.adjustValue(feature, - learningRate() * grad.value());
		}
	}

	protected TIntDoubleMap gradient(ParamVector params, PosNegRWExample example) {
		PosNegRWExample ex = (PosNegRWExample) example;
		Set<String> features = this.localFeatures(params, ex.getGraph());
		TIntDoubleMap gradient = new TIntDoubleHashMap(features.size());
		// add regularization term
		regularization(params, ex, gradient);
		
		int nonzero=0;
		double mag = 0;
		
		// add empirical loss gradient term
		// positive examples
		double pmax = 0;
		for (int a : ex.getPosList()) {
			double pa = clip(ex.p[a]);
			if(pa > pmax) pmax = pa;
			for (TIntDoubleIterator da = ex.dp[a].iterator(); da.hasNext(); ) {
				da.advance();
				if (da.value()==0) continue;
				nonzero++;
				double aterm = -da.value() / pa;
				mag += aterm*aterm;
				gradient.adjustOrPutValue(da.key(), aterm, aterm);
			}
			if (log.isDebugEnabled()) log.debug("+p="+pa);
			this.cumloss.add(LOSS.LOG, -Math.log(pa));
		}

		//negative instance booster
		double h = pmax + c.delta;
		double beta = 1;
		if(c.delta < 0.5) beta = (Math.log(1/h))/(Math.log(1/(1-h)));

		// negative examples
		for (int b : ex.getNegList()) {
			double pb = clip(ex.p[b]);
			for (TIntDoubleIterator db = ex.dp[b].iterator(); db.hasNext(); ) {
				db.advance();
				if (db.value()==0) continue;
				nonzero++;
				double bterm = beta * db.value() / (1 - pb);
				mag += bterm*bterm;
				gradient.adjustOrPutValue(db.key(), bterm, bterm);
			}
			if (log.isDebugEnabled()) log.debug("-p="+pb);
			this.cumloss.add(LOSS.LOG, -Math.log(1.0-pb));
		}

//		log.info("gradient step magnitude "+Math.sqrt(mag)+" "+ex.ex.toString());
		if (nonzero==0) {
			this.zeroGradientData.numZero++;
			if (this.zeroGradientData.numZero < MAX_ZERO_LOGS) {
				this.zeroGradientData.examples.append("\n").append(ex);
			}
//				log.warn("0 gradient. Try a different squashing function? "+ex.toString());
//				zeroLogsThisEpoch++;
//				if (zeroLogsThisEpoch >= MAX_ZERO_LOGS) {
//					log.warn("(that's your last 0 gradient warning this epoch)");
//				}
		}
		return gradient;
	}
	
	public class ZeroGradientData {
		public int numZero=0;
		public StringBuilder examples=new StringBuilder();
		public void add(ZeroGradientData z) {
			if (numZero < MAX_ZERO_LOGS) {
				examples.append(z.examples);
			}
			numZero += z.numZero;
		}
	}
	
	public ZeroGradientData getZeroGradientData() {
		return this.zeroGradientData;
	}
	
	/** template: update gradient with regularization term */
	protected void regularization(ParamVector params, PosNegRWExample ex, TIntDoubleMap gradient) {}

	//////////////////////////// copypasta from SRW.java:

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
			reader.close();
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
		if (log.isDebugEnabled()) log.debug("d size:" + diagonalDegree.size());
		return diagonalDegree;
	}


	protected double learningRate() {
		return Math.pow(this.epoch,-2) * c.eta;
	}

	public double clip(double prob)
	{
		if(prob <= 0) return BOUND;
		return prob;
	}

	public boolean trainable(String feature) {
		return !(untrainedFeatures.contains(feature) || feature.startsWith(FIXED_WEIGHT_FUNCTOR));
	}

	/** Allow subclasses to filter feature list **/
	public Set<String> localFeatures(ParamVector<String,?> paramVec, LearningGraph graph) {
		return paramVec.keySet();
	}
	/** Allow subclasses to swap in an alternate parameter implementation **/
	public ParamVector<String,?> setupParams(ParamVector<String,?> params) { return params; }


	/** Allow subclasses to do pre-example calculations (e.g. lazy regularization) **/
	public void prepareForExample(ParamVector params, LearningGraph graph, ParamVector apply) {}
	
	/** Allow subclasses to do additional parameter processing at the end of an epoch **/
	public void cleanupParams(ParamVector<String,?> params, ParamVector apply) {}


	public Set<String> untrainedFeatures() { return this.untrainedFeatures; }
	public SquashingFunction getSquashingFunction() {
		return c.squashingFunction;
	}
	public void setEpoch(int e) {
		this.epoch = e;
		this.zeroGradientData = new ZeroGradientData();
	}
	public void clearLoss() {
		this.cumloss.clear();
	}
	public LossData cumulativeLoss() {
		return this.cumloss.copy();
	}
	public void setSquashingFunction(SquashingFunction f) {
		c.squashingFunction = f;
	}
	public SRWOptions getOptions() {
		return c;
	}
	public void setAlpha(double d) {
		c.apr.alpha = d;
	}
	public void setMu(double d) {
		c.mu = d;
	}
	public PosNegRWExample makeExample(String string, LearningGraph g,
			TIntDoubleMap queryVec, int[] posList, int[] negList) {
		return new PprExample(string, g, queryVec, posList, negList);
	}
	public SRW copy() {
		Class<? extends SRW> clazz = this.getClass();
		try {
			SRW copy = clazz.getConstructor(SRWOptions.class).newInstance(this.c);
			copy.untrainedFeatures = this.untrainedFeatures;
			return copy;
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		throw new UnsupportedOperationException("Programmer error in SRW subclass "+clazz.getName()
				+": Must provide the standard SRW constructor signature, or else override copy()");
	}
}
