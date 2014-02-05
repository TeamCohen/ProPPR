package edu.cmu.ml.praprolog.prove;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.cmu.ml.praprolog.util.Dictionary;
import edu.cmu.ml.praprolog.util.SymbolTable;

public abstract class Component {
	private static final Logger log = Logger.getLogger(Component.class);
	public static final double ALPHA_DEFAULT = 0.0;//DprProver.MINALPH_DEFAULT;//0.0;
    protected Goal restartFeature = new Goal("id","defaultRestart");
//    protected Map<Goal,Double> restartFD = new HashMap<Goal,Double>();
	protected double alpha=ALPHA_DEFAULT;//0.2;
	protected Goal boostedRestartFeature=new Goal("id","alphaBooster");
    
    public Component() {
//    	this.restartFD.put(restartFeature,1.0);
    }
	/**
	 * Alpha is used to limit the minimum restart weight, when you
        use a uniformWeighter (or something related, like a fdWeighter
        with learned weights that are close to 1.0).
        
        With unit feature weights, a graph node of degree n will lead
        to an lpState with degree n+1, and have a restart weight that
        is 1/(n+1).  With alpha set, a new feature (named
        'alphaBooster') is introduced with a non-unit VALUE of n *
        (alpha/(1-alpha)) for the restart weight, which means that
        unit weights will give that edge a total weight of alpha.
	 */
    public void setAlpha(double a) { this.alpha = a; }
	
	/**
	 * Declare that you have interest in this state - Must be overridden.
	 * @param state
	 * @return
	 */
	public abstract boolean claim(LogicProgramState state); 

	/**
	 * Yield a sequence of tuple (edgeFeatureDict,t) where t is a
        child of the state, and edgeFeatureDict is a feature vector describing the
        state->t transition
	 * @param state
	 * @return
	 * @throws LogicProgramException 
	 */
	public abstract List<Outlink> outlinks(LogicProgramState state) throws LogicProgramException;
	public static class Outlink {
			Map<Goal,Double> featureDict;
			LogicProgramState state;
			public Outlink(Map<Goal,Double> fd, LogicProgramState s) {
				this.featureDict = fd;
				this.state = s;
			}
			public Map<Goal,Double> getFeatureDict() {
				return this.featureDict;
			}
			public LogicProgramState getState() {
				return this.state;
			}
	}
	/**
	 * Yield a pair (edgeFeatures,alpha) associated with the restart
        link for this state.  Weight alpha is a little different from
        other weights: the weight of the restart link will be alpha, 
        and the weight of all other links nominally weighted w will
        be (1-alpha)*w
	 * @param state
	 * @return
	 * @throws LogicProgramException 
	 */
	public Map<Goal, Double> restartFeatureDict(LogicProgramState state) throws LogicProgramException {
		int n = degree(state);
		Map<Goal,Double> featureDict = new HashMap<Goal,Double>();
//		featureDict.putAll(this.restartFD);
		featureDict.put(restartFeature,1.0);
		double wt = 1.0;
		if (this.alpha > 0 && n > 0) {
			wt = n * this.alpha / (1.0-this.alpha);
			if (log.isDebugEnabled()) log.debug("n="+n+"; a="+this.alpha+"; w="+wt);
		}
		featureDict.put(this.boostedRestartFeature, wt);
		return featureDict;
	}
	
	public abstract void compile();
	public abstract void compile(SymbolTable variableSymTab);

	public int degree(LogicProgramState state) throws LogicProgramException {
        return this.outlinks(state).size();
    }

	public static Component[] loadComponents(String[] programFiles,double alpha) {
		HashMap<String,List<String>> args = new HashMap<String,List<String>>();

		// collect all files of each component type together
		for (String fileName : programFiles) {
			if (fileName.length() == 0) continue;
			if (fileName.endsWith(GoalComponent.FILE_EXTENSION)) {
				Dictionary.safeAppend(args, GoalComponent.FILE_EXTENSION, fileName);
			} else if (fileName.endsWith(RuleComponent.FILE_EXTENSION)) {
				Dictionary.safeAppend(args, RuleComponent.FILE_EXTENSION, fileName);
			} else if (fileName.endsWith(GraphComponent.FILE_EXTENSION)) {
				Dictionary.safeAppend(args, GraphComponent.FILE_EXTENSION, fileName);
			} else if (fileName.endsWith(SparseGraphComponent.FILE_EXTENSION)) {
				Dictionary.safeAppend(args, SparseGraphComponent.FILE_EXTENSION, fileName);
			} else if (fileName.endsWith(TuprologComponent.UNCOMPILED_EXTENSION)) { 
				Dictionary.safeAppend(args, TuprologComponent.UNCOMPILED_EXTENSION, fileName);
			} else {
				throw new IllegalArgumentException("Unknown file type for "+fileName);
			}
		}

		int nComponents = args.size();
		if (args.containsKey(SparseGraphComponent.FILE_EXTENSION)) {
			nComponents += args.get(SparseGraphComponent.FILE_EXTENSION).size() - 1;
		}
		Component[] result = new Component[nComponents];
		
		// instantiate each type
		int i=0;
		for (Map.Entry<String,List<String>> arg : args.entrySet()) {
			if (arg.getKey().equals(SparseGraphComponent.FILE_EXTENSION)) {
				for (String fileName : arg.getValue()) {
					log.info("Loading from file '"+fileName+"' with alpha="+alpha+" ...");
					result[i++] = SparseGraphComponent.load(fileName);
					result[i-1].setAlpha(alpha);
				}
			} else {
			
				if (arg.getKey().equals(GoalComponent.FILE_EXTENSION)) {
					result[i++] = GoalComponent.loadCompiled(arg.getValue());
				} else if (arg.getKey().equals(RuleComponent.FILE_EXTENSION)) {
					result[i++] = RuleComponent.loadCompiled(arg.getValue());
				} else if (arg.getKey().equals(GraphComponent.FILE_EXTENSION)) {
					result[i++] = GraphComponent.load(arg.getValue());
				} else  if (arg.getKey().equals(TuprologComponent.UNCOMPILED_EXTENSION)) { 
					// catches both compiled and uncompiled files,
					// which are separated inside the load method
					result[i++] = TuprologComponent.load(arg.getValue());
				} else {
					throw new IllegalArgumentException("Bad programmer: added a file extension entry to Component without adding handling code");
				}
				result[i-1].setAlpha(alpha);
			}
		}
		return result;
	}

	public String listing() {
		return "component <no string available>";
	}
	public static String cleanLabel(String label) {
		if (!label.matches("[A-Za-z0-9_]*")) return "'"+label+"'";
		return label;
	}
}
