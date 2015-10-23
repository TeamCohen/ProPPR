package edu.cmu.ml.proppr.learn.tools;

/** A simple programmable procedure to determine if a feature is fixed
		(i.e., the weight will not be changed in training) or not.  The
		procedure is specified by a single string, which is a
		comma-separated list of pairs PREFIX~DECISION where PREFIX is any
		string and DECISION is 'y' or 'n'.  To make a decision of a
		feature with name S, you go through the rules, and take the
		DECISION associated with the first prefix of S that matches.
		
		
		For example: 

		"fixedWeight~y,~n" means tune all features that don't start with
		"fixedWeight".

		"f(~n,~y" means tune only features that start with "f("
		
		If no prefix matches then the decision return is false.
**/

public class FixedWeightFilter {
	private DecisionRule[] ruleList;
	class DecisionRule {
		public String prefixToMatch;
		public boolean fixThisWeight;
		public DecisionRule(String pref,boolean fix) {
			prefixToMatch = pref;
			fixThisWeight = fix;
		}
		@Override
		public String toString() { return "if f.startsWith('"+prefixToMatch+"') then "+Boolean.toString(fixThisWeight); }
	}

	public FixedWeightFilter() {
		this("fixedWeight~y");
	}

	public FixedWeightFilter(String specification) {
		String[] parts = specification.split(",");
		ruleList = new DecisionRule[parts.length];
		for (int i=0; i<parts.length; i++) {
			String[] subparts = parts[i].split("~");
			ruleList[i] = new DecisionRule(subparts[0], subparts[1].startsWith("y"));
		}
	}

	public boolean isFixedWeight(String featureName) {
		for (DecisionRule rule : ruleList) {
			if (featureName.startsWith(rule.prefixToMatch)) {
				return rule.fixThisWeight;
			}
		}
		return false;
	}

	@Override
	public String toString() { 
		StringBuffer buf = new StringBuffer("");
		for (DecisionRule rule : ruleList) {
			buf.append(rule.toString());
			buf.append(" else ");
		}
		buf.append(Boolean.toString(false));
		return buf.toString();
	}

	/** Simple test main - usage: SPECIFICATION featureName1 featureName2 ...  **/
	public static void main(String[] argv) {
		FixedWeightFilter fwt = new FixedWeightFilter(argv[0]);
		System.out.println(argv[0] + " == " + fwt.toString());
		for (int i=1; i<argv.length; i++) {
			System.out.println(" fix "+argv[i]+": "+fwt.isFixedWeight(argv[i]));
		}
	}
}
