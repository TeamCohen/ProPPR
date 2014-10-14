package edu.cmu.ml.praprolog;

import static org.junit.Assert.*;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;

import edu.cmu.ml.praprolog.util.multithreading.Multithreading;

@Ignore
public class Trainer2Test extends TrainerTest {

	@Override
	public void initTrainer() {
		this.trainer = new Trainer2<String>(this.srw, 2);
//		Logger.getLogger(Trainer2.class).setLevel(Level.DEBUG);
	}
	
	private void nthreadsnthrottle(int nthreads, int throttle) {
		((Trainer2) this.trainer).setThreads(nthreads);
		((Trainer2) this.trainer).setThrottle(throttle);
		int times = 6;
		long accu= 0;
		for (int i=0; i<times; i++) {
			long start = System.currentTimeMillis();
			this.train();
			accu += ((System.currentTimeMillis() - start));
		}
		System.err.println(nthreads+" threads "+throttle+" throttle: " + (accu/times));
	}
	
	@Test
	public void test_1threads_nothrottle() {
		nthreadsnthrottle(1, Multithreading.NO_THROTTLE);
	}
	
//	@Test
//	public void test_1threads_1throttle() {
//		nthreadsnthrottle(1, 1);
//	}
	
	@Test
	public void test_1threads_8throttle() {
		nthreadsnthrottle(1, 8);
	}
	
	@Test
	public void test_2threads_nothrottle() {
		nthreadsnthrottle(2, Multithreading.NO_THROTTLE);
	}
	
//	@Test
//	public void test_2threads_2throttle() {
//		nthreadsnthrottle(2, 2);
//	}
//	
//	@Test
//	public void test_2threads_4throttle() {
//		nthreadsnthrottle(2, 4);
//	}
	
	@Test
	public void test_2threads_8throttle() {
		nthreadsnthrottle(2, 8);
	}
}
