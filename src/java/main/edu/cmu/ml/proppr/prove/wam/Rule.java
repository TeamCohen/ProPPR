package edu.cmu.ml.proppr.prove.wam;

import java.util.List;

import edu.cmu.ml.proppr.util.Dictionary;
import edu.cmu.ml.proppr.util.SymbolTable;

/**
 * A prolog rule.  The lhs is a goal, the rhs a list of goals, so the
    rule's format is "lhs :- rhs."  The features for a rule are, in
    general, of the form "features : findall", where 'findall' and
    'features' are lists of goals.  Features are produced as follows:
    after binding the head of the rule, you find all solutions to the
    'findall' part (the "generator"), and for each solution, create a
    feature corresponding to a bound version of each goal g in
    'features'.
 * @author "William Cohen <wcohen@cs.cmu.edu>"
 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
 *
 */
public class Rule {
	protected Goal lhs;
	protected Goal[] body;
	protected Goal[] features;
	protected Goal[] findall;
	protected int nvars;
//	protected List<Argument> variableList;
	public Rule(Goal lhs, Goal[] rhs, Goal[] features, Goal[] findall) {
		this.lhs = lhs;
		this.body = rhs;
		this.features = features;
		this.findall = findall;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (this.lhs != null) sb.append(this.lhs).append(" :- ");
		Dictionary.buildString(this.body, sb, ",");
		if (this.features.length>0) {
			sb.append(" {");
			Dictionary.buildString(features,sb,",");
			if (this.findall.length>0) {
				sb.append(" : ");
				Dictionary.buildString(findall,sb,",");
			}
			sb.append("}");
		}
		if (this.nvars>0) {
			sb.append("  #v:[");
//			Dictionary.buildString(this.variableList, sb, ",");
			sb.append("?");
			sb.append("]");
		}
		sb.append(".");
		return sb.toString();
	}

	public SymbolTable<Argument> variabilize() {
		return variabilize(new SymbolTable<Argument>());
	}
	/**
	 * Convert the variables to integer indices, -1,-2, ... and save their
	 * original names in "variableList", and the number of distinct
	 * variables in 'nvars'.
	 */
	public SymbolTable<Argument> variabilize(SymbolTable<Argument> varTab) {
		if (this.nvars > 0) throw new IllegalStateException("Rule already variabilized! "+this.toString());
		int before = varTab.getSymbolList().size();
		if (this.lhs != null) {
			this.lhs = convertGoal(this.lhs, varTab);
		}
		convertGoals(this.body, varTab);
		convertGoals(this.features, varTab);
		convertGoals(this.findall, varTab);
//		this.variableList = varTab.getSymbolList();
//		this.nvars = this.variableList.size();
		this.nvars = varTab.getSymbolList().size() - before;
		return varTab;
	}

	public static void convertGoals(Goal[] goals, SymbolTable<Argument> varTab) {
		for (int i=0; i<goals.length; i++) {
			goals[i] = convertGoal(goals[i], varTab);
		}
	}
	public static Goal convertGoal(Goal g, SymbolTable<Argument> varTab) {
		return new Goal(g.getFunctor(), convertArgs(g.getArgs(), varTab));
	}
	public static Argument[] convertArgs(Argument[] args, SymbolTable<Argument> varTab) {
		Argument[] ret = new Argument[args.length];
		for (int i=0; i<args.length; i++) {
			if (args[i].isVariableAtom()) ret[i] = new VariableArgument(-varTab.getId(args[i]));
			else ret[i] = args[i];
		}
		return ret;
	} 
	public Goal getLhs() {
		return lhs;
	}


	public Goal[] getRhs() {
		return body;
	}

	public Goal[] getFeatures() {
		return features;
	}

	public Goal[] getFindall() {
		return findall;
	}

	public int getNvars() {
		return nvars;
	}
}
