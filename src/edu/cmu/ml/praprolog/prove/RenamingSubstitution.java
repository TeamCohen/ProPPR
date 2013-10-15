package edu.cmu.ml.praprolog.prove;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.cmu.ml.praprolog.util.Dictionary;

/**
 * A logical substitution.  For efficiency, the substitution also
    includes a variable-renaming scheme in the form of an
    'offset'. The offset is an efficient way of doing the variable
    renaming necessary to 'standardize apart' a rule: when applying
    the substitution, every variable (with index -i) in the will be
    replaced by a variable with index -i-offset.
 * @author krivard
 *
 */
public class RenamingSubstitution {
	private static final Logger log = Logger.getLogger(RenamingSubstitution.class);
	public static final int NOT_RENAMED = 0;
	public static final int RENAMED = 1;
	
	protected int offset=0;
	protected Map<Argument,Argument> dict = new HashMap<Argument,Argument>();
	public RenamingSubstitution(int offset) {
		this.offset = offset;
	}
	/**
	 * Return mgu (most general unifier) of two goals.  The offset
    only applies to the arguments that should be been 'renamed' (i.e.,
    variables renamed so that they are standardized apart.
	 * @param goal1
	 * @param goal2
	 * @param offset
	 * @param renamedP1
	 * @param renamedP2
	 * @return
	 */
	public static RenamingSubstitution unify(Goal goal1, Goal goal2, int offset,
			int renamedP1, int renamedP2) {
		if (goal1.getArity() != goal2.getArity()) {
			log.info("*** arity mismatch");
			return null;
		} else if (!goal1.getFunctor().equals(goal2.getFunctor())) {
			log.info("*** functor mismatch");
			return null;
		} else {
			RenamingSubstitution theta = new RenamingSubstitution(offset);
			for (int i=0; i<goal1.getArity(); i++) {
				Argument a = theta.valueOf(theta.applyToAtom(goal1.getArg(i), renamedP1));
				Argument b = theta.valueOf(theta.applyToAtom(goal2.getArg(i), renamedP2));
				if (a.isVariable()) theta.put(a,b);
				else if (b.isVariable()) theta.put(b, a);
				else if (!a.equals(b)) {
					log.info("Fails "+a+"!="+b); return null;
				}
			}
			return theta;
		}
		/*
		 * def unify(goal1,goal2,offset,renamedOrNot1,renamedOrNot2):
    """"""
    #print 'unify','goal',str(goal1),'otherGoal',str(goal2),'offset',offset
    if goal1.arity != goal2.arity:
        print "** arity mismatch"
        return None
    elif goal1.functor != goal2.functor:
        print "** functor mismatch"
        return None
    else:
        theta = renamingSubstitution(offset=offset)
        #print 'init theta',theta
        for i in xrange(goal1.arity):
            a = theta.valueOf(theta.applyToAtom(goal1.args[i],renamedOrNot1))
            b = theta.valueOf(theta.applyToAtom(goal2.args[i],renamedOrNot2))
            #print 'i',i,'a',a,'b',b,'offset',offset,'goal',str(goal),'goal2',str(goal2)
            if isVariable(a):
                theta.d[a]=b
                #print '1 mapping',a,'to',b
            elif isVariable(b):
                theta.d[b]=a
                #print '2 mapping',b,'to',a
            elif a!=b:
                #print 'fails',a,'!=',b
                return None
        #print 'returning',str(theta)
        return theta
		 */
	}
	/** Not normally for public use
	 * 
	 * @param a
	 * @param b
	 */
	public void put(Argument a, Argument b) {
		this.dict.put(a, b);
	}
	protected Argument applyToAtom(Argument arg, int renamedP1) {
		if (arg.isConstant()) return arg;
		else if (renamedP1==RENAMED) return this.valueOf(arg.getRenamed(this.offset));
		else return this.valueOf(arg);
	}
	/**
	 * Return theta(var).
	 * @param i
	 * @return
	 */
	protected Argument valueOf(Argument i) {
		while (this.dict.containsKey(i)) {
			Argument j = this.dict.get(i);
			if (j.equals(i)) throw new IllegalStateException("Theta contains loop "+i+"->"+j);
			i = j;
		}
		return i;
	}
	/**
	 * make a copy of this substitution, overwriting with any extra offsets/keys provided
	 * @param overwrite
	 * @return
	 */
	public RenamingSubstitution copy(RenamingSubstitution overwrite) {
		RenamingSubstitution result = new RenamingSubstitution(overwrite.offset);
		result.dict.putAll(this.dict);
		if (overwrite != this) result.dict.putAll(overwrite.dict);
		return result;
	}

	public RenamingSubstitution copy() {
		return copy(this);
	}
	/**
	 * Return the result of applying this substitution to the goal list G1,.,,,,Gk
	 * @param goals
	 * @param renamedP
	 * @return
	 */
	public Goal[] applyToGoalList(List<Goal> goals, int renamedP) {
		Goal[] result = new Goal[goals.size()];
		for (int i=0; i<result.length; i++) {
			result[i] = applyToGoal(goals.get(i), renamedP);
		}
		return result;
		/*
		 * 
        """"""
        return map(lambda g:self.applyToGoal(g,renamedOrNot), goals)
		 */
	}
	/**
	 * Return the result of applying this substitution to the goal list G1,.,,,,Gk
	 * @param goals
	 * @param renamedP
	 * @return
	 */
	public Goal[] applyToGoalList(Goal[] goals, int renamedP) {
		Goal[] result = new Goal[goals.length];
		for (int i=0; i<result.length; i++) {
			result[i] = applyToGoal(goals[i], renamedP);
		}
		return result;
	}
	/**
	 * Return theta(g), the result of applying this substitution to the goal.
	 * @param goal
	 * @param renamedP
	 * @return
	 */
	public Goal applyToGoal(Goal goal, int renamedP) {
		Argument[] args = goal.getArgs();
		Argument[] newArgs = new Argument[args.length];
		for (int i=0; i<args.length; i++) {
			newArgs[i] = this.applyToAtom(args[i], renamedP);
		}
		return new Goal(goal.getFunctor(), newArgs);
	}

	public String toString() {
	    StringBuilder sb = new StringBuilder("theta{");
	    Dictionary.buildString(this.dict,sb," ").append(" }");
	    return sb.toString();
	}
}
