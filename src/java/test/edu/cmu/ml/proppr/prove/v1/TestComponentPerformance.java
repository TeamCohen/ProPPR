package edu.cmu.ml.proppr.prove.v1;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;

import edu.cmu.ml.proppr.prove.v1.Component;
import edu.cmu.ml.proppr.prove.v1.Goal;
import edu.cmu.ml.proppr.prove.v1.LogicProgram;
import edu.cmu.ml.proppr.prove.v1.LogicProgramException;
import edu.cmu.ml.proppr.prove.v1.LogicProgramState;
import edu.cmu.ml.proppr.prove.v1.ProPPRLogicProgramState;

/**
 * Runs N outlinks queries on a component or logic program and records timing data.
 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
 *
 */
public class TestComponentPerformance {
	public static void main(String[] args) throws LogicProgramException {
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
		Component[] cs = Component.loadComponents(cstring, Component.ALPHA_DEFAULT,null);
		long gend = System.currentTimeMillis();
		System.out.println(new StringBuilder("dat ms load ")
		.append(name).append(" ")
		.append(TIMES).append(" 0 ")
		.append(gend-gstart).toString());

		totalload = Runtime.getRuntime().totalMemory();
		freeload = Runtime.getRuntime().freeMemory();

		int n=0; long t=0;
		System.out.println("Outlinks...");
		if (cs.length == 1) {
			Component c = cs[0];
			gstart = System.currentTimeMillis();
			for(int i=0;i<TIMES;i++) {
				n+=c.outlinks(states[i]).size();
				gend = System.currentTimeMillis();
				t += gend-gstart;
				if (i % 100 == 0) {
					System.out.println(new StringBuilder("dat ms outlinks ")
					.append(name).append(" ")
					.append(i).append(" ")
					.append(n).append(" ")
					.append(t).toString());
				}
				gstart = System.currentTimeMillis();
			}
			gend = System.currentTimeMillis();
		} else {
			LogicProgram lp = new LogicProgram(cs);
			gstart = System.currentTimeMillis();
			for(int i=0;i<TIMES;i++) {
				try {
					n+=lp.lpOutlinks(states[i],LogicProgram.DEFAULT_TRUELOOP, LogicProgram.DEFAULT_RESTART).size();
				} catch (LogicProgramException e) {
					throw new IllegalStateException(e);
				}
				gend = System.currentTimeMillis();
				t += gend-gstart;
				if (i % 100 == 0) {
					System.out.println(new StringBuilder("dat ms outlinks ")
					.append(name).append(" ")
					.append(i).append(" ")
					.append(n).append(" ")
					.append(t).toString());
				}
				gstart = System.currentTimeMillis();
			}
		}

		System.out.println(new StringBuilder("dat ms outlinks ")
		.append(name).append(" ")
		.append(TIMES).append(" ")
		.append(n).append(" ")
		.append(t).toString());

		totaloutlinks = Runtime.getRuntime().totalMemory();
		freeoutlinks = Runtime.getRuntime().freeMemory();

		System.out.println(
				new StringBuilder("dat mb start ")
				.append(name).append(" ")
				.append(totalstart).append(" ")
				.append(freestart).append(" ")
				.append(totalstart-freestart).toString());
		System.out.println(
				new StringBuilder("dat mb load ")
				.append(name).append(" ")
				.append(totalload).append(" ")
				.append(freeload).append(" ")
				.append(totalload-freeload).toString());
		System.out.println(
				new StringBuilder("dat mb outlinks ")
				.append(name).append(" ")
				.append(totaloutlinks).append(" ")
				.append(freeoutlinks).append(" ")
				.append(totaloutlinks-freeoutlinks).toString());

	} 
}
