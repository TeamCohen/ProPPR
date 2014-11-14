package edu.cmu.ml.proppr.util.multithreading;

import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;


public class WritingCleanup extends Cleanup<String> {
	protected Logger log;
	protected Writer writer;
	public WritingCleanup(Writer w, Logger l) { 
		this.writer = w;
		this.log = l;
	}
	@Override
	public Runnable cleanup(Future<String> in, int id) {
		return new RunWritingCleanup(in,id,writer);
	}
	public class RunWritingCleanup implements Runnable {
		protected Writer writer;
		protected int id;
		protected Future<String> input;
		public RunWritingCleanup(Future<String>in, int id, Writer w) {
			this.input = in;
			this.id = id;
			this.writer = w;
		}
		@Override
		public void run() {
			try {
				String s = this.input.get();
				if (s != null) this.writer.write(s);
				else log.warn("No output for #"+id);
			} catch (IOException e) {
				throw new IllegalStateException("IO trouble while writing: ",e);
			} catch (InterruptedException e) {
				throw new IllegalStateException("Interrupted while writing: ",e);
			} catch (ExecutionException e) {
				log.error("Execution trouble with #"+this.id,e);
			}
		}
	}
}
