package edu.cmu.ml.proppr.prove.wam.plugins;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import edu.cmu.ml.proppr.prove.wam.LogicProgramException;
import edu.cmu.ml.proppr.prove.wam.Outlink;
import edu.cmu.ml.proppr.prove.wam.State;
import edu.cmu.ml.proppr.prove.wam.WamInterpreter;
import edu.cmu.ml.proppr.util.APROptions;

/** 
 * Manage multiple .cfacts files, supporting splitting a functor across multiple files
 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
 *
 */
public class SplitFactsPlugin extends WamPlugin {
	protected List<FactsPlugin> plugins = new LinkedList<FactsPlugin>();
	public SplitFactsPlugin(APROptions apr) {
		super(apr);
	}

	@Override
	public String about() {
		StringBuilder sb = new StringBuilder("union(");
		for (FactsPlugin p : plugins) sb.append(p.about()).append(",");
		sb.setCharAt(sb.length()-1, ')');
		return sb.toString();
	}
	
	public void add(FactsPlugin p) {
		this.plugins.add(p);
	}

	@Override
	public boolean _claim(String jumpto) {
		for (FactsPlugin p : plugins)
			if (p._claim(jumpto)) return true;
		return false;
	}

	@Override
	public List<Outlink> outlinks(State state, WamInterpreter wamInterp,
			boolean computeFeatures) throws LogicProgramException {
		List<Outlink> ret = new ArrayList<Outlink>();
		for (FactsPlugin p : plugins) {
			if (p._claim(state.getJumpTo())) {
				wamInterp.restoreState(state);
				List<Outlink> outs = p.outlinks(state, wamInterp, computeFeatures);
				ret.addAll(outs);
			}
		}
		return ret;
	}
}
