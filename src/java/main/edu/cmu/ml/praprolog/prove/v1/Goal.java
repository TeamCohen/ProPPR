// status - this works but my encoding now doesn't allow for h or s to
// be a constant first argument! I should use some other convention -
// like using a special char as the final char of the predicate
// symbol.

package edu.cmu.ml.praprolog.prove.v1;

import edu.cmu.ml.praprolog.util.Dictionary;
import edu.cmu.ml.praprolog.util.SymbolTable;
import org.apache.log4j.Logger;

public class Goal implements Comparable<Goal> {

    private static final Logger log = Logger.getLogger(Goal.class);
    private static final String HARD_INDICATOR = "+";
    protected String functor;
    protected Argument[] args;
    protected int hashcode;
    protected String name = "";
    protected String argString;
    protected boolean hardGoal = false;
    private boolean isCompiled = false;

    /**
     * @param fnctr - if the fnctr ends with HARD_INDICATOR then
     *              that substring will be stripped and the goal will be a 'hard goal',
     *              which is treated differently in the prover.
     */
    public Goal(String fnctr, String... args) {
        this.hardGoal = indicatesHardGoal(fnctr);
        this.functor = functorWithoutIndicator(fnctr);
        this.args = new Argument[args.length];
        for (int i = 0; i < args.length; i++) {
            this.args[i] = Argument.fromString(args[i]);
        }
        this.freeze();
    }

    public Goal(String fnctr, Argument[] args) {
        this.hardGoal = indicatesHardGoal(fnctr);
        this.functor = functorWithoutIndicator(fnctr);
        this.args = args; // TODO: may need defensive protection
        this.freeze();
    }

    private boolean indicatesHardGoal(String fnctr) {
        return fnctr.endsWith(HARD_INDICATOR);
    }

    private String functorWithoutIndicator(String fnctr) {
        if (indicatesHardGoal(fnctr)) {
            return fnctr.substring(0, fnctr.length() - HARD_INDICATOR.length());
        } else {
            return fnctr;
        }
    }

    /**
     * (internal) set up hashcode and argstring *
     */
    protected void freeze() {
        hashcode = functor.hashCode();
        StringBuilder sb = new StringBuilder();
        for (Argument a : args) {
            hashcode += a.hashCode();
            sb.append(",").append(a.toString());
        }
        this.argString = sb.toString();
    }

    public String getFunctor() {
        return functor;
    }

    public int getArity() {
        return args.length;
    }

    public Argument[] getArgs() { // TODO: may need defensive protection
        return this.args;
    }

    public Argument getArg(int i) {
        return this.args[i];
    }

    public void setArg(int i, Argument a) {
        if (a == null)
            throw new NullPointerException("cannot have null argument");
        args[i] = a;
    }

    public boolean isHard() {
        return this.hardGoal;
    }

    @Override
    public String toString() {
        String pref = "";
        String suff = "";
        if (this.isHard()) {
            pref = "[";
            suff = "]";
        }
        StringBuilder sb = new StringBuilder(pref + "goal(").append(this.functor);
        return Dictionary.buildString(this.args, sb, ",", false).append(")").append(suff).toString();
    }

    public String toSaveString() {
        StringBuilder sb = new StringBuilder(this.functor);
        if (this.args.length > 0) {
            sb.append("(");
            for (Argument a : this.args) sb.append(a.getName()).append(",");
            sb.deleteCharAt(sb.length() - 1);
            sb.append(")");
        }
        return sb.toString();
    }

    public void compile(SymbolTable variableSymTab) {
        for (int i = 0; i < args.length; i++) {
            String argstring = args[i].getName();
            if (args[i].isConstant() &&
                (argstring.startsWith("_") || argstring.matches("[A-Z].*")))
                args[i] = new VariableArgument(-variableSymTab.getId(argstring));
        }
        this.freeze();
        isCompiled = true;
    }

    public boolean isCompiled() { return isCompiled; }

    @Override
    public boolean equals(Object o) { // FIXME probably slow
        if (!(o instanceof Goal)) return false;
        Goal g = (Goal) o;
        if (this.hashCode() != g.hashCode()) return false;
        if (!this.functor.equals(g.functor)) return false;
        if (this.args.length != g.args.length) return false;
        if (!this.argString.equals(g.argString)) return false;
//		for (int i=0; i<this.args.length; i++) 
//			if (! this.args[i].equals(g.args[i])) return false;
        return true;
    }

    @Override
    public int hashCode() { return hashcode; }

    /**
     * Create a Goal object from the compiled string, in format e.g.
     * <p/>
     * predict,-1,-2
     * <p/>
     * where predict is functor name,  a trailing '+' indicatng a hard goal, -1 -2 are arguments, negative numbers are variables,
     * positive numbers are constants.
     *
     * @param string
     * @return
     */

    public static Goal decompile(String string) {


        String[] functor_args = string.split(",", 2);
        if (functor_args.length == 0) throw new IllegalStateException("Couldn't locate functor in '" + string + "'");
        //functor-only case
        if (functor_args.length == 1) return new Goal(functor_args[0].trim());

        //otherwise parse the arguments
        String[] argstrings = functor_args[1].split(",");
        Argument[] args = new Argument[argstrings.length];
        for (int i = 0; i < argstrings.length; i++) {
            argstrings[i] = argstrings[i].trim();
            int a = 0;
            boolean ithArgStringIsANumber = false;
            try {
                a = Integer.parseInt(argstrings[i]);
                ithArgStringIsANumber = true;
            } catch (NumberFormatException e) {
                ;
            }
            if (ithArgStringIsANumber && a < 0) {
                args[i] = new VariableArgument(a);
            } else {
                args[i] = new ConstantArgument(argstrings[i]);
            }
        }
        return new Goal(functor_args[0].trim(), args);
    }

    /**
     * Create a goal from the string-delimited format
     * functor arg1 arg2 arg3 ...
     *
     * @param string
     * @return
     */
    public static Goal parseGoal(String string) {
        return parseGoal(string, " ");
    }

    @Override
    public int compareTo(Goal arg0) {
        int c = this.functor.compareTo(arg0.functor);
        if (c != 0) return c;
        if (this.args.length != arg0.args.length)
            return this.args.length - arg0.args.length;
        for (int i = 0; i < this.args.length; i++) {
            c = this.args[i].compareTo(arg0.args[i]);
            if (c != 0) return c;
        }
        return 0;
    }

    public static Goal parseGoal(String string, String delim) {
        String[] f_a = string.split(delim, 2);
        if (f_a.length > 1) return new Goal(f_a[0], f_a[1].split(delim));
        return new Goal(f_a[0]);
    }
}
