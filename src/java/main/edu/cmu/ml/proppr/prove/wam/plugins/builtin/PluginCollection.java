package edu.cmu.ml.proppr.prove.wam.plugins.builtin;

import java.util.HashMap;
import java.util.Map;

import edu.cmu.ml.proppr.prove.wam.Goal;
import edu.cmu.ml.proppr.prove.wam.Query;
import edu.cmu.ml.proppr.prove.wam.WamInterpreter;
import edu.cmu.ml.proppr.prove.wam.plugins.WamPlugin;
import edu.cmu.ml.proppr.util.APROptions;
import edu.cmu.ml.proppr.util.Dictionary;
/**
"""Used for collections of simple built-in plugins."""

def __init__(self):
    self.registery = {}
    self.helpText = {}
    self.fd = {'builtin':1}
def register(self,jumpTo,fun,help='no help available'):
    self.registery[jumpTo] = fun
    self.helpText[jumpTo] = help
def claim(self,jumpTo):
    return (jumpTo in self.registery)
def outlinks(self,state,wamInterp,computeFeatures=True):
    assert False,'abstract method called'
    
    @author William Cohen <wcohen@cs.cmu.edu>
    @author Kathryn Mazaitis <krivard@cs.cmu.edu>
**/
public abstract class PluginCollection extends WamPlugin {
	protected Map<String,PluginFunction> registry;
	protected Map<Goal,Double> fd;
	// TODO: helpText
	
	public PluginCollection(APROptions apr) {
		super(apr);
		this.fd = new HashMap<Goal,Double>();
		this.fd.put(Query.parseGoal("builtin"), 1.0);
	}

	@Override
	public String about() {
		return Dictionary.buildString(registry.keySet(), new StringBuilder(), ", ").toString();
	}

	@Override
	public boolean claim(String jumpto) {
		return registry.containsKey(jumpto);
	}

	public void register(String jumpTo, PluginFunction fun) {
		if (registry == null) registry = new HashMap<String,PluginFunction>();
		registry.put(jumpTo, fun);
	}
	

}
