package edu.cmu.ml.praprolog.prove.v1;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.Arrays;

import edu.cmu.ml.praprolog.prove.v1.Goal;
import edu.cmu.ml.praprolog.prove.v1.GraphComponent;
import edu.cmu.ml.praprolog.prove.v1.LogicProgramState;
import edu.cmu.ml.praprolog.prove.v1.ProPPRLogicProgramState;
import edu.cmu.ml.praprolog.prove.v1.SparseGraphComponent;

public class TestGraphlikeComponents {

	public static void main(String[] args) {
		if (args.length<2) {
//			System.err.println("Usage:\n\tntimes {program.graph|program.sparse} functor,arg,-1\n");
			System.err.println("Usage:\n\tntimes {program.graph|program.sparse} queryfile.txt\n");
			System.exit(0);
		}
		long totalstart,freestart,totalload,freeload,totaloutlinks,freeoutlinks;

		int TIMES=Integer.parseInt(args[0]);
		LogicProgramState[] states = new LogicProgramState[TIMES];

		try {
			LineNumberReader reader;
				reader = new LineNumberReader(new FileReader(args[2]));
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
		
		if (args[1].endsWith(".graph")) {
			totalstart = Runtime.getRuntime().totalMemory();
			freestart = Runtime.getRuntime().freeMemory();
			{
				System.out.println("Loading graph component...");
				long gstart = System.currentTimeMillis();
				GraphComponent g = GraphComponent.load(args[1]);
				long gend = System.currentTimeMillis();
				System.out.println("ms load graph "+TIMES+" 0 "+(gend-gstart));

				totalload = Runtime.getRuntime().totalMemory();
				freeload = Runtime.getRuntime().freeMemory();

				System.out.println("Outlinks for graph component...");
				int n=0;
				gstart = System.currentTimeMillis();
				for(int i=0;i<TIMES;i++) n+=g.outlinks(states[i]).size();
				gend = System.currentTimeMillis();
				System.out.println("ms outlinks graph "+TIMES+" "+n+" "+(gend-gstart));

				totaloutlinks = Runtime.getRuntime().totalMemory();
				freeoutlinks = Runtime.getRuntime().freeMemory();
			}

			System.out.println("mb start graph "+totalstart+" "+freestart+" "+(totalstart-freestart));
			System.out.println("mb load graph "+totalload+" "+freeload+" "+(totalload-freeload));
			System.out.println("mb outlinks graph "+totaloutlinks+" "+freeoutlinks+" "+(totaloutlinks-freeoutlinks));

		} else {
			totalstart = Runtime.getRuntime().totalMemory();
			freestart = Runtime.getRuntime().freeMemory();
			{
				System.out.println("Loading sparse component...");
				long sstart = System.currentTimeMillis();
				SparseGraphComponent s = SparseGraphComponent.load(args[1]);
				long send = System.currentTimeMillis();
				System.out.println("ms load sparse "+TIMES+" 0 "+(send-sstart));

				totalload = Runtime.getRuntime().totalMemory();
				freeload = Runtime.getRuntime().freeMemory();

				System.out.println("Outlinks for sparse component...");
				int n=0;
				sstart = System.currentTimeMillis();
				for(int i=0;i<TIMES;i++) n+=s.outlinks(states[i]).size();
				send = System.currentTimeMillis();
				System.out.println("ms outlinks sparse "+TIMES+" "+n+" "+(send-sstart));

				totaloutlinks = Runtime.getRuntime().totalMemory();
				freeoutlinks = Runtime.getRuntime().freeMemory();
			}

			System.out.println("mb start sparse "+totalstart+" "+freestart+" "+(totalstart-freestart));
			System.out.println("mb load sparse "+totalload+" "+freeload+" "+(totalload-freeload));
			System.out.println("mb outlinks sparse "+totaloutlinks+" "+freeoutlinks+" "+(totaloutlinks-freeoutlinks));
		}
	}

}
