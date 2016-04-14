package sarf.jingredients.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Concurrent {

	private ExecutorService executor;

	public Concurrent() {
		this(Integer.parseInt(System.getProperty("sarf.threads", "0")));		
	}
	

	public Concurrent(int thread) {
		if (thread <= 0) {
			thread = Runtime.getRuntime().availableProcessors();	
			if (thread > 4) {
				thread = thread - 2; // don't occupy all the processors
			}
		}
		executor = Executors.newFixedThreadPool(thread);
	}
	
	public void execute(Runnable task) {
		executor.execute(task);
	}
	
	public void waitComplete() {
		waitComplete(null);
	}
	
	public void waitComplete(Runnable onComplete) {
		try {
			executor.shutdown();
			executor.awaitTermination(1, TimeUnit.DAYS);
			if (onComplete != null) onComplete.run();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	
}
