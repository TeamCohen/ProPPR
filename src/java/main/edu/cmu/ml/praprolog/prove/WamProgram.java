package edu.cmu.ml.praprolog.prove;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cmu.ml.praprolog.prove.wam.Instruction;

/**
 * Holds a modified Warran abstract machine program, consisting of:

    1)instructions = a list of tuples (opcode,arg1,...)

    2) labels = a defaultdict such that labels["p/n"] is a list of
     addresses (ie, indices in instructions) where the instructions
     for the clauses of p/n start.

    3) instLabels = a dict such that instLabels[i] is the label
    given to instruction i, if there is such a label.
 * @author "Kathryn Mazaitis <krivard@cs.cmu.edu>"
 *
 */
public class WamProgram {
	private List<Instruction> instructions;
	private List<String> instLabels;
	private Map<String,Integer> labels;
	public WamProgram() {
		instructions = new ArrayList<Instruction>();
		instLabels = new ArrayList<String>();
		labels = new HashMap<String,Integer>();
	}
	public void append(Instruction inst) {
		
	}
//	public void truncateTo(int addr) {}
//	public void insertLabel(String key) {}
	
	public static WamProgram load(String file) throws IOException {
		LineNumberReader reader = new LineNumberReader(new FileReader(file));
		WamProgram program = new WamProgram();
		for (String line; (line = reader.readLine()) != null;){
			program.append(Instruction.parseInstruction(line));
		}
	}
}
