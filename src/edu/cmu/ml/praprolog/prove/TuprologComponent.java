package edu.cmu.ml.praprolog.prove;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import alice.tuprolog.InvalidTheoryException;
import alice.tuprolog.MalformedGoalException;
import alice.tuprolog.NoSolutionException;
import alice.tuprolog.Prolog;
import alice.tuprolog.SolveInfo;
import alice.tuprolog.Struct;
import alice.tuprolog.Term;
import alice.tuprolog.Theory;
import alice.tuprolog.Var;
import edu.cmu.ml.praprolog.util.SymbolTable;
import edu.cmu.ml.praprolog.util.tuprolog.SolutionIterator;
import edu.cmu.ml.praprolog.util.tuprolog.TuprologAdapter;

public class TuprologComponent extends Component {
	private static final String COMPILED_EXTENSION = "x.pl";
	private static final Logger log = Logger.getLogger(TuprologComponent.class);
	private static final String OUTLINKS_RULES = "outlinks.2p";
	public static final String UNCOMPILED_EXTENSION = ".pl";
	private Prolog engine;
	private static void loadOutlinks(Prolog p) {
		try {
			InputStream is = TuprologComponent.class.getClassLoader().getResourceAsStream(OUTLINKS_RULES);
			p.addTheory(new Theory(is));
		} catch (InvalidTheoryException e) {
			log.error(e);
			throw new IllegalArgumentException(e);
		} catch (FileNotFoundException e) {
			log.error(e);
			throw new IllegalArgumentException(e);
		} catch (IOException e) {
			log.error(e);
			throw new IllegalArgumentException(e);
		}
	}
	public TuprologComponent() {		
		engine = new Prolog();
		loadOutlinks(engine);
	}
	public TuprologComponent(String ... files) {
		this();
		List<String> uncompiled = new ArrayList<String>();
		List<String> compiled = new ArrayList<String>();
		for (String f : files) {
			if (f.endsWith(COMPILED_EXTENSION)) compiled.add(f);
			else uncompiled.add(f);
		}
		if (!uncompiled.isEmpty()) compiled.add(compileTheories(uncompiled));
		for (String f : compiled) this.addTheory(f);
	}
	public String compileTheories(Collection<String> filenames) {
		return compileTheories(filenames,"compiled"+System.currentTimeMillis()+COMPILED_EXTENSION);
	}
	public String compileTheories(Collection<String> filenames, String compiledFile) {
		Prolog compileEngine = new Prolog();
		loadOutlinks(compileEngine);
		for (String s : filenames) {
			try {
				compileEngine.addTheory(new Theory(new FileInputStream(s)));
			} catch (InvalidTheoryException e) {
				log.error("Trouble with "+s,e);
			} catch (FileNotFoundException e) {
				log.error("Trouble with "+s,e);
			} catch (IOException e) {
				log.error("Trouble with "+s,e);
			}
		}
		try {
			compileEngine.solve("expandAllClauses('"+compiledFile+"').");
			log.info("Saved compiled rules as "+compiledFile);
		} catch (MalformedGoalException e) {
			log.error("Trouble expanding clauses",e);
		}
		return compiledFile;
	}
	public void addTheory(String filename) {
		if (!filename.endsWith(COMPILED_EXTENSION)) {
			log.warn("Prolog file "+filename+" may not have been compiled first. Did you run edu.cmu.ml.praprolog.prove.TuprologComponent on it?");
		}
		try {
			engine.addTheory(new Theory(new FileInputStream(filename)));
		} catch (InvalidTheoryException e) {
			log.error("Trouble with "+filename,e);
		} catch (FileNotFoundException e) {
			log.error("Trouble with "+filename,e);
		} catch (IOException e) {
			log.error("Trouble with "+filename,e);
		}
	}

	@Override
	public boolean claim(LogicProgramState state) {
		Term tustate = ((TuprologLogicProgramState) state.asTuprolog()).asTerm();
		Term query = new Struct("claim",tustate);
		return this.engine.solve(query).isSuccess();
	}

	@Override
	public List<Outlink> outlinks(LogicProgramState state0) {
		TuprologLogicProgramState state = (TuprologLogicProgramState) state0.asTuprolog();
		Term tustate = state.asTerm();
		Term query = new Struct("outlinks",tustate,new Var("S1"),new Var("F1"));
		ArrayList<Outlink> ret = new ArrayList<Outlink>();
		for (SolveInfo info : new SolutionIterator(this.engine, query)) {
			try {
				Term solution = info.getVarValue("S1");
				Term features = info.getVarValue("F1");
				ret.add(TuprologAdapter.termsToOutlink(solution,features,state));
			} catch (NoSolutionException e) {
				log.error("Something hideously wrong with the solution iterator",e);
			}
		}
		return ret;
	}

	@Override
	public void compile() {
		// TODO Auto-generated method stub

	}

	@Override
	public void compile(SymbolTable variableSymTab) {
		// TODO Auto-generated method stub

	}

	public static TuprologComponent load(String filename) {
		return new TuprologComponent(filename);
	}
	public static void main(String[] args) {
		if (args.length < 2) {
			System.out.println("Usage:\n\ttheoryfile.pl compiledtheoryfilex.pl\n\nCompiles a theory file to fast-tuprolog format (*x.pl)\n");
			System.exit(0);
		}
		String theory = args[0];
		String compile = args[1];
		System.out.println("Compiling "+theory+"...");
		new TuprologComponent().compileTheories(Collections.singleton(theory), compile);
		System.out.println("Done.");
	}
}
