package edu.cmu.ml.praprolog.prove;

import java.util.Arrays;

public class TestGraphlikeComponents {

	public static void main(String[] args) {
		if (args.length<2) {
			System.err.println("Usage:\n\t{program.graph|program.sparse} functor,arg,-1\n");
			System.exit(0);
		}
		LogicProgramState state = new LogicProgramState(Goal.decompile(args[1]));
		long totalstart,freestart,totalload,freeload,totaloutlinks,freeoutlinks;
		
		String wait=System.console().readLine();

		if (args[0].endsWith(".graph")) {
			totalstart = Runtime.getRuntime().totalMemory();
			freestart = Runtime.getRuntime().freeMemory();
			{
				System.out.println("Loading graph component...");
				long gstart = System.currentTimeMillis();
				GraphComponent g = GraphComponent.load(args[0]);
				long gend = System.currentTimeMillis();
				System.out.println("ms load graph "+(gend-gstart));

				totalload = Runtime.getRuntime().totalMemory();
				freeload = Runtime.getRuntime().freeMemory();

				System.out.println("Outlinks for graph component...");
				gstart = System.currentTimeMillis();
				for(int i=0;i<100;i++) g.outlinks(state);
				gend = System.currentTimeMillis();
				System.out.println("ms outlinks graph "+(gend-gstart));

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
				SparseGraphComponent s = SparseGraphComponent.load(args[0]);
				long send = System.currentTimeMillis();
				System.out.println("ms load sparse "+(send-sstart));

				totalload = Runtime.getRuntime().totalMemory();
				freeload = Runtime.getRuntime().freeMemory();

				System.out.println("Outlinks for sparse component...");
				sstart = System.currentTimeMillis();
				for(int i=0;i<100;i++) s.outlinks(state);
				send = System.currentTimeMillis();
				System.out.println("ms outlinks sparse "+(send-sstart));

				totaloutlinks = Runtime.getRuntime().totalMemory();
				freeoutlinks = Runtime.getRuntime().freeMemory();
			}

			System.out.println("mb start sparse "+totalstart+" "+freestart+" "+(totalstart-freestart));
			System.out.println("mb load sparse "+totalload+" "+freeload+" "+(totalload-freeload));
			System.out.println("mb outlinks sparse "+totaloutlinks+" "+freeoutlinks+" "+(totaloutlinks-freeoutlinks));
		}
	}

}
