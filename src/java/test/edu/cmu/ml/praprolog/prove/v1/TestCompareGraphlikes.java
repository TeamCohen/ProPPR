package edu.cmu.ml.praprolog.prove.v1;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Iterator;
import java.util.List;

import edu.cmu.ml.praprolog.prove.v1.Goal;
import edu.cmu.ml.praprolog.prove.v1.GraphComponent;
import edu.cmu.ml.praprolog.prove.v1.LogicProgramState;
import edu.cmu.ml.praprolog.prove.v1.ProPPRLogicProgramState;
import edu.cmu.ml.praprolog.prove.v1.SparseGraphComponent;
import edu.cmu.ml.praprolog.prove.v1.Component.Outlink;

/**
 * Runs N outlinks queries from a file on a GraphComponent and a SparseGraphComponent
 * and checks that their outputs match.
 * 
 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
 *
 */
public class TestCompareGraphlikes {

	public TestCompareGraphlikes() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) {
		if (args.length<2) {
			//			System.err.println("Usage:\n\tntimes {program.graph|program.sparse} functor,arg,-1\n");
			System.err.println("Usage:\n\tntimes program.graph program.sparse queryfile.txt\n");
			System.exit(0);
		}

		int TIMES=Integer.parseInt(args[0]);
		LogicProgramState[] states = new LogicProgramState[TIMES];

		try {
			LineNumberReader reader;
			reader = new LineNumberReader(new FileReader(args[3]));
			int i=0;
			for(String line=""; (line=reader.readLine()) != null && i<states.length; ) {
				states[i++] = new ProPPRLogicProgramState(Goal.decompile(line.trim()));
			}
			reader.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(0);
		}

		System.out.println("Loading graph component...");
		GraphComponent g = GraphComponent.load(args[1]);
		System.out.println("Loading sparse component...");
		SparseGraphComponent s = SparseGraphComponent.load(args[2]);

		System.out.println("Outlinks...");
		for(int i=0;i<TIMES;i++) {
			List<Outlink> graphlinks = g.outlinks(states[i]);
			List<Outlink> sparselinks = s.outlinks(states[i]);
			if (graphlinks.size() != sparselinks.size()) {
				System.err.println("query "+i+" didn't match: graph "+graphlinks.size()+", sparse "+sparselinks.size());
				String cmd = System.console().readLine("> ");
				if ("p".equals(cmd)) {
					Iterator<Outlink> gi = graphlinks.iterator();
					Iterator<Outlink> si = sparselinks.iterator();
					while(gi.hasNext() || si.hasNext()) {
						
						if (gi.hasNext()) System.out.println("G\t"+gi.next().getState());
						if (si.hasNext()) System.out.println("S\t"+si.next().getState());
						System.out.println();
					}
					break;
				}
			}
		}
	}

}
