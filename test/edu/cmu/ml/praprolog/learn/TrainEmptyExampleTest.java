package edu.cmu.ml.praprolog.learn;

import static org.junit.Assert.*;

import org.junit.Test;


public class TrainEmptyExampleTest {

	@Test
	public void test() {
		edu.cmu.ml.praprolog.trove.Trainer.main(new String[] {"testcases/fatherMother.gr-grounded","testcases/tmp"});
		edu.cmu.ml.praprolog.Trainer.main(new String[] {"testcases/fatherMother.gr-grounded","testcases/tmp"});
	}

}
