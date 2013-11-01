package edu.cmu.ml.praprolog.prove;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import alice.tuprolog.InvalidTheoryException;
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
	public static final String FILE_EXTENSION = "pl";
	private Prolog engine;
	public TuprologComponent() {		
		engine = new Prolog();
		try {
			engine.addTheory(new Theory(ClassLoader.getSystemResourceAsStream("outlinks.2p")));
		} catch (InvalidTheoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public TuprologComponent(String ... files) {
		this();
		for (String f : files) this.addTheory(f);
	}
	public void addTheory(String filename) {
		try {
			engine.addTheory(new Theory(new FileInputStream(filename)));
		} catch (InvalidTheoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
				e.printStackTrace();
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
}
