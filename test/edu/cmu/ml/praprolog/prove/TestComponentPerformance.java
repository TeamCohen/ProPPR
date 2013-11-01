package edu.cmu.ml.praprolog.prove;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;

/**
 * Runs N outlinks queries on a component or logic program and records timing data.
 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
 *
 */
public class TestComponentPerformance {
	public static void main(String[] args) {
		if (args.length<4) {
			System.err.println("Usage:\n\tntimes {component|component:component:...} name queryfile.txt\n");
			System.exit(0);
		}
		String nstring = args[0], components=args[1], name = args[2], qfile = args[3];

		long totalstart,freestart,totalload,freeload,totaloutlinks,freeoutlinks;

		int TIMES=Integer.parseInt(nstring);
		LogicProgramState[] states = new LogicProgramState[TIMES];

		try {
			LineNumberReader reader;
			reader = new LineNumberReader(new FileReader(qfile));
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


		totalstart = Runtime.getRuntime().totalMemory();
		freestart = Runtime.getRuntime().freeMemory();
		System.out.println("Loading component(s)...");
		String[] cstring = components.split(":");
		long gstart = System.currentTimeMillis();
		Component[] cs = Component.loadComponents(cstring, Component.ALPHA_DEFAULT);
		long gend = System.currentTimeMillis();
		System.out.println(new StringBuilder("ms load ")
		.append(name).append(" ")
		.append(TIMES).append(" 0 ")
		.append(gend-gstart).toString());

		totalload = Runtime.getRuntime().totalMemory();
		freeload = Runtime.getRuntime().freeMemory();

		int n=0;
		System.out.println("Outlinks...");
		if (cs.length == 1) {
			Component c = cs[0];
			gstart = System.currentTimeMillis();
			for(int i=0;i<TIMES;i++) n+=c.outlinks(states[i]).size();
			gend = System.currentTimeMillis();
		} else {
			LogicProgram lp = new LogicProgram(cs);
			gstart = System.currentTimeMillis();
			for(int i=0;i<TIMES;i++) 
				n+=lp.lpOutlinks(states[i],LogicProgram.DEFAULT_TRUELOOP, LogicProgram.DEFAULT_RESTART).size();
			gend = System.currentTimeMillis();
		}

		System.out.println(new StringBuilder("ms outlinks ")
		.append(name).append(" ")
		.append(TIMES).append(" ")
		.append(n).append(" ")
		.append(gend-gstart).toString());

		totaloutlinks = Runtime.getRuntime().totalMemory();
		freeoutlinks = Runtime.getRuntime().freeMemory();

		System.out.println(
				new StringBuilder("mb start ")
				.append(name).append(" ")
				.append(totalstart).append(" ")
				.append(freestart).append(" ")
				.append(totalstart-freestart).toString());
		System.out.println(
				new StringBuilder("mb load ")
				.append(name).append(" ")
				.append(totalload).append(" ")
				.append(freeload).append(" ")
				.append(totalload-freeload).toString());
		System.out.println(
				new StringBuilder("mb outlinks ")
				.append(name).append(" ")
				.append(totaloutlinks).append(" ")
				.append(freeoutlinks).append(" ")
				.append(totaloutlinks-freeoutlinks).toString());

	} 
}
