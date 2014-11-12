package edu.cmu.ml.proppr.prove.v1;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import edu.cmu.ml.proppr.util.Configuration;

import bsh.EvalError;
import bsh.Interpreter;

public class Prompt {
	public static final String HELPSTR="This is a beanshell, a command-line interpreter for java.\n\n"+
			"Type java statements at the prompt. Don't forget your semicolons.\n\n"+
			"For a listing of available variables, type 'list();'.\n\n"+
			"To quit, type 'quit();'";
	public static void main(String[] args) {
		Configuration c = new Configuration(args, new TracingDfsProver(), 
				Configuration.USE_DEFAULTS & ~Configuration.USE_OUTPUT);
		
		System.out.println("Starting up beanshell...");
		Interpreter bsh = new Interpreter(new InputStreamReader(System.in), System.out, System.err, true);
		try {
			bsh.set("prv", c.prover);
			bsh.set("dpr", new DprProver());
			bsh.set("cmp", new Comparator<Map.Entry<LogicProgramState,Double>>() {
				public int compare(Entry<LogicProgramState, Double> o1,
						Entry<LogicProgramState, Double> o2) {
					if (o1.getKey().isSolution() ^ o2.getKey().isSolution()) {
						return o1.getKey().isSolution() ? -1 : 1;
					}
					return o2.getValue().compareTo(o1.getValue());
				}});
			bsh.println("prv set: "+bsh.get("prv"));
			if (c.programFiles != null) {
				bsh.set("lp", new LogicProgram(Component.loadComponents(c.programFiles,c.alpha,c)));
				bsh.println("lp set: "+bsh.get("lp"));
			}
			bsh.eval("help() { print(\"This is a beanshell, a command-line interpreter for java. A full beanshell manual is available at <http://www.beanshell.org/manual/contents.html>.\\n\\n"+
					"Type java statements and expressions at the prompt. Don't forget semicolons.\\n\\n"+
					"Type 'help();' for help, 'quit();' to quit; 'list();' for a variable listing.\\n\\n"+
					"'show();' will toggle automatic printing of the results of expressions. Otherwise you must use 'print( expr );' to see results.\\n\\n"+
					"'javap( x );' will list the fields and methods available on an object. Be warned; beanshell has trouble locating methods that are only defined on the superclass.\\n\\n"+
					"'[sol = ]run(prover,logicprogram,\\\"functor(arg,arg,...,arg)\\\")' will prove the associated state.\\n\\n"+
					"'pretty(sol)' will list solutions first, then intermediate states in descending weight order.\\n\"); }");
			bsh.eval("list() { for (String x : super.variables) print(x); }");
			bsh.eval("quit() { System.exit(0); }");
			bsh.eval("run(prv,lp,query) {\n"+
					"String[] parts = query.split(\"\\\\(\",2);\n"+
					"String functor = parts[0];\n"+
					"String[] args = parts[1].substring(0,parts[1].indexOf(\")\")).split(\",\");\n"+
					"prv.proveState(lp,Prover.parseQuery(functor,args));\n"+
					"}");
//			
			bsh.eval("pretty(sol) {\n"+
					"ArrayList el = new ArrayList();\n"+
					"el.addAll(sol.entrySet());\n"+
					"Collections.sort(el, super.cmp);\n"+
					"for(x : el) { print ( x.getValue() +\": \"+x.getKey() ); }\n"+
					"}");
//			bsh.eval("show();");
			bsh.println("\nType 'help();' for help, 'quit();' to quit; 'list();' for a variable listing.\n");
			bsh.eval("import edu.cmu.ml.praprolog.*;");
			bsh.eval("import edu.cmu.ml.praprolog.prove.*;");
			bsh.eval("import edu.cmu.ml.praprolog.graph.*;");
			bsh.eval("import edu.cmu.ml.praprolog.learn.*;");
			bsh.run();
			
		} catch (EvalError e) {
			System.out.println("Couldn't set initial variables for prover and logic program:");
			e.printStackTrace();
		}
	}
}
