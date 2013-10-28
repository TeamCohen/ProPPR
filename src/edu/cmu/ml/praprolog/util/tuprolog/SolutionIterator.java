package edu.cmu.ml.praprolog.util.tuprolog;

import java.util.Iterator;

import alice.tuprolog.MalformedGoalException;
import alice.tuprolog.NoMoreSolutionException;
import alice.tuprolog.Prolog;
import alice.tuprolog.SolveInfo;
import alice.tuprolog.Term;

public class SolutionIterator implements Iterable<SolveInfo>, Iterator<SolveInfo> {
	private SolveInfo info;
	private Prolog engine;
	private boolean hasNext;
	public SolutionIterator(Prolog engine, String theory) throws MalformedGoalException {
		this.init(engine,engine.solve(theory));
	}
	public SolutionIterator(Prolog engine, Term theory) {
		this.init(engine,engine.solve(theory));
	}
	private void init(Prolog engine, SolveInfo info) {
		this.engine = engine;
		this.info = info;
		this.hasNext = info.isSuccess();
	}
	
	@Override
	public Iterator<SolveInfo> iterator() {
		return this;
	}

	@Override
	public boolean hasNext() {
		return this.hasNext;
	}

	@Override
	public SolveInfo next() {
		SolveInfo next = this.info;
		this.hasNext = false;
		if (engine.hasOpenAlternatives()) {
			try {
				this.info = engine.solveNext();
				this.hasNext = info.isSuccess();
			} catch (NoMoreSolutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return next;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("Can't remove from this iterator!");
	}

}
