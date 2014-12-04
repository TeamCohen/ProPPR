package edu.cmu.ml.proppr.prove.wam.plugins;

import java.util.List;

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
	public static Goal pluginFeature(WamPlugin plugin, String identifier) {
		return new Goal("db",new ConstantArgument(plugin.getClass().getSimpleName()),new ConstantArgument(identifier));
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
	public abstract boolean claim(String jumpto);
	/** The feature dictionary for the restart state.
	 * 
	 * @param state
	 * @param wamInterp
	 */
	public abstract void restartFD(State state, WamInterpreter wamInterp);
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
}
