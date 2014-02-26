package edu.cmu.ml.praprolog;

import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import edu.cmu.ml.praprolog.prove.LogicProgram;
import edu.cmu.ml.praprolog.prove.Prover;
import edu.cmu.ml.praprolog.prove.RawPosNegExample;
import edu.cmu.ml.praprolog.prove.RawPosNegExampleStreamer;

public class ModularMultiExampleCooker extends MultithreadedExampleCooker {
	private static final Logger log = Logger.getLogger(ModularMultiExampleCooker.class);
	private static final int THROTTLE=100;
	private static final int UNTHROTTLE=30;

	public ModularMultiExampleCooker(Prover p, LogicProgram program, int nt) {
		super(p, program, nt);
	}

	@Override
	public void cookExamples(String dataFile, Writer writer) throws IOException {
		ExecutorService cookingPool = Executors.newFixedThreadPool(this.nthreads);
		ExecutorService writingPool = Executors.newFixedThreadPool(1);
		int k=0;
		for(RawPosNegExample rawX : new RawPosNegExampleStreamer(dataFile).stream()) {
			
			int i = nextId(); k++;
			
			if (k-finished > THROTTLE) {
				int wait=100;
				while(k-finished > UNTHROTTLE) { 
					try {
						Thread.sleep(wait);
						wait *= 1.5;//2;
					} catch (InterruptedException e) {
						log.error("Interrupted?",e);
					} 
				}
			}
			
			writingPool.submit(
					new WriterThread(
							cookingPool.submit(new CookerThread(rawX,this,i,log)),
							writer,
							this,
							i));
			if (i % 100 == 0) {
				if (log.isDebugEnabled()) log.debug("Free Memory Created "+i+" "+Runtime.getRuntime().freeMemory()+" / "+Runtime.getRuntime().totalMemory()+" "+System.currentTimeMillis());
			}
		}
		cookingPool.shutdown();
		writingPool.shutdown();
		try {
			log.debug("Finishing writingPool...");
			writingPool.awaitTermination(7, TimeUnit.DAYS);
			log.debug("WritingPool finished.");
		} catch (InterruptedException e) {
			log.error("Interrupted?",e);
		}
		writer.close();
	}
	
	static int finished=0,empty=0;
	protected synchronized static void finish(boolean mt) {
		finished++;
		if (mt) empty++;
	}
	
	public class WriterThread implements Runnable {
		Future<ExampleCookingResult> future;
		Writer writer;
		ExampleCooker cooker;
		int id;
		public WriterThread(Future<ExampleCookingResult> f, Writer w, ExampleCooker c, int i) {
			this.future = f;
			this.writer = w;
			this.cooker = c;
			this.id = i;
		}

		@Override
		public void run() {
			ExampleCookingResult result;
			try {
				if (log.isDebugEnabled()) {
					log.debug("Asking "+id+":? "+System.currentTimeMillis()+" "+Thread.currentThread().getName());
					if (id % 10 == 0) log.debug("Free Memory Asking "+id+" "+Runtime.getRuntime().freeMemory()+" / "+Runtime.getRuntime().totalMemory()+" "+System.currentTimeMillis());
				}
				
				result = future.get(); // blocking call
				
				if (log.isDebugEnabled()) {
					if (id % 10 == 0) log.debug("Free Memory Got "+id+" "+Runtime.getRuntime().freeMemory()+" / "+Runtime.getRuntime().totalMemory()+" "+System.currentTimeMillis());
					log.debug("Got "+id+":"+result.id+" "+System.currentTimeMillis()+" "+Thread.currentThread().getName());
				}
				
				if (result.cookedExample.getGraph().getNumEdges() > 0) {
					writer.write(cooker.serializeCookedExample(result.rawX, result.cookedExample));
					if (log.isDebugEnabled())log.debug("Wrote "+id+":"+result.id+" "+System.currentTimeMillis()+" "+Thread.currentThread().getName());
					finish(false);
				} else {
					log.warn("Empty graph for example "+id); finish(true);
				}
			} catch (InterruptedException e) {
				log.error(id,e);
			} catch (ExecutionException e) {
				log.error(id,e);
			} catch (IOException e) {
				log.error(id,e);
			}
		}
		
	}
}
