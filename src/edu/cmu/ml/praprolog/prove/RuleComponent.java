package edu.cmu.ml.praprolog.prove;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.cmu.ml.praprolog.util.Dictionary;
import edu.cmu.ml.praprolog.util.ParsedFile;
import edu.cmu.ml.praprolog.util.SymbolTable;


public class RuleComponent extends Component {
	private static final Logger log = Logger.getLogger(RuleComponent.class);
	public static final String FILE_EXTENSION=".crules";
	protected Map<String,List<Rule>> index = new HashMap<String,List<Rule>>();
	public void add(Rule rule) {
		String key = key(rule.lhs);
		if (log.isDebugEnabled()) log.debug("Adding ["+rule+"] to ["+key+"]");
		Dictionary.safeAppend(index, key, rule);
	}
	@Override
	public boolean claim(LogicProgramState state) {
		// FIXME -- only works b/c we know isSolution is true iff #goals == 0
		return (!state.isSolution() && this.index.containsKey(key(state.getHeadFunctor(), state.getHeadArity())));
	}
	protected List<Rule> rulesFor(Goal goal) {
		return this.index.get(this.key(goal));
	}
	protected String key(Goal goal) {
		return goal.getFunctor()+goal.getArity();//String.format("%s/%d",goal.getFunctor(),goal.getArity());
	}
	protected String key(String functor, int arity) {
		return functor+arity;//String.format("%s/%d",goal.getFunctor(),goal.getArity());
	}

	@Override
	public int degree(LogicProgramState state) {
		if (state.isSolution()) return 0;
		return getSubstitutions(state).size();
	}

	protected List<RuleSubstitutionPair> getSubstitutions(LogicProgramState state0) {
		ProPPRLogicProgramState state = (ProPPRLogicProgramState) state0.asProPPR();
		LinkedList<RuleSubstitutionPair> matches = new LinkedList<RuleSubstitutionPair>();
		int offsetToStandardizeApart = state.getVarSketchSize();
		for (Rule r : this.rulesFor(state.getHeadGoal())) {
			// print 'trying rule',r,'on',state.goals[0],"with offset",offsetToStandardizeApart
			if (log.isDebugEnabled()) log.debug("trying rule "+r+" on "+state.getHeadGoal()+" with offset "+offsetToStandardizeApart);
			RenamingSubstitution theta = 
					RenamingSubstitution.unify(r.lhs, state.getHeadGoal(), 
							offsetToStandardizeApart, 
							RenamingSubstitution.RENAMED, RenamingSubstitution.NOT_RENAMED);
			if (theta != null) {
				if (log.isDebugEnabled()) log.debug("succeeds "+theta);
				matches.add(new RuleSubstitutionPair(theta, r));
			}
		}
		return matches;

	}

	@Override
	public List<Outlink> outlinks(LogicProgramState state0) throws LogicProgramException {
		if (! (state0 instanceof ProPPRLogicProgramState)) 
			throw new UnsupportedOperationException("Can't handle tuprolog states yet in rulecomponent");
		ProPPRLogicProgramState state = (ProPPRLogicProgramState) state0;
		List<RuleSubstitutionPair> matches = getSubstitutions(state);
		List<Outlink> result = new ArrayList<Outlink>();
		for (RuleSubstitutionPair rp : matches) {
			result.add(new Outlink(rp.r.featuresAsDict(rp.theta, RenamingSubstitution.RENAMED), state.child(rp.r.rhs,rp.theta)));
		}
		return result;
	}

	public class RuleSubstitutionPair {
		RenamingSubstitution theta;
		Rule r;
		public RuleSubstitutionPair(RenamingSubstitution t, Rule rl) {
			this.theta = t; this.r = rl;
		}
	}

	@Override
	public void compile() {
		this.compile(new SymbolTable());
	}
	@Override
	public void compile(SymbolTable variableSymTab) {
		for(List<Rule> el : this.index.values()) {
			for (Rule r : el) r.compile(variableSymTab);
		}
	}
	public String toString() {
		StringBuilder sb = new StringBuilder("ruleComponent:");
		for(List<Rule> el : this.index.values()) {
			for (Rule r : el) sb.append("\n\t").append(r);
		}
		return sb.toString();
	}

	/**
	 * Load a rulebase in the format produced by the rulecompiler.
	 * @param fileName
	 * @return
	 */
	public static RuleComponent loadCompiled(String filename) {
		RuleComponent result = new RuleComponent();
		ParsedFile file = new ParsedFile(filename);
		for (String line : file) {
			String[] parts = line.split("#");
			if (parts.length != 3) {
				file.parseError("3 #-delimited fields required; found "+parts.length);
			}
			String[] variableList = parts[2].trim().split(",");

			String[] ruleGoalStrings =  parts[0].trim().split("&");
			Goal lhs;
			Goal[] rhs = new Goal[ruleGoalStrings.length-1];
			lhs = Goal.decompile(ruleGoalStrings[0]);
			for (int i=1; i<ruleGoalStrings.length; i++) {
				rhs[i-1] = Goal.decompile(ruleGoalStrings[i]);
			}

			String[] featureGoalStrings = parts[1].trim().split("&");
			Goal[] featureGoals = new Goal[featureGoalStrings.length];
			for (int i=0; i<featureGoalStrings.length; i++) {
				featureGoals[i] = Goal.decompile(featureGoalStrings[i]);
			}

			result.add(new Rule(lhs,rhs,featureGoals)); // FIXME fishy! python has 
			// r = rule(ruleGoals[0],ruleGoals[1:],'',featureGoals)
			// where constructor is
			// rule(lhs,rhs,features=tuple(),variableList=string.ascii_uppercase)

		}
		return result;
	}

	@Override
	public String listing() {
		StringBuilder sb= new StringBuilder();
		boolean first = true;
		for (Map.Entry<String,List<Rule>> key : this.index.entrySet()) {
			if(first)first=false;
			else sb.append("\n");
			sb.append("% rules for ").append(key.getKey());
			for (Rule r : key.getValue()) {
				sb.append("\n").append(r);
			}
		}
		return sb.toString();
	}

	// a little test case...
	public static void main(String[] args)  {
		RuleComponent rc = loadCompiled(args[0]);
		System.out.println("listing of " + args[0] + ":\n" + rc.listing());
	}
}