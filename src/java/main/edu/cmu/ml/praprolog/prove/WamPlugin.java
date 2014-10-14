package edu.cmu.ml.praprolog.prove;

import java.util.List;

/**
 * Abstract extension to a WAM program.
 * @author "William Cohen <wcohen@cs.cmu.edu>"
 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
 *
 */
public abstract class WamPlugin {
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
	 */
	public abstract List<Outlink> outlinks(State state, WamInterpreter wamInterp, boolean computeFeatures);
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
	public int degree(String jumpto,State state, WamInterpreter wamInterp) {
		throw new UnsupportedOperationException("degree method not implemented");
	}
}
