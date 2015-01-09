package edu.cmu.ml.proppr.prove.wam.plugins.builtin;

import java.util.Collections;
import java.util.List;


import edu.cmu.ml.proppr.prove.wam.LogicProgramException;
import edu.cmu.ml.proppr.prove.wam.Outlink;
import edu.cmu.ml.proppr.prove.wam.State;
import edu.cmu.ml.proppr.prove.wam.WamInterpreter;
import edu.cmu.ml.proppr.util.APROptions;

/**
 * 
    """Used for built-ins that may or may not succeed, like 'neq'."""
    def __init__(self):
        PlugInCollection.__init__(self)
    def implementsDegree(self,state):
        return False
    def outlinks(self,state,wamInterp,computeFeatures=True):
        jumpTo = state.jumpTo
        filterFun = self.registery[jumpTo]
        if not filterFun(wamInterp):
            wamInterp.restoreState(state)
            wamInterp.returnp()
            wamInterp.state.failed = True
        else:
            wamInterp.restoreState(state)
            wamInterp.returnp()
            wamInterp.executeWithoutBranching()
        if computeFeatures:
            yield (self.fd,wamInterp.saveState())
        else:
            yield wamInterp.saveState()

 * @author "William Cohen <wcohen@cs.cmu.edu>"
 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
 *
 */
public class FilterPluginCollection extends PluginCollection {

	public FilterPluginCollection(APROptions apr) {
		super(apr);
	}

	@Override
	public List<Outlink> outlinks(State state, WamInterpreter wamInterp,
			boolean computeFeatures) throws LogicProgramException {
		String jumpTo = state.getJumpTo();
		PluginFunction fun = this.registry.get(jumpTo);
		wamInterp.restoreState(state);
		wamInterp.returnp();
		if (!fun.run(wamInterp)) {
			wamInterp.getState().setFailed(true);
		} else {
			wamInterp.executeWithoutBranching();
		}
		if (computeFeatures) {
			return Collections.singletonList(new Outlink(this.fd, wamInterp.saveState()));
		}
		return Collections.singletonList(new Outlink(Outlink.EMPTY_FD, wamInterp.saveState()));
	}

}
