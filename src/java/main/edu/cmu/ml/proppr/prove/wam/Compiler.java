package edu.cmu.ml.proppr.prove.wam;

import java.util.TreeSet;

import edu.cmu.ml.proppr.prove.wam.Instruction.OP;
import edu.cmu.ml.proppr.util.Dictionary;

/**
 * Compile prolog rules into a wam program.
 * @author "William Cohen <wcohen@cs.cmu.edu>"
 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
 *
 */
public class Compiler {
	public static final String JUMPTO_DELIMITER = "/";

	public void compileRule(Rule rule, AWamProgram wamprog) {
		// allocate variables
		int nvars = rule.nvars;
		if (nvars > 0) {
			wamprog.append(new Instruction(OP.allocate,nvars));
		}
		int placeToPatch=-1;
		TreeSet<Argument> previousVars = new TreeSet<Argument>();
		// compile code to match the head
		if (rule.lhs != null) {
			int nargs = rule.lhs.getArity();
			for (int i=0; i<nargs; i++) {
				Argument a = rule.lhs.getArg(i);
				int relativeHeapIndex = -nargs + i;
				if (a.isConstant()) {
					wamprog.append(new Instruction(OP.unifyconst, a.getName(), relativeHeapIndex));
				} else if (previousVars.contains(a)) {
					wamprog.append(new Instruction(OP.unifyboundvar, a.getValue(), relativeHeapIndex));
				} else {
					wamprog.append(new Instruction(OP.initfreevar, a.getValue(), relativeHeapIndex));
					previousVars.add(a);
				}
			}
			//now code to produce the features
			if (rule.features.length>0){
				// if no features specified, create a unique 'id' feature for the rule
				int ruleStartAddr = wamprog.size();
				rule.features = new Goal[] { 
					new Goal(
							"id",
							new ConstantArgument(rule.lhs.getFunctor()),
							new ConstantArgument(String.valueOf(rule.lhs.getArity())),
							new ConstantArgument(String.valueOf(ruleStartAddr))
							) 
					};
			}
			// clear the feature stack
			wamprog.append(new Instruction(OP.fclear));
			if (rule.findall.length == 0) {
				this.compileFeatures(rule,wamprog,previousVars);
			} else {
				// complex case - a 'findall' generator for the features. 
				// the code for the feature generation will be inserted 
				// after the returnp, and inserted here will be an opcode
				// instructing the interpreter to execute that code via
				// depth-first-search.
				placeToPatch = wamprog.size();
				wamprog.append(new Instruction(OP.ffindall,-1));
			}
			// return the features
			wamprog.append(new Instruction(OP.freport));
		}
		//compile the body
		for (Goal g : rule.body) {
			this.compileGoal(g,wamprog,previousVars);
		}
		wamprog.append(new Instruction(OP.returnp));
		// in the complex case, produce the code for feature generation
		// after the main body of the clause
		if (rule.findall.length>0) {
			StringBuilder sb = new StringBuilder("features ");
			Dictionary.buildString(rule.features,sb,",");
			sb.append(" : ");
			Dictionary.buildString(rule.findall,sb,",");
			wamprog.append(new Instruction(OP.comment,sb.toString()));
			// patch the ''ffindall' opcode to point to the right place
			wamprog.setInstruction(placeToPatch, new Instruction(OP.ffindall,wamprog.size()));
			// compile the generator code
			for (Goal g : rule.findall) {
				this.compileGoal(g,wamprog,previousVars);
			}
			// compile the feature generation code
			this.compileFeatures(rule,wamprog,previousVars);
			wamprog.append(new Instruction(OP.returnp));
		}
	}

	private void compileGoal(Goal g, AWamProgram wamprog,
			TreeSet<Argument> previousVars) {
		for (int i=0; i<g.getArity(); i++) {
			Argument a = g.getArg(i);
			if (a.isConstant()) 
				wamprog.append(new Instruction(OP.pushconst,a.getName()));
			else if (previousVars.contains(a))
				wamprog.append(new Instruction(OP.pushboundvar,a.getValue()));
			else {
				wamprog.append(new Instruction(OP.pushfreevar,a.getValue()));
				previousVars.add(a);
			}
		}
		wamprog.append(new Instruction(OP.callp,new StringBuilder(g.getFunctor()).append(JUMPTO_DELIMITER).append(g.getArity()).toString()));
	}

	private void compileFeatures(Rule rule, AWamProgram wamprog,
			TreeSet<Argument> previousVars) {
		for (Goal g : rule.features) {
			wamprog.append(new Instruction(OP.fpushstart,g.getFunctor(),g.getArity()));
			for (Argument a : g.getArgs()) {
				if (a.isConstant()) 
					wamprog.append(new Instruction(OP.fpushconst,a.getName()));
				else if (previousVars.contains(a)) 
					wamprog.append(new Instruction(OP.fpushboundvar,a.getValue()));
				else
					throw new WAMSyntaxException("Unbound variable in feature in rule :"+rule.toString());
			}
		}
	}
	
	public static class WAMSyntaxException extends RuntimeException {
		public WAMSyntaxException(String s) { super(s); }
	}
}
