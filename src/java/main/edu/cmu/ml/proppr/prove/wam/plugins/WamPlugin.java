package edu.cmu.ml.proppr.prove.wam.plugins;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cmu.ml.proppr.prove.wam.ConstantArgument;
import edu.cmu.ml.proppr.prove.wam.Goal;
import edu.cmu.ml.proppr.prove.wam.LogicProgramException;
import edu.cmu.ml.proppr.prove.wam.Outlink;
import edu.cmu.ml.proppr.prove.wam.State;
import edu.cmu.ml.proppr.prove.wam.WamInterpreter;
import edu.cmu.ml.proppr.util.APROptions;

/**
 * Abstract extension to a WAM program.
 * 
 * When making new plugins, be sure to use WamPlugin.pluginFeature() to generate the feature for your plugin, so that we can monitor the minalpha projection assumption correctly.
 * @author "William Cohen <wcohen@cs.cmu.edu>"
 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
 *
 */
public abstract class WamPlugin {
	public static final String FACTS_FUNCTOR = "db(";
	protected static final double DEFAULT_DSTWEIGHT = 1.0;
	public static final String WEIGHTED_SUFFIX = "#";
	public static Goal pluginFeature(WamPlugin plugin, String identifier) {
		return new Goal("db",new ConstantArgument(plugin.getClass().getSimpleName()),new ConstantArgument(identifier));
	}

	/** Convert from a string like "foo#/3" to "foo/2" **/
	public static String unweightedJumpto(String jumpto) {
		int n = jumpto.length();
		String[] parts = jumpto.split(WamInterpreter.WEIGHTED_JUMPTO_DELIMITER,2);
		// String stem = jumpto.substring(0,n-WEIGHTED_GRAPH_SUFFIX_PLUS_ARITY.length());
		// return stem + GRAPH_ARITY;
		return parts[0] + WamInterpreter.JUMPTO_DELIMITER + (Integer.parseInt(parts[1])-1);
	}

	protected APROptions apr;
	public WamPlugin(APROptions apr) {
		this.apr = apr;
	}
	
	public abstract String about();
	
	/** Return True if this plugin should be called to implement this predicate/arity pair.
	 * 
	 * @param jumpto
	 * @return
	 */
	public abstract boolean _claim(String jumpto);
	
	public boolean claim(String rawJumpto) {
		if (rawJumpto.indexOf(WamInterpreter.WEIGHTED_JUMPTO_DELIMITER) < 0)
			return _claim(rawJumpto);
		return _claim(unweightedJumpto(rawJumpto));
	}
	
	/** Yield a list of successor states, not including the restart state.
	 * 
	 * @param state
	 * @param wamInterp
	 * @param computeFeatures
	 * @return
	 * @throws LogicProgramException 
	 */
	public abstract List<Outlink> outlinks(State state, WamInterpreter wamInterp, boolean computeFeatures) throws LogicProgramException;
	/** True if the subclass implements a degree() function that's quicker than computing the outlinks.
	 * 
	 * @return
	 */
	public boolean implementsDegree() {
		return false;
	}
	/** Return the number of outlinks, or else throw an error if implementsDegree is false.
	 * 
	 * @param jumpto
	 * @param state
	 * @param wamInterp
	 * @return
	 */
	public int degree(String jumpto, State state, WamInterpreter wamInterp) {
		throw new UnsupportedOperationException("degree method not implemented");
	}
	
	public String toString() {
		return this.about();
	}

	protected Map<Goal,Double> scaleFD(Map<Goal,Double> fd, double wt) {
		if (wt == 1.0) return fd;
		Map<Goal,Double> ret = new HashMap<Goal,Double>();
		ret.putAll(fd);
		for (Map.Entry<Goal, Double> val : ret.entrySet()) {
			val.setValue(val.getValue() * wt);
		}
		return ret;
	}
}
