package edu.cmu.ml.praprolog.prove;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.ml.praprolog.util.SymbolTable;

public class WamInterpreter {
	private MutableState state;
	private SymbolTable constantTable;
	private List<Feature> featureStack;
	private Map<Goal,Integer>
	public WamInterpreter(WamProgram program, WamPlugin[] plugins) {
		constantTable = new SymbolTable();
		featureStack = new ArrayList<Feature>();
	}
	public void addPlugins(WamPlugin ... plugins) {}
	public ImmutableState saveState() {return this.state.immutableVersion();}
	public void restoreState(ImmutableState state) {}
	public void executeWithoutBranching(int startAddress, boolean computeFeatures) {}
//	public void allocate(int n, String ... names) {}
//	public void callp(String predicate) {}
//	public void returnp() {}
//	public void pushconst(String a) {}
//	public void pushfreevar(int a) {}
//	public void pushboundvar(int a) {}
//	public void unifyconst(int a, int relativeHeapIndex) {}
//	public void initfreevar(int a, int relativeHeapIndex) {}
//	public void unifyboundvar(int a, int relativeHeapIndex) {}
//	public void fclear() {}
//	public void fpushstart(String functor, int arity) {}
//	public void fpushconst(int a) {}
//	public void fpushboundvar(int a) {}
//	public void freport() {}
//	public void ffindall(int address) {}
	private void doFeatureFindallDFS(State state, int depth) {}
	public List<Outlink> wamOutlinks(State state, boolean computeFeatures) { return null; }
	public Argument getConstantArg(int k, int i) { return null; }
	public Argument getArg(int k, int i) { return null; }
	public MutableState getState() {
		return this.state;
	}
	public SymbolTable getConstantTable() {
		return constantTable;
	}
	public List<Feature> getFeatureStack() {
		return featureStack;
	}
	public Feature getFeaturePeek() {
		return featureStack.get(featureStack.size()-1);
	}
	public void reportFeature(Goal g) {
		// TODO
	}
	
}
