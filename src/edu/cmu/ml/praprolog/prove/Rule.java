package edu.cmu.ml.praprolog.prove;

import java.util.HashMap;
import java.util.Map;

import edu.cmu.ml.praprolog.util.Dictionary;
import edu.cmu.ml.praprolog.util.SymbolTable;


public class Rule {
	protected Goal lhs;
	protected Goal[] features;
	protected Goal[] rhs;
	public Rule(Goal lhs, Goal feature, Goal ... rhs) {
		this.lhs = lhs;
		this.features = new Goal[1]; this.features[0] = feature;
		this.rhs = rhs; // FIXME defensive copy? 
	}
	public Rule(Goal lhs, Goal[] rhs, Goal[] features) {
		this.lhs = lhs;
		this.rhs = rhs; // FIXME defensive copy?
		this.features = features; // FIXME defensive copy?
		
	}
	
	public Map<Goal, Double> featuresAsDict(RenamingSubstitution theta) throws LogicProgramException {
		return featuresAsDict(theta, RenamingSubstitution.NOT_RENAMED);
	}
	/**
	 * Convert the features F1 ... Fk to a dictionary with keys
        being string versious of theta(F1),....,theta(Fk), and values 1.0.
	 * @param theta
	 * @return
	 * @throws LogicProgramException 
	 */
	public Map<Goal, Double> featuresAsDict(RenamingSubstitution theta, int renamedP) throws LogicProgramException {
		Map<Goal,Double> result = new HashMap<Goal,Double>();
		for (Goal g0 : this.features) {
			Goal g = theta.applyToGoal(g0, renamedP);
			for (Argument a : g.getArgs()) {
				if (!a.isConstant()) {
					throw new LogicProgramException("Error converting features of rule "+this.toString()+" with theta "+theta.toString()
							+": offending feature "+g0+" became "+g);
				}
			}
			result.put(g,1.0);
		}
		return result;
	}

	public String toString() {
		if (this.rhs.length > 0)
			return this.lhs + " :- " + Dictionary.buildString(this.rhs, new StringBuilder(), ",").toString();
		return this.lhs.toString()+" :- ";
	}
	public void compile(SymbolTable variableSymTab) {
		lhs.compile(variableSymTab);
		for (Goal g : this.rhs) g.compile(variableSymTab);
	}
}
