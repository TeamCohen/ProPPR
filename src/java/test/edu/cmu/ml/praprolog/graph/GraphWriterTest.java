package edu.cmu.ml.praprolog.graph;

import static org.junit.Assert.*;

import java.util.Collections;

import org.apache.log4j.Appender;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;

import edu.cmu.ml.praprolog.prove.Argument;
import edu.cmu.ml.praprolog.prove.ConstantArgument;
import edu.cmu.ml.praprolog.prove.Goal;
import edu.cmu.ml.praprolog.prove.LogicProgramState;
import edu.cmu.ml.praprolog.prove.ProPPRLogicProgramState;
import edu.cmu.ml.praprolog.prove.RenamingSubstitution;
import edu.cmu.ml.praprolog.prove.VariableArgument;

public class GraphWriterTest {

	int i=0, j=0;
	ProPPRLogicProgramState[] state = new ProPPRLogicProgramState[18];
	
	int[] nodes = {1,2,3,4,5,6,7,7,8, 9, 9, 9,10,11,12,13,13};
	int[] edges = {1,2,3,4,5,6,7,8,9,10,10,11,12,13,14,15,15};
	int[] ow    = {0,0,0,0,0,0,0,1,0, 0, 1, 1, 0, 0, 0, 0, 1};
	
	public void printAndTest(GraphWriter gw) {
		i++; 
		printAndTest(gw, i-1,i);
	}
	
	public void printAndTest(GraphWriter gw, int from, int to) {
		System.out.println(String.format("%2d %s",to,state[to]));
		
		gw.writeEdge(state[from], state[to], Collections.EMPTY_LIST);
		assertEquals("nodes",nodes[j],gw.getGraph().getNumNodes());
		assertEquals("edges",edges[j],gw.getGraph().getNumEdges());
		System.out.println("   > "+gw.getGraph().getNumNodes());
		if (ow[j]==1) {
			boolean foundit = false;
			for(int k=0; k<to; k++) {
				if (state[k].equals(state[to])) { foundit = true;
					System.out.println("   > equals "+k+" "+ state[k]);
				} if (state[k].hashCode() == state[to].hashCode()) { foundit = true;
					System.out.println("   > hash== "+k+" "+ state[k]);
				}
			}
			if (from < to-1) foundit = true;
			if (!foundit) System.out.println("   > NOT FOUND");
		}
		j++;
	}
	
	@Test
	public void test() {
		BasicConfigurator.configure(); Logger.getRootLogger().setLevel(Level.WARN);
//		Logger.getLogger(GraphWriter.class).setLevel(Level.DEBUG);
		
		GraphWriter gw = new GraphWriter();
		
		
		state[0] = new ProPPRLogicProgramState(Goal.decompile("samebib,class_338,-1"));
		System.out.println(state[0]);
		
		Goal[] goals1 = {
				Goal.decompile("author,-1,-3"),
				Goal.decompile("sameauthor,-3,-4"),
				Goal.decompile("authorinverse,-4,-2")};
		RenamingSubstitution th = RenamingSubstitution.unify(Goal.decompile("samebib,-1,-2"), state[0].getGoal(0), 
				state[0].getVarSketchSize(), 1, 0);
		state[1] = (ProPPRLogicProgramState) state[0].child(goals1, th); //System.out.println("VARSKETCH "+state[0].getVarSketchSize());
		printAndTest(gw);
		
		th = new RenamingSubstitution(state[1].getOffset()); th.put(new VariableArgument(-2),new ConstantArgument("author_y_freund_"));
		state[2] = (ProPPRLogicProgramState) state[1].child(new Goal[0], th);
		printAndTest(gw);
		
		Goal[] goals3 = {
				Goal.decompile("haswordauthor,-1,-3"),
				Goal.decompile("haswordauthorinverse,-3,-2"),
				Goal.decompile("keyauthorword,-3")
		};
		state[3] = (ProPPRLogicProgramState) state[2].child(goals3, RenamingSubstitution.unify(Goal.decompile("sameauthor,-1,-2"), state[2].getGoal(0), 
				state[2].getVarSketchSize(), 1,0));//System.out.println("VARSKETCH "+state[2].getVarSketchSize());
		printAndTest(gw);
		
		th = new RenamingSubstitution(state[3].getOffset()); th.put(new VariableArgument(-2),new ConstantArgument("word_freund"));
		state[4] = (ProPPRLogicProgramState) state[3].child(new Goal[0],th);
		printAndTest(gw);
		
		th = new RenamingSubstitution(state[4].getOffset()); th.put(new VariableArgument(-2), new ConstantArgument("author_y_freund_"));
		state[5] = (ProPPRLogicProgramState) state[4].child(new Goal[0],th);
		printAndTest(gw);
		
		state[6] = (ProPPRLogicProgramState) state[5].child(new Goal[0], new RenamingSubstitution(state[5].getOffset()));
		printAndTest(gw);
		
		th = new RenamingSubstitution(state[6].getOffset()); th.put(new VariableArgument(-1), new ConstantArgument("class_338"));
		state[7] = (ProPPRLogicProgramState) state[6].child(new Goal[0], th);
		printAndTest(gw);
		
		th = new RenamingSubstitution(state[3].getOffset()); th.put(new VariableArgument(-2),new ConstantArgument("word_y"));
		state[8] = (ProPPRLogicProgramState) state[3].child(new Goal[0], th);  i++;
		printAndTest(gw,3,8);
		
		th = new RenamingSubstitution(state[8].getOffset()); th.put(new VariableArgument(-2), new ConstantArgument("author_y_freund_"));
		state[9] = (ProPPRLogicProgramState) state[8].child(new Goal[0],th);
		printAndTest(gw);
		
		state[10] = (ProPPRLogicProgramState) state[9].child(new Goal[0], new RenamingSubstitution(state[9].getOffset()));
		printAndTest(gw);
		
		th = new RenamingSubstitution(state[10].getOffset()); th.put(new VariableArgument(-1),new ConstantArgument("class_338"));
		state[11] = (ProPPRLogicProgramState) state[10].child(new Goal[0], th);
		printAndTest(gw);
		
		Goal[] goals12 = {
				Goal.decompile("sameauthor,-1,-3"),
				Goal.decompile("sameauthor,-3,-2")
		};
		state[12] = (ProPPRLogicProgramState) state[2].child(goals12, RenamingSubstitution.unify(Goal.decompile("sameauthor,-1,-2"), state[2].getGoal(0),
				state[2].getVarSketchSize(), 1, 0)); i++;
		printAndTest(gw,2,12);
		
		Goal[] goals13 = {
				Goal.decompile("haswordauthor,-1,-3"),
				Goal.decompile("haswordauthorinverse,-3,-2"),
				Goal.decompile("keyauthorword,-3")
		};
		state[13] = (ProPPRLogicProgramState) state[12].child(goals13, RenamingSubstitution.unify(Goal.decompile("sameauthor,-1,-2"), state[12].getGoal(0),
				state[12].getVarSketchSize(), 1, 0));
		printAndTest(gw);
		
		th = new RenamingSubstitution(state[13].getOffset()); th.put(new VariableArgument(-2), new ConstantArgument("word_freund"));
		state[14] = (ProPPRLogicProgramState) state[13].child(new Goal[0], th);
		printAndTest(gw);
		
		th = new RenamingSubstitution(state[14].getOffset()); th.put(new VariableArgument(-2), new ConstantArgument("author_y_freund_"));
		state[15] = (ProPPRLogicProgramState) state[14].child(new Goal[0], th);
		printAndTest(gw);
		
		state[16] = (ProPPRLogicProgramState) state[15].child(new Goal[0], new RenamingSubstitution(state[15].getOffset()));
		printAndTest(gw);
		
		// sameauthor,-1,-2 & haswordauthor,-1,-3 & haswordauthorinverse,-3,-2 & keyauthorword,-3
		Goal[] goals17 = {
				Goal.decompile("haswordauthor,-1,-3"),
				Goal.decompile("haswordauthorinverse,-3,-2"),
				Goal.decompile("keyauthorword,-3")
		};
		state[17] = (ProPPRLogicProgramState) state[16].child(goals17, RenamingSubstitution.unify(Goal.decompile("sameauthor,-1,-2"), state[16].getGoal(0),
				state[16].getVarSketchSize(), 1, 0));
		assertEquals("hash equality",state[3].hashCode(),state[17].hashCode());
		assertTrue("state equality",state[3].equals(state[17]));
		printAndTest(gw);
		
		
	}

}
