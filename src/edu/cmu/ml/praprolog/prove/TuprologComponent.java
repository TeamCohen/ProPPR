package edu.cmu.ml.praprolog.prove;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import alice.tuprolog.InvalidTheoryException;
import alice.tuprolog.Prolog;
import alice.tuprolog.Struct;
import alice.tuprolog.Term;
import alice.tuprolog.Theory;
import alice.tuprolog.Var;

import edu.cmu.ml.praprolog.util.SymbolTable;

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

	@Override
	public boolean claim(LogicProgramState state) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<Outlink> outlinks(LogicProgramState state) {
		
		// TODO Auto-generated method stub
		return null;
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
