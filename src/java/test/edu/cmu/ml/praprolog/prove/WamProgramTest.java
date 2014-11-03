package edu.cmu.ml.praprolog.prove;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Test;

import edu.cmu.ml.praprolog.prove.wam.WamProgram;
import edu.cmu.ml.praprolog.prove.wam.Instruction.OP;

public class WamProgramTest {
	@Test
	public void testLoad() throws IOException {
		WamProgram program = WamProgram.load(new File("testcases/wam/simpleProgram.wam"));
		OP[] simpleProgram = 
			{
				OP.comment,
				OP.allocate,
				OP.initfreevar,
				OP.initfreevar,
				OP.fclear,
				OP.fpushstart,
				OP.fpushconst,
				OP.fpushconst,
				OP.fpushconst,
				OP.freport,
				OP.pushboundvar,
				OP.pushfreevar,
				OP.callp,
				OP.pushboundvar,
				OP.pushboundvar,
				OP.callp,
				OP.returnp,
				OP.comment,
				OP.allocate,
				OP.initfreevar,
				OP.initfreevar,
				OP.fclear,
				OP.fpushstart,
				OP.fpushconst,
				OP.fpushconst,
				OP.fpushconst,
				OP.freport,
				OP.pushboundvar,
				OP.callp,
				OP.pushboundvar,
				OP.pushboundvar,
				OP.callp,
				OP.returnp,
				OP.comment,
				OP.unifyconst,
				OP.unifyconst,
				OP.fclear,
				OP.fpushstart,
				OP.fpushconst,
				OP.fpushconst,
				OP.fpushconst,
				OP.freport,
				OP.returnp,
				OP.comment,
				OP.unifyconst,
				OP.unifyconst,
				OP.fclear,
				OP.fpushstart,
				OP.fpushconst,
				OP.fpushconst,
				OP.fpushconst,
				OP.freport,
				OP.returnp,
				OP.comment,
				OP.unifyconst,
				OP.fclear,
				OP.fpushstart,
				OP.fpushconst,
				OP.fpushconst,
				OP.fpushconst,
				OP.freport,
				OP.returnp
			};
		assertEquals(simpleProgram.length,program.size());
		for (int i=0; i<simpleProgram.length; i++) {
			assertEquals("Instruction "+i,simpleProgram[i],program.getInstructions().get(i).opcode);
		}
		assertTrue(program.hasLabel("coworker/2"));
		List<Integer> addr = program.getAddresses("coworker/2");
		assertEquals("coworker/2",1,addr.get(0).intValue());
		assertTrue(program.hasLabel("employee/2"));
		addr = program.getAddresses(("employee/2"));
		assertEquals("employee/2",18,addr.get(0).intValue());
	}

}
