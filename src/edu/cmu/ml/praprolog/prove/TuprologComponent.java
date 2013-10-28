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
	private Prolog engine;
	public TuprologComponent() {		
		engine = new Prolog();
		try {
			engine.addTheory(new Theory(new FileInputStream("outlinks.2p")));
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
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<Outlink> outlinks(LogicProgramState state) {
		Term tustate = TuprologAdapter.lpStateToTerm(state);
		Term query = new Struct("outlinks",tustate,new Var("S1"),new Var("F1"));
		ArrayList<Outlink> ret = new ArrayList<Outlink>();
		for (SolveInfo info : new SolutionIterator(this.engine, query)) {
			try {
				Term solution = info.getVarValue("S1");
				Term features = info.getVarValue("F1");
				ret.add(TuprologAdapter.termsToOutlink(solution,features));
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

}
